package com.missedcalllogger.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.missedcalllogger.app.MissedCallLoggerApp
import com.missedcalllogger.app.MainActivity
import com.missedcalllogger.app.R
import com.missedcalllogger.app.receiver.CallReceiver

/**
 * MissedCallService — Foreground service that keeps call monitoring alive.
 * 
 * Android kills background services after a while. A foreground service with
 * a persistent notification ensures the app keeps monitoring calls reliably.
 */
class MissedCallService : Service() {

    companion object {
        private const val TAG = "MissedCallService"
        private const val NOTIFICATION_ID = 1001
    }

    private var callReceiver: CallReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 MissedCallService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "▶️ MissedCallService started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register the call receiver dynamically for extra reliability
        registerCallReceiver()
        
        // If the system kills the service, restart it
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "⏹ MissedCallService destroyed")
        unregisterCallReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Creates the persistent foreground notification
     */
    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MissedCallLoggerApp.CHANNEL_ID_SERVICE)
            .setContentTitle("📞 Missed Call Logger")
            .setContentText("Monitoring calls in background...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Registers the call receiver dynamically
     */
    private fun registerCallReceiver() {
        if (callReceiver == null) {
            callReceiver = CallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(callReceiver, filter)
            Log.d(TAG, "📡 CallReceiver registered dynamically")
        }
    }

    /**
     * Unregisters the call receiver
     */
    private fun unregisterCallReceiver() {
        callReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver: ${e.message}")
            }
            callReceiver = null
        }
    }
}
