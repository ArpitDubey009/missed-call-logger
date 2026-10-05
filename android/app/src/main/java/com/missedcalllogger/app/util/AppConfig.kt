package com.missedcalllogger.app.util

/**
 * AppConfig — Centralized configuration for the app.
 * Your Google Apps Script Web App URL is pre-configured here.
 */
object AppConfig {

    /**
     * Google Apps Script Web App URL
     * Deployed by: vd391999@gmail.com
     * Auto-logs missed calls to Google Sheets
     */
    const val SCRIPT_URL = "https://script.google.com/macros/s/" +
            "AKfycbw9aGwXfNknILog47LlPeoHlvJ_KjxAPBDqxzlTBuc6cy3DWt8JHQlo9QLLafxx92Y/exec"

    /** App version for tracking */
    const val APP_VERSION = "1.0.0"

    /** Days to keep in pending offline queue before giving up */
    const val PENDING_RETRY_DAYS = 7
}
