package com.oxygenupdater.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oxygenupdater.utils.UpdateCheckScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * OnePlus devices have very aggressive battery restrictions. By default, apps don't receive
 * [Intent.ACTION_BOOT_COMPLETED] even if [android.Manifest.permission.RECEIVE_BOOT_COMPLETED]
 * is specified. OOS12+ has a per-app toggle for "Allow auto-launch", which must also be enabled
 * for this receiver to work.
 */
@AndroidEntryPoint
class DeviceBootOrAppUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateCheckScheduler: UpdateCheckScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // Ensure the periodic update check survives reboots & app updates,
        // then run an immediate check
        updateCheckScheduler.schedule(context)
        updateCheckScheduler.checkNow(context)
    }
}
