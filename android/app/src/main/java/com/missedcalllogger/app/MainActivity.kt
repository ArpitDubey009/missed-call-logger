package com.missedcalllogger.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.missedcalllogger.app.service.MissedCallService
import com.missedcalllogger.app.util.AppConfig
import com.missedcalllogger.app.util.PrefsManager

/**
 * MainActivity — The main UI for the Missed Call Logger app.
 * 
 * Features:
 * - Configure Google Apps Script URL
 * - Start/Stop the monitoring service
 * - View status and stats
 * - Request all required permissions
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    
    private lateinit var tvStatus: TextView
    private lateinit var tvStats: TextView
    private lateinit var etScriptUrl: EditText
    private lateinit var btnToggleService: Button
    private lateinit var btnSaveUrl: Button
    private lateinit var btnTestConnection: Button

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)
        initViews()
        checkAndRequestPermissions()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStats = findViewById(R.id.tvStats)
        etScriptUrl = findViewById(R.id.etScriptUrl)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnSaveUrl = findViewById(R.id.btnSaveUrl)
        btnTestConnection = findViewById(R.id.btnTestConnection)

        // Load saved URL (pre-filled with deployed URL if not set)
        val savedUrl = prefs.getScriptUrl()
        etScriptUrl.setText(if (savedUrl.isNotEmpty()) savedUrl else AppConfig.SCRIPT_URL)

        // Save URL button
        btnSaveUrl.setOnClickListener {
            val url = etScriptUrl.text.toString().trim()
            if (url.isNotEmpty() && url.startsWith("https://script.google.com")) {
                prefs.setScriptUrl(url)
                Toast.makeText(this, "✅ URL saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Please enter a valid Google Apps Script URL", Toast.LENGTH_LONG).show()
            }
        }

        // Toggle service button
        btnToggleService.setOnClickListener {
            if (prefs.getScriptUrl().isEmpty()) {
                Toast.makeText(this, "⚠️ Please save your Google Apps Script URL first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (prefs.isServiceRunning()) {
                stopMonitoringService()
            } else {
                startMonitoringService()
            }
            updateUI()
        }

        // Test connection button
        btnTestConnection.setOnClickListener {
            testConnection()
        }
    }

    private fun updateUI() {
        val isRunning = prefs.isServiceRunning()
        
        // Status indicator
        if (isRunning) {
            tvStatus.text = "🟢 Monitoring Active"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnToggleService.text = "⏹ Stop Monitoring"
        } else {
            tvStatus.text = "🔴 Monitoring Stopped"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnToggleService.text = "▶️ Start Monitoring"
        }

        // Stats
        val totalLogged = prefs.getTotalLogged()
        val lastCall = prefs.getLastMissedCall()
        val pendingSync = prefs.getPendingSyncCount()
        
        tvStats.text = buildString {
            append("📊 Total Calls Logged: $totalLogged\n")
            append("📞 Last Missed Call: $lastCall\n")
            if (pendingSync > 0) {
                append("⏳ Pending Sync: $pendingSync calls")
            }
        }
    }

    // ====== SERVICE MANAGEMENT ======

    private fun startMonitoringService() {
        val intent = Intent(this, MissedCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        prefs.setServiceRunning(true)
        
        // Request battery optimization exemption
        requestBatteryOptimization()
        
        Toast.makeText(this, "✅ Monitoring started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoringService() {
        val intent = Intent(this, MissedCallService::class.java)
        stopService(intent)
        prefs.setServiceRunning(false)
        Toast.makeText(this, "⏹ Monitoring stopped.", Toast.LENGTH_SHORT).show()
    }

    // ====== PERMISSIONS ======

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, 
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
                    PERMISSION_REQUEST_CODE + 1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("The app needs phone, call log, and contacts permissions to detect and log missed calls. Please grant all permissions.")
                    .setPositiveButton("Grant") { _, _ ->
                        checkAndRequestPermissions()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    // ====== BATTERY OPTIMIZATION ======

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Battery Optimization")
                    .setMessage("For reliable background monitoring, please disable battery optimization for this app.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    // ====== TEST CONNECTION ======

    private fun testConnection() {
        val url = prefs.getScriptUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "⚠️ Please save URL first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "🔄 Testing connection...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val testUrl = java.net.URL(url)
                val connection = testUrl.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().readText()

                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "✅ Connection successful!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "❌ Error: HTTP $responseCode", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "❌ Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
