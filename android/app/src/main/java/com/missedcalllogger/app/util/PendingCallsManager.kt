package com.missedcalllogger.app.util

import android.content.Context
import android.util.Log
import com.missedcalllogger.app.network.GoogleSheetHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * PendingCallsManager — Manages a queue of missed calls that couldn't be uploaded.
 * 
 * AUTO-SCALING FEATURE:
 * When there's no internet, calls are saved locally in a JSON queue.
 * When internet is restored (via NetworkChangeReceiver), all pending calls
 * are automatically synced to Google Sheets.
 */
object PendingCallsManager {

    private const val TAG = "PendingCallsManager"
    private const val PREFS_NAME = "pending_calls_prefs"
    private const val KEY_PENDING_QUEUE = "pending_queue"

    /**
     * Adds a missed call to the pending upload queue
     */
    fun addPendingCall(
        context: Context,
        phoneNumber: String,
        callerName: String,
        date: String,
        time: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val queueStr = prefs.getString(KEY_PENDING_QUEUE, "[]") ?: "[]"
        val queue = JSONArray(queueStr)

        val callEntry = JSONObject().apply {
            put("phoneNumber", phoneNumber)
            put("callerName", callerName)
            put("date", date)
            put("time", time)
            put("timestamp", System.currentTimeMillis())
        }

        queue.put(callEntry)
        prefs.edit().putString(KEY_PENDING_QUEUE, queue.toString()).apply()

        Log.d(TAG, "📥 Added to pending queue. Queue size: ${queue.length()}")
    }

    /**
     * Gets the number of pending calls
     */
    fun getPendingCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val queueStr = prefs.getString(KEY_PENDING_QUEUE, "[]") ?: "[]"
        return JSONArray(queueStr).length()
    }

    /**
     * Syncs all pending calls to Google Sheets
     * Called automatically when internet is restored
     */
    fun syncPendingCalls(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appPrefs = PrefsManager(context)
        val scriptUrl = appPrefs.getScriptUrl()

        if (scriptUrl.isEmpty()) {
            Log.w(TAG, "⚠️ No script URL configured, skipping sync")
            return
        }

        val queueStr = prefs.getString(KEY_PENDING_QUEUE, "[]") ?: "[]"
        val queue = JSONArray(queueStr)

        if (queue.length() == 0) {
            Log.d(TAG, "✅ No pending calls to sync")
            return
        }

        Log.d(TAG, "🔄 Syncing ${queue.length()} pending calls...")

        val remainingQueue = JSONArray()

        for (i in 0 until queue.length()) {
            val call = queue.getJSONObject(i)

            GoogleSheetHelper.sendMissedCall(
                context = context,
                scriptUrl = scriptUrl,
                phoneNumber = call.getString("phoneNumber"),
                callerName = call.getString("callerName"),
                date = call.getString("date"),
                time = call.getString("time"),
                onSuccess = {
                    Log.d(TAG, "✅ Synced pending call: ${call.getString("phoneNumber")}")
                    appPrefs.decrementPendingSync()
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to sync: $error")
                    // Add back to remaining queue
                    synchronized(remainingQueue) {
                        remainingQueue.put(call)
                    }
                }
            )
        }

        // Update the queue with only the failed ones (after a delay for async completion)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            prefs.edit().putString(KEY_PENDING_QUEUE, remainingQueue.toString()).apply()
            Log.d(TAG, "📝 Updated pending queue. Remaining: ${remainingQueue.length()}")
        }, 10000) // Wait 10 seconds for all async requests to complete
    }

    /**
     * Clears all pending calls (use carefully)
     */
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_QUEUE, "[]").apply()
        PrefsManager(context).resetPendingSync()
        Log.d(TAG, "🗑️ Cleared all pending calls")
    }
}
