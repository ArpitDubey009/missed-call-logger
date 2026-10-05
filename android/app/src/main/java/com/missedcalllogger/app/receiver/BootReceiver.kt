package com.missedcalllogger.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.missedcalllogger.app.service.MissedCallService
import com.missedcalllogger.app.util.PrefsManager

/**
 * BootReceiver — Auto-starts the monitoring service when the device boots up.
 * This ensures missed calls are tracked even after a phone restart.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val prefs = PrefsManager(context)
            
            // Only auto-start if the service was running before reboot
            if (prefs.isServiceRunning()) {
                Log.d(TAG, "🔄 Device booted — restarting MissedCallService")
                
                val serviceIntent = Intent(context, MissedCallService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
