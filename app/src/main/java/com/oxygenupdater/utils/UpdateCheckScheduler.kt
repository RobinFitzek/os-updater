package com.oxygenupdater.utils

import androidx.compose.runtime.getValue
import android.content.Context
import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.oxygenupdater.internal.settings.KeyUpdateCheckIntervalMinutes
import com.oxygenupdater.extensions.get
import com.oxygenupdater.workers.UpdateCheckWorker
import com.oxygenupdater.workers.WorkUniqueUpdateCheck
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimum periodic interval supported by WorkManager: 15 minutes.
 *
 * @see androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
 */
const val MinUpdateCheckIntervalMinutes = 15L

/** Default background update check interval: 15 minutes */
const val DefaultUpdateCheckIntervalMinutes = 15L

@Singleton
class UpdateCheckScheduler @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {

    fun intervalMinutes(context: Context): Long = sharedPreferences[KeyUpdateCheckIntervalMinutes, NotSetInterval]
        .let { if (it <= 0L) DefaultUpdateCheckIntervalMinutes else it }
        .coerceAtLeast(MinUpdateCheckIntervalMinutes)

    /**
     * (Re)schedules the periodic [UpdateCheckWorker] using the user-configured interval.
     * `UPDATE` replaces any existing schedule, so changing the interval in settings
     * takes effect immediately.
     */
    fun schedule(context: Context, intervalMinutes: Long = intervalMinutes(context)) {
        val minutes = intervalMinutes.coerceAtLeast(MinUpdateCheckIntervalMinutes)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkUniqueUpdateCheck,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(minutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MINUTES)
                .build()
        )
        logInfo(TAG, "Scheduled update check every $minutes min")
    }

    /** Enqueues a one-off immediate check, e.g. when a push notification arrives or the app starts */
    fun checkNow(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(
        WorkUniqueUpdateCheck + "_now",
        androidx.work.ExistingWorkPolicy.KEEP,
        androidx.work.OneTimeWorkRequestBuilder<UpdateCheckWorker>()
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
