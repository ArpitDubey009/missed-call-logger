package com.missedcalllogger.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.missedcalllogger.app.network.GoogleSheetHelper
import com.missedcalllogger.app.util.PendingCallsManager
import com.missedcalllogger.app.util.PrefsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CallReceiver — Detects phone state changes and identifies missed calls.
 * 
 * FLOW:
 * 1. Phone rings (RINGING) → Store the incoming number
 * 2. Phone stops ringing (IDLE) without being answered → MISSED CALL detected
 * 3. Extract caller name from contacts
 * 4. Send data to Google Sheets via Apps Script webhook
 * 5. If no internet → save to pending queue for auto-retry
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber: String? = null
        private var isIncoming = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> return
        }

        onCallStateChanged(context, state, number)
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        when {
            // ---- Incoming call starts ringing ----
            lastState == TelephonyManager.CALL_STATE_IDLE 
                && state == TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                incomingNumber = number
                Log.d(TAG, "📞 Incoming call from: $number")
            }

            // ---- Call was answered (went off-hook) ----
            state == TelephonyManager.CALL_STATE_OFFHOOK -> {
                // If it went from RINGING → OFFHOOK, the call was answered
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = true // was incoming, but answered
                }
            }

            // ---- Call ended (back to IDLE) ----
            state == TelephonyManager.CALL_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING && isIncoming) {
                    // RINGING → IDLE = MISSED CALL! 🎯
                    Log.d(TAG, "📵 MISSED CALL detected from: $incomingNumber")
                    
                    // Delay slightly to ensure call log is updated
                    val phoneNumber = incomingNumber ?: "Unknown"
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleMissedCall(context, phoneNumber)
                    }, 3000) // 3 second delay
                }

                // Reset state
                isIncoming = false
                incomingNumber = null
            }
        }

        lastState = state
    }

    /**
     * Handles a missed call: extracts info and sends to Google Sheets
     */
    private fun handleMissedCall(context: Context, phoneNumber: String) {
        val callerName = getContactName(context, phoneNumber) ?: "Unknown"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val now = Date()
        val date = dateFormat.format(now)
        val time = timeFormat.format(now)

        Log.d(TAG, "📝 Logging missed call: $phoneNumber ($callerName) at $date $time")

        // Update local stats
        val prefs = PrefsManager(context)
        prefs.incrementTotalLogged()
        prefs.setLastMissedCall("$callerName ($phoneNumber) — $time")

        // Send to Google Sheets
        val scriptUrl = prefs.getScriptUrl()
        if (scriptUrl.isNotEmpty()) {
            GoogleSheetHelper.sendMissedCall(
                context = context,
                scriptUrl = scriptUrl,
                phoneNumber = phoneNumber,
                callerName = callerName,
                date = date,
                time = time,
                onSuccess = {
                    Log.d(TAG, "✅ Successfully logged to Google Sheet")
                    sendNotification(context, phoneNumber, callerName, true)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to log: $error")
                    // Save to pending queue for auto-retry
                    PendingCallsManager.addPendingCall(context, phoneNumber, callerName, date, time)
                    prefs.incrementPendingSync()
                    sendNotification(context, phoneNumber, callerName, false)
                }
            )
        } else {
            Log.w(TAG, "⚠️ No Google Apps Script URL configured!")
            PendingCallsManager.addPendingCall(context, phoneNumber, callerName, date, time)
        }
    }

    /**
     * Looks up contact name from phone number
     */
    private fun getContactName(context: Context, phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact: ${e.message}")
        }
        return null
    }

    /**
     * Sends a notification about the logged missed call
     */
    private fun sendNotification(context: Context, phone: String, name: String, success: Boolean) {
        try {
            val notifHelper = com.missedcalllogger.app.util.NotificationHelper(context)
            if (success) {
                notifHelper.showCallLoggedNotification(phone, name)
            } else {
                notifHelper.showSyncPendingNotification(phone, name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Notification error: ${e.message}")
        }
    }
}
