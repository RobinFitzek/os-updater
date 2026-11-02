package com.oxygenupdater.viewmodels

import android.util.Log
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExperimentalWorkRequestBuilderApi
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.remove
import com.oxygenupdater.extensions.set
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyDownloadBytesDone
import com.oxygenupdater.internal.settings.KeyIgnoreIncorrectDeviceWarnings
import com.oxygenupdater.internal.settings.KeyIgnoreNotificationPermissionSheet
import com.oxygenupdater.internal.settings.KeyIgnoreUnsupportedDeviceWarnings
import com.oxygenupdater.internal.settings.KeyMethodRolloutSnapshots
import com.oxygenupdater.internal.settings.KeySetupDone
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.device.DefaultDeviceName
import com.oxygenupdater.ui.update.DownloadStatus
import com.oxygenupdater.ui.update.WorkInfoWithStatus
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.workers.DirectoryRoot
import com.oxygenupdater.workers.DownloadWorker
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraFilename
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraHttpCode
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraHttpMessage
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraOtaVersion
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraUrl
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraVersion
import com.oxygenupdater.workers.WorkDataDownloadFailureType
import com.oxygenupdater.workers.WorkDataDownloadProgress
import com.oxygenupdater.workers.WorkDataDownloadSuccessType
import com.oxygenupdater.workers.WorkDataMd5VerificationFailureType
import com.oxygenupdater.workers.WorkUniqueDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverRepository: ServerRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val allDevicesFlow = MutableStateFlow<List<Device>?>(null)
    val allDevicesState = allDevicesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = allDevicesFlow.value
    )

    private val serverMessagesFlow = MutableStateFlow<List<ServerMessage>>(listOf())
    val serverMessages = serverMessagesFlow.asStateFlow()

    private val serverStatusFlow = MutableStateFlow(ServerStatus(ServerStatus.Status.NORMAL, BuildConfig.VERSION_NAME))
    val serverStatus = serverStatusFlow.asStateFlow()

    var deviceOsSpec: DeviceOsSpec? = null
    var deviceMismatch by mutableStateOf<Triple<Boolean, String, String>?>(null)
        private set

    var shouldShowOnboarding by mutableStateOf(!sharedPreferences[KeySetupDone, false])

    val canShowNotifPermissionSheet = !sharedPreferences[KeyIgnoreNotificationPermissionSheet, false]

    val shouldShowUnsupportedDeviceDialog
        get() = !sharedPreferences[KeyIgnoreUnsupportedDeviceWarnings, false]

    val shouldShowIncorrectDeviceDialog
        get() = !sharedPreferences[KeyIgnoreIncorrectDeviceWarnings, false]

    val deviceId
        get() = sharedPreferences[KeyDeviceId, NotSetL]

    val updateMethodId
        get() = sharedPreferences[KeyUpdateMethodId, NotSetL]

    /** Checks if [deviceId] & [updateMethodId] are set (i.e. not [NotSetL]) */
    val isDeviceAndMethodSet
        get() = deviceId != NotSetL && updateMethodId != NotSetL

    /**
     * Checks if user has completed the onboarding process, meaning they've
     * selected a device & method, and also pressed the "Start app" button.
     */
    val isOnboardingComplete
        get() = sharedPreferences[KeySetupDone, false] && isDeviceAndMethodSet

    /** Ensure `init` is placed after all declarations that may be used in contained functions */
    init {
        fetchAllDevices()
        fetchServerStatus()
    }

    private var lastDownloadStatus: DownloadStatus? = null
    private val cancelShouldPauseDownload = AtomicBoolean(false)
    val workInfoWithStatus = workManager.getWorkInfosForUniqueWorkFlow(WorkUniqueDownload).map { info ->
        val infoAndStatus = info.firstOrNull()?.let {
            val status = when (it.state) {
                State.ENQUEUED -> if (it.stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP) {
                    /**
                     * Required to workaround https://issuetracker.google.com/issues/552283281: a re-cancel is required
                     * because WorkManager currently erroneously re-enqueues a user/app-cancelled job if
                     * [androidx.work.WorkRequest.Builder.setBackoffForSystemInterruptions] is used.
                     */
                    lastDownloadStatus?.let { last ->
                        when (last) {
                            /**
                             * Note: sometimes WorkManager transitions directly to [DownloadStatus.Downloading]
                             * after pausing/cancelling, so we check for that as well here. We already guard this block
                             * with [WorkInfo.stopReason] above so it should be fine.
                             */
                            DownloadStatus.Downloading -> {
                                workManager.cancelUniqueWork(WorkUniqueDownload)
                                val pause = cancelShouldPauseDownload.get()
                                if (pause) DownloadStatus.DownloadPaused else DownloadStatus.NotDownloading
                            }

                            /** These functions internally update [cancelShouldPauseDownload] */
                            DownloadStatus.DownloadPaused -> last.also { pauseDownloadWork() }
                            DownloadStatus.NotDownloading -> last.also { cancelDownloadWork() }

                            else -> DownloadStatus.DownloadQueued
                        }
                    } ?: DownloadStatus.DownloadQueued
                } else DownloadStatus.DownloadQueued

                State.BLOCKED -> DownloadStatus.DownloadQueued

                State.RUNNING -> when (it.progress.getInt(WorkDataDownloadProgress, 0)) {
                    NotSet -> DownloadStatus.Verifying
                    else -> DownloadStatus.Downloading
                }

                State.SUCCEEDED -> when (it.outputData.getInt(WorkDataDownloadSuccessType, NotSet)) {
                    // We don't set any output data until the verification process has also completed
                    NotSet -> DownloadStatus.DownloadCompleted
                    else -> DownloadStatus.VerificationCompleted
                }

                State.FAILED -> {
                    val outputData = it.outputData
                    when (outputData.getInt(WorkDataMd5VerificationFailureType, NotSet)) {
                        NotSet -> when (outputData.getInt(WorkDataDownloadFailureType, NotSet)) {
                            NotSet -> DownloadStatus.DownloadFailed
                            else -> DownloadStatus.NotDownloading
                        }

                        else -> DownloadStatus.VerificationFailed
                    }
                }

                // Downloads are paused by cancelling work (we're tracking `bytesDone` and sending a `Range` header for
                // resume operations). The only place from where the user can cancel the download is the action button,
                // and we use the [cancelShouldPauseDownload] flag to temporarily force pause/delete. Status is also set
                // in UpdateInformationFragment's downloadAction callback right after pausing/cancelling work.
                State.CANCELLED -> {
                    val pause = cancelShouldPauseDownload.getAndSet(false) // reset to initial value
                    if (pause) DownloadStatus.DownloadPaused else DownloadStatus.NotDownloading
                }
            }
            WorkInfoWithStatus(it, status)
        } ?: WorkInfoWithStatus(null, lastDownloadStatus ?: DownloadStatus.NotDownloading)

        infoAndStatus.also { lastDownloadStatus = it.downloadStatus }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkInfoWithStatus(null, DownloadStatus.NotDownloading)
    )

    fun updateDeviceMismatch(
        allDevices: List<Device>? = allDevicesFlow.value,
    ) = Utils.checkDeviceMismatch(
        devices = allDevices,
        savedDeviceId = sharedPreferences[KeyDeviceId, NotSetL],
    ).let {
        val savedDeviceName = sharedPreferences[KeyDevice, DefaultDeviceName]
        deviceMismatch = Triple(it.first, savedDeviceName, it.second ?: DefaultDeviceName)
    }

    private var deviceFetchFailed = MutableStateFlow(false)

    /** `true` when the last device-list fetch failed (server unreachable) */
    val deviceFetchFailedState = deviceFetchFailed.asStateFlow()

    fun fetchAllDevices() = viewModelScope.launch(Dispatchers.IO) {
        val response = serverRepository.fetchDevices(DeviceRequestFilter.All)
        if (response == null) {
            // fetch failed: keep previous list but flag the error so the UI can
            // show an error + retry instead of "please wait" forever
            deviceFetchFailed.value = true
            if (allDevicesFlow.value.isNullOrEmpty()) allDevicesFlow.emit(listOf())
            return@launch
        }
        deviceFetchFailed.value = false
        deviceOsSpec = Utils.checkDeviceOsSpec(response)
        updateDeviceMismatch(response)
        allDevicesFlow.emit(response)
    }

    fun fetchServerMessages() = viewModelScope.launch(Dispatchers.IO) {
        serverMessagesFlow.emit(serverRepository.fetchServerMessages()?.filterNot {
            it.text.isNullOrBlank() // filter out empty text just in case
        } ?: return@launch)
    }

    fun fetchServerStatus() = viewModelScope.launch(Dispatchers.IO) {
        serverStatusFlow.emit(serverRepository.fetchServerStatus())
    }

    /**
     * @return true if the downloaded ZIP was successfully deleted
     */
    fun deleteDownloadedFile(context: Context, filename: String?): Boolean {
        if (filename == null) {
            Log.w(TAG, "Can't delete downloaded file: filename null")
            return false
        }

        logDebug(TAG, "Deleting any associated tracker preferences for downloaded file")
        sharedPreferences.remove(KeyDownloadBytesDone)
        logDebug(TAG, "Deleting downloaded update file $filename")

        File(context.getExternalFilesDir(null), filename).run {
            if (exists() && !delete()) Log.w(TAG, "Can't delete temporary file $filename")
        }

        return File(Environment.getExternalStoragePublicDirectory(DirectoryRoot).absolutePath, filename).run {
            if (exists() && !delete()) {
                Log.w(TAG, "Can't delete downloaded file $filename")
                false
            } else true
        }
    }

    @OptIn(ExperimentalWorkRequestBuilderApi::class)
    fun enqueueDownloadWork() = workManager.enqueueUniqueWork(
        WorkUniqueDownload,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .setBackoffForSystemInterruptions()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
    )

    fun cancelDownloadWork(context: Context, filename: String?) {
        cancelDownloadWork()
        deleteDownloadedFile(context, filename)
    }

    private fun cancelDownloadWork() {
        cancelShouldPauseDownload.set(false) // make work's CANCELLED state map to DownloadStatus.NOT_DOWNLOADING
        workManager.cancelUniqueWork(WorkUniqueDownload)
    }

    fun pauseDownloadWork() {
        cancelShouldPauseDownload.set(true) // make work's CANCELLED state map to DownloadStatus.DOWNLOAD_PAUSED
        workManager.cancelUniqueWork(WorkUniqueDownload)
    }

    /**
     * This method is called in [MainActivity.onDestroy], to prune finished work.
     */
    fun maybePruneWork() {
        if (workInfoWithStatus.value.downloadStatus.run { successful || failed }) {
            workManager.pruneWork()
        }
    }

    fun logDownloadError(data: Data) = viewModelScope.launch(Dispatchers.IO) {
        maybePruneWork() // prune to avoid re-sending the same error on recompose
        serverRepository.logDownloadError(
            data.getString(WorkDataDownloadFailureExtraUrl),
            data.getString(WorkDataDownloadFailureExtraFilename),
            data.getString(WorkDataDownloadFailureExtraVersion),
            data.getString(WorkDataDownloadFailureExtraOtaVersion),
            data.getInt(WorkDataDownloadFailureExtraHttpCode, NotSet),
            data.getString(WorkDataDownloadFailureExtraHttpMessage),
        )
    }.let {}

    private val rolloutSnapshotsJsonFlow = MutableStateFlow(sharedPreferences[KeyMethodRolloutSnapshots, ""])

    /** JSON of methodId -> [com.oxygenupdater.models.MethodRolloutSnapshot]; re-emitted when a background check writes it */
    val rolloutSnapshotsJson = rolloutSnapshotsJsonFlow.asStateFlow()

    /** Call after a background update check may have written new snapshots */
    fun refreshRolloutSnapshots() {
        rolloutSnapshotsJsonFlow.value = sharedPreferences[KeyMethodRolloutSnapshots, ""]
    }

    fun getPref(key: String, default: String) = sharedPreferences[key, default]
    fun getPref(key: String, default: Boolean) = sharedPreferences[key, default]
    fun getPref(key: String, default: Long) = sharedPreferences[key, default]
    fun persist(key: String, value: Boolean) = sharedPreferences.set(key, value)
    fun persist(key: String, value: Long) = sharedPreferences.set(key, value)

    fun openEmail(context: Context) {
        val chosenDevice = sharedPreferences[KeyDevice, "<UNKNOWN>"]
        val chosenMethod = sharedPreferences[KeyUpdateMethod, "<UNKNOWN>"]
        val advancedMode = sharedPreferences[KeyAdvancedMode, false]
        val osVersionWithType = SystemVersionProperties.osVersion + SystemVersionProperties.osType.let {
            if (it.isNotEmpty()) " ($it)" else ""
        }

        // Don't localize any part of this, it'll be an annoyance for us while reading emails
        val emailBody = """
--------------------
• Device: $chosenDevice (${SystemVersionProperties.deviceProductName}/${Build.DEVICE})
• Method: $chosenMethod
• OS version: $osVersionWithType
• OTA version: ${SystemVersionProperties.otaVersion}
• Advanced mode: $advancedMode
• App version: ${BuildConfig.VERSION_NAME}
--------------------

<write your query here>"""

        try {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("support@oxygenupdater.com"))
                    .putExtra(Intent.EXTRA_TEXT, emailBody)
            )
        } catch (_: ActivityNotFoundException) {
            // TODO(translate)
            context.showToast("You don't appear to have an email client installed on your phone")
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
