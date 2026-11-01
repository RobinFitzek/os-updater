package com.oxygenupdater.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.oxygenupdater.extensions.get
import com.oxygenupdater.internal.settings.KeyUpdateCheckIntervalMinutes
import com.oxygenupdater.workers.UpdateCheckWorker
import com.oxygenupdater.workers.WorkUniqueUpdateCheck
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimum background update check interval: 1 minute.
 *
 * WorkManager's periodic work has a hard floor of 15 minutes, so intervals below
 * that are implemented as a self-rescheduling one-shot chain (the worker enqueues
 * the next run itself, see [UpdateCheckWorker]).
 */
const val MinUpdateCheckIntervalMinutes = 1L

/** Default background update check interval: 15 minutes */
const val DefaultUpdateCheckIntervalMinutes = 15L

/** WorkManager periodic-work floor, used when the interval is >= this */
private const val PeriodicFloorMinutes = 15L

/** Input data key telling the worker to reschedule itself */
const val WorkDataReschedule = "reschedule"

@Singleton
class UpdateCheckScheduler @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {

    fun intervalMinutes(): Long = sharedPreferences[KeyUpdateCheckIntervalMinutes, NotSetInterval]
        .let { if (it <= 0L) DefaultUpdateCheckIntervalMinutes else it }
        .coerceAtLeast(MinUpdateCheckIntervalMinutes)

    /**
     * (Re)schedules the background [UpdateCheckWorker] using the user-configured interval.
     * Intervals >= 15 min use a periodic request; below that, a self-rescheduling
     * one-shot chain. Existing work is replaced, so changing the interval in
     * settings takes effect immediately.
     */
    fun schedule(context: Context, intervalMinutes: Long = intervalMinutes()) {
        val minutes = intervalMinutes.coerceAtLeast(MinUpdateCheckIntervalMinutes)
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WorkUniqueUpdateCheck)

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        if (minutes >= PeriodicFloorMinutes) {
            wm.enqueueUniquePeriodicWork(
                WorkUniqueUpdateCheck,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                androidx.work.PeriodicWorkRequestBuilder<UpdateCheckWorker>(minutes, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MINUTES)
                    .build()
            )
        } else {
            // Self-rescheduling one-shot chain for sub-15-minute intervals
            wm.enqueueUniqueWork(
                WorkUniqueUpdateCheck,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                    .setConstraints(constraints)
                    .setInputData(Data.Builder().putBoolean(WorkDataReschedule, true).build())
                    .setInitialDelay(minutes, TimeUnit.MINUTES)
                    .build()
            )
        }
        logInfo(TAG, "Scheduled update check every $minutes min")
    }

    /** Enqueues a one-off immediate check, e.g. when a push notification arrives or the app starts */
    fun checkNow(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(
        WorkUniqueUpdateCheck + "_now",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
    )

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WorkUniqueUpdateCheck)

    companion object {
        private const val TAG = "UpdateCheckScheduler"

        /** Sentinel meaning "user never picked an interval" (legacy installs) */
        private const val NotSetInterval = -1L
    }
}
