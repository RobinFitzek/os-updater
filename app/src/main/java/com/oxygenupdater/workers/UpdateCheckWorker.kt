package com.oxygenupdater.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyLastNotifiedOtaVersion
import com.oxygenupdater.internal.settings.KeyMethodRolloutSnapshots
import com.oxygenupdater.models.MethodRolloutSnapshot
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.json
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.LocalNotifications
import com.google.firebase.crashlytics.FirebaseCrashlytics.logWarning
import com.oxygenupdater.utils.logDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodically checks the server for a newer update, for **every** update method
 * available for the selected device, so that whichever method (e.g. full vs
 * incremental) receives the rollout first is detected.
 *
 * When a method reports an update newer than the last notified one, a local
 * notification is posted. The local update-data row is left untouched, so opening
 * the app still shows fresh data only after a real (no-cache) fetch.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val sharedPreferences: SharedPreferences,
    private val serverRepository: ServerRepository,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        logDebug(TAG, "doWork")

        val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
        if (deviceId == NotSetL) return@withContext Result.success().also {
            logDebug(TAG, "No device selected yet; skipping update check")
        }

        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId)
        if (methods.isNullOrEmpty()) return@withContext Result.retry().also {
            logWarning(TAG, "Couldn't fetch update methods for device $deviceId")
        }

        var newest: UpdateData? = null
        val snapshots = HashMap<Long, MethodRolloutSnapshot>(methods.size)
        val checkedAt = System.currentTimeMillis()
        for (method in methods) {
            val methodId = method.id ?: continue
            val updateData = serverRepository.fetchUpdateData(deviceId, methodId, persist = false)
            if (updateData == null) continue

            // Record what this method currently reports, even if up-to-date, so the
            // rollout table can show the latest known build per method
            val ota = updateData.otaVersionNumber
            snapshots[methodId] = MethodRolloutSnapshot(
                methodId = methodId,
                methodName = method.name,
                otaVersionNumber = ota,
                buildDate = UpdateData.getBuildDate(ota),
                checkedAtEpochMs = checkedAt,
            )

            if (updateData.systemIsUpToDate || !updateData.isUpdateInformationAvailable) continue

            val candidate = newest
            if (candidate == null || UpdateData.getBuildDate(updateData.otaVersionNumber) >
                UpdateData.getBuildDate(candidate.otaVersionNumber)
            ) newest = updateData
        }

        // Persist snapshots (client-side only; no backend endpoint needed)
        if (snapshots.isNotEmpty()) {
            sharedPreferences[KeyMethodRolloutSnapshots] = json.encodeToString(snapshots)
        }

        val latest = newest
        if (latest == null) return@withContext Result.success().also {
            logDebug(TAG, "No new update available for any method of device $deviceId")
        }

        val otaVersion = latest.otaVersionNumber
        if (otaVersion.isNullOrBlank()) return@withContext Result.success()

        val lastNotified = sharedPreferences[KeyLastNotifiedOtaVersion, ""]
        val isNewer = lastNotified.isEmpty() ||
                UpdateData.getBuildDate(otaVersion) > UpdateData.getBuildDate(lastNotified)

        if (isNewer) {
            sharedPreferences[KeyLastNotifiedOtaVersion] = otaVersion
            LocalNotifications.showUpdateAvailableNotification(
                context = applicationContext,
                versionNumber = latest.versionNumber ?: otaVersion,
                otaVersionNumber = otaVersion,
            )
            logDebug(TAG, "New update detected: $otaVersion (notified)")
        } else {
            logDebug(TAG, "Update $otaVersion was already notified; skipping notification")
        }

        Result.success()
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
    }
}
