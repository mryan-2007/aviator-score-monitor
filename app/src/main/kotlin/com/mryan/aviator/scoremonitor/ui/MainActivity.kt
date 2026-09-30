package com.mryan.aviator.scoremonitor.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mryan.aviator.scoremonitor.R
import com.mryan.aviator.scoremonitor.service.ScreenCaptureService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var captureService: ScreenCaptureService? = null
    private var isServiceBound = false

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var thresholdInput: EditText
    private lateinit var consecutiveCountInput: EditText
    private lateinit var statusText: TextView
    private lateinit var streakText: TextView
    private lateinit var lastValueText: TextView
    private lateinit var alertCountText: TextView
    private lateinit var logText: TextView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            captureService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            captureService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        bindService(
            Intent(this, ScreenCaptureService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        startButton.setOnClickListener { startMonitoring() }
        stopButton.setOnClickListener { stopMonitoring() }
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        thresholdInput = findViewById(R.id.thresholdInput)
        consecutiveCountInput = findViewById(R.id.consecutiveCountInput)
        statusText = findViewById(R.id.statusText)
        streakText = findViewById(R.id.streakText)
        lastValueText = findViewById(R.id.lastValueText)
        alertCountText = findViewById(R.id.alertCountText)
        logText = findViewById(R.id.logText)

        stopButton.isEnabled = false
    }

    private fun startMonitoring() {
        val threshold = thresholdInput.text.toString().toFloatOrNull() ?: 2.0f
        val consecutiveCount = consecutiveCountInput.text.toString().toIntOrNull() ?: 7

        if (captureService == null || !isServiceBound) {
            logMessage("Error: Service not ready")
            return
        }

        captureService!!.startMonitoring(
            threshold = threshold,
            consecutiveCount = consecutiveCount,
            roiLeft = 10,
            roiTop = 10,
            roiWidth = 300,
            roiHeight = 100
        ) { text, lastValue, streak, alertCount ->
            runOnUiThread {
                statusText.text = "Running"
                streakText.text = streak.toString()
                lastValueText.text = lastValue?.let { String.format("%.2f", it) } ?: "--"
                alertCountText.text = alertCount.toString()

                if (text.isNotBlank()) {
                    logMessage("OCR: $text | Last: ${String.format("%.2f", lastValue ?: 0f)} | Streak: $streak")
                }
            }
        }

        startButton.isEnabled = false
        stopButton.isEnabled = true
        thresholdInput.isEnabled = false
        consecutiveCountInput.isEnabled = false
        logMessage("Monitoring started | Threshold: $threshold | Count: $consecutiveCount")
    }

    private fun stopMonitoring() {
        captureService?.stopMonitoring()
        statusText.text = "Stopped"
        streakText.text = "0"
        lastValueText.text = "--"

        startButton.isEnabled = true
        stopButton.isEnabled = false
        thresholdInput.isEnabled = true
        consecutiveCountInput.isEnabled = true
        logMessage("Monitoring stopped")
    }

    private fun logMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentLog = logText.text.toString()
        val newLog = if (currentLog.contains("Logs will appear here")) {
            "[$timestamp] $message"
        } else {
            "[$timestamp] $message\n$currentLog"
        }
        logText.text = newLog
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
