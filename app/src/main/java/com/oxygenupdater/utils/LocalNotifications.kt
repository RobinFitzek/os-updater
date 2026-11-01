package com.oxygenupdater.utils

import android.app.Notification.CATEGORY_ERROR
import android.app.Notification.CATEGORY_PROGRESS
import android.app.Notification.CATEGORY_STATUS
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.extensions.setBigTextStyle
import com.oxygenupdater.extensions.tryNotify
import com.oxygenupdater.ui.main.DeepLinkGuide
import com.oxygenupdater.ui.main.DeepLinkUpdate
import com.oxygenupdater.ui.main.DownloadedArg
import com.oxygenupdater.ui.main.OuSchemeSuffixed
import com.oxygenupdater.ui.update.KeyDownloadErrorMessage
import com.oxygenupdater.ui.update.KeyDownloadErrorResumable
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DownloadStatusNotifChannelId
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.VerificationStatusNotifChannelId
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.UpdateNotifChannelId

object LocalNotifications {

    /**
     * Shows a high-priority notification that a new system update is available,
     * posted by the background [com.oxygenupdater.workers.UpdateCheckWorker].
     * Tapping it opens the app's update screen.
     */
    fun showUpdateAvailableNotification(
        context: Context,
        versionNumber: String,
        otaVersionNumber: String,
    ) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            // Since MainActivity's `launchMode` is `singleTask`, we don't
            // need to add any flags to avoid creating multiple instances
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "$OuSchemeSuffixed$DeepLinkUpdate".toUri()
            },
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.update_notification_channel_name)
        val text = context.getString(R.string.notification_version, versionNumber, otaVersionNumber)

        val notification = NotificationCompat.Builder(context, UpdateNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setSubText(title)
            .setContentTitle(title)
            .setBigTextStyle(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(context).tryNotify(NotificationIds.LocalUpdateAvailable, notification)
    }

    fun showDownloadCompleteNotification(context: Context) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW,
                "$OuSchemeSuffixed$DeepLinkGuide?$DownloadedArg=true".toUri(),
                context,
                MainActivity::class.java
            ),
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.download_complete)
        val text = context.getString(R.string.download_complete_notification)

        val notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
            .setSmallIcon(R.drawable.download)
            .setSubText(context.getString(R.string.download_status_notification_channel_name))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalMd5Verification)
            tryNotify(NotificationIds.LocalDownload, notification)
        }
    }

    fun showDownloadFailedNotification(
        context: Context,
        resumable: Boolean,
        @StringRes message: Int,
        @StringRes notificationMessage: Int,
    ) {
        // Since MainActivity's `launchMode` is `singleTask`, we don't
        // need to add any flags to avoid creating multiple instances
        val intent = Intent(context, MainActivity::class.java)
            // Show a dialog detailing the download failure
            .putExtra(KeyDownloadErrorMessage, context.getString(message))
            .putExtra(KeyDownloadErrorResumable, resumable)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
            .setSmallIcon(R.drawable.download)
            .setSubText(context.getString(R.string.download_status_notification_channel_name))
            .setContentTitle(context.getString(R.string.download_failed))
            .setBigTextStyle(context.getString(notificationMessage))
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setCategory(CATEGORY_ERROR)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalMd5Verification)
            tryNotify(NotificationIds.LocalDownload, notification)
        }
    }

    fun showVerificationFailedNotification(context: Context) {
        val title = context.getString(R.string.download_verifying_error)
        val text = context.getString(R.string.download_notification_error_corrupt)

        val notification = NotificationCompat.Builder(context, VerificationStatusNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setSubText(context.getString(R.string.verification_status_notification_channel_name))
            .setContentTitle(title)
            .setBigTextStyle(text)
            .setOngoing(false)
            .setCategory(CATEGORY_ERROR)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalDownload)
            tryNotify(NotificationIds.LocalMd5Verification, notification)
        }
    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     */
    fun showVerifyingNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, VerificationStatusNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setSubText(context.getString(R.string.verification_status_notification_channel_name))
            .setContentTitle(context.getString(R.string.download_verifying))
            .setProgress(100, 50, true)
            .setOngoing(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalDownload)
            tryNotify(NotificationIds.LocalMd5Verification, notification)
        }
    }

    fun hideDownloadCompleteNotification(context: Context) = with(NotificationManagerCompat.from(context)) {
        cancel(NotificationIds.LocalDownload)
        cancel(NotificationIds.LocalMd5Verification)
    }
}
