package com.missedcalllogger.app.network

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * GoogleSheetHelper — Handles HTTP communication with Google Apps Script.
 * 
 * Sends missed call data as JSON POST to the Apps Script web app URL.
 * The Apps Script then writes the data to the Google Sheet.
 */
object GoogleSheetHelper {

    private const val TAG = "GoogleSheetHelper"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)         // Apps Script redirects after deploy
        .followSslRedirects(true)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Sends a missed call entry to Google Sheets via Apps Script
     * 
     * @param context       Android context
     * @param scriptUrl     Google Apps Script deployed URL
     * @param phoneNumber   Caller's phone number
     * @param callerName    Caller's name (from contacts, or "Unknown")
     * @param date          Date string (yyyy-MM-dd)
     * @param time          Time string (hh:mm:ss a)
     * @param onSuccess     Callback on successful upload
     * @param onFailure     Callback on failure with error message
     */
    fun sendMissedCall(
        context: Context,
        scriptUrl: String,
        phoneNumber: String,
        callerName: String,
        date: String,
        time: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Build JSON payload
        val json = JSONObject().apply {
            put("phoneNumber", phoneNumber)
            put("callerName", callerName)
            put("date", date)
            put("time", time)
            put("callType", "Missed")
            put("deviceId", android.os.Build.MODEL)
        }

        Log.d(TAG, "📤 Sending to Google Sheet: $json")

        val requestBody = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(scriptUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""
                    Log.d(TAG, "📥 Response: $responseBody")

                    if (it.isSuccessful) {
                        try {
                            val responseJson = JSONObject(responseBody)
                            if (responseJson.optString("status") == "success") {
                                onSuccess()
                            } else {
                                onFailure("Server returned: ${responseJson.optString("message")}")
                            }
                        } catch (e: Exception) {
                            // Apps Script sometimes returns HTML on redirect
                            // If we got 200, consider it success
                            onSuccess()
                        }
                    } else {
                        onFailure("HTTP ${it.code}: $responseBody")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Network error: ${e.message}")
                onFailure(e.message ?: "Unknown network error")
            }
        })
    }

    /**
     * Tests the connection to the Google Apps Script
     */
    fun testConnection(
        scriptUrl: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val request = Request.Builder()
            .url(scriptUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: ""
                    onResult(it.isSuccessful, body)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message ?: "Connection failed")
            }
        })
    }
}
