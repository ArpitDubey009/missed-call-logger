package com.missedcalllogger.app.util

import android.content.Context
import android.content.SharedPreferences
import com.missedcalllogger.app.util.AppConfig

/**
 * PrefsManager — Manages all SharedPreferences for the app.
 * 
 * Stores:
 * - Google Apps Script URL
 * - Service running state
 * - Call statistics (total logged, pending sync, last missed call)
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "missed_call_logger_prefs"
        private const val KEY_SCRIPT_URL = "script_url"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_TOTAL_LOGGED = "total_logged"
        private const val KEY_PENDING_SYNC = "pending_sync"
        private const val KEY_LAST_MISSED_CALL = "last_missed_call"
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ====== Script URL ======

    fun getScriptUrl(): String = prefs.getString(KEY_SCRIPT_URL, AppConfig.SCRIPT_URL) ?: AppConfig.SCRIPT_URL

    fun setScriptUrl(url: String) {
        prefs.edit().putString(KEY_SCRIPT_URL, url).apply()
    }

    // ====== Service State ======

    fun isServiceRunning(): Boolean = prefs.getBoolean(KEY_SERVICE_RUNNING, false)

    fun setServiceRunning(running: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply()
    }

    // ====== Statistics ======

    fun getTotalLogged(): Int = prefs.getInt(KEY_TOTAL_LOGGED, 0)

    fun incrementTotalLogged() {
        val current = getTotalLogged()
        prefs.edit().putInt(KEY_TOTAL_LOGGED, current + 1).apply()
    }

    fun getPendingSyncCount(): Int = prefs.getInt(KEY_PENDING_SYNC, 0)

    fun incrementPendingSync() {
        val current = getPendingSyncCount()
        prefs.edit().putInt(KEY_PENDING_SYNC, current + 1).apply()
    }

    fun decrementPendingSync() {
        val current = getPendingSyncCount()
        if (current > 0) {
            prefs.edit().putInt(KEY_PENDING_SYNC, current - 1).apply()
        }
    }

    fun resetPendingSync() {
        prefs.edit().putInt(KEY_PENDING_SYNC, 0).apply()
    }

    fun getLastMissedCall(): String = prefs.getString(KEY_LAST_MISSED_CALL, "None yet") ?: "None yet"

    fun setLastMissedCall(info: String) {
        prefs.edit().putString(KEY_LAST_MISSED_CALL, info).apply()
    }
}
