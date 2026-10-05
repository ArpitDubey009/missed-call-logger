package com.missedcalllogger.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.missedcalllogger.app.MissedCallLoggerApp
import com.missedcalllogger.app.MainActivity
import com.missedcalllogger.app.R

/**
 * NotificationHelper — Displays notifications for missed call events.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private var notificationId = 2000
    }

    /**
     * Shows notification when a missed call is successfully logged
     */
    fun showCallLoggedNotification(phoneNumber: String, callerName: String) {
        val displayName = if (callerName != "Unknown") callerName else phoneNumber

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MissedCallLoggerApp.CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📵 Missed Call Logged")
            .setContentText("$displayName — saved to Google Sheet ✅")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId++, notification)
    }

    /**
     * Shows notification when a call couldn't be synced (offline)
     */
    fun showSyncPendingNotification(phoneNumber: String, callerName: String) {
        val displayName = if (callerName != "Unknown") callerName else phoneNumber

        val notification = NotificationCompat.Builder(context, MissedCallLoggerApp.CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📵 Missed Call — Sync Pending")
            .setContentText("$displayName — will sync when online ⏳")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId++, notification)
    }
}
