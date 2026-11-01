package com.oxygenupdater.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.WorkManager
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
import com.oxygenupdater.utils.UpdateCheckScheduler
import com.oxygenupdater.utils.WorkDataReschedule
import android.util.Log
import com.oxygenupdater.utils.logDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
    private val updateCheckScheduler: UpdateCheckScheduler,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        logDebug(TAG, "doWork")

        val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
        if (deviceId == NotSetL) return@withContext Result.success().also {
            logDebug(TAG, "No device selected yet; skipping update check")
        }

        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId)
        if (methods.isNullOrEmpty()) return@withContext Result.retry().also {
            Log.w(TAG, "Couldn't fetch update methods for device $deviceId")
        }

        var newest: UpdateData? = null
        val snapshots = HashMap<String, MethodRolloutSnapshot>(methods.size * 6)
        val checkedAt = System.currentTimeMillis()
        for (method in methods) {
            val methodId = method.id ?: continue
            // Probe every region (plus the device's own) so the rollout table shows
            // which variants already have a newer build
            val regions = MethodRolloutSnapshot.RolloutRegions + ""
            for (region in regions) {
                val updateData = serverRepository.fetchUpdateData(deviceId, methodId, persist = false, regionOverride = region)
                if (updateData == null) continue

                val ota = updateData.otaVersionNumber
                snapshots["$methodId:$region"] = MethodRolloutSnapshot(
                    methodId = methodId,
                    methodName = method.name,
                    region = region,
                    otaVersionNumber = ota,
                    buildDate = UpdateData.getBuildDate(ota),
                    checkedAtEpochMs = checkedAt,
                )

                // Only the device's own region decides the "new update" notification
                if (region.isNotEmpty()) continue
                if (updateData.systemIsUpToDate || !updateData.isUpdateInformationAvailable) continue

                val candidate = newest
                if (candidate == null || UpdateData.getBuildDate(updateData.otaVersionNumber) >
                    UpdateData.getBuildDate(candidate.otaVersionNumber)
                ) newest = updateData
            }
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

        rescheduleIfNeeded()
        Result.success()
    }

    /**
     * For sub-15-minute intervals (WorkManager's periodic floor), re-enqueue the
     * next one-shot run ourselves. Periodic work never carries the flag, so this
     * is a no-op there.
     */
    private fun rescheduleIfNeeded() {
        if (!inputData.getBoolean(WorkDataReschedule, false)) return
        val context = applicationContext
        val minutes = updateCheckScheduler.intervalMinutes()
        if (minutes >= 15L) return // periodic work handles these
        WorkManager.getInstance(context).enqueueUniqueWork(
            WorkUniqueUpdateCheck,
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(Data.Builder().putBoolean(WorkDataReschedule, true).build())
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .build()
        )
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
    }
}
