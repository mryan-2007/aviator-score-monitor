package com.mryan.aviator.scoremonitor.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mryan.aviator.scoremonitor.R

class MainActivity : AppCompatActivity() {

    private lateinit var thresholdInput: EditText
    private lateinit var streakInput: EditText
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private var isMonitoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views by ID
        thresholdInput = findViewById(R.id.thresholdInput)
        streakInput = findViewById(R.id.streakInput)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)

        // Set default values
        thresholdInput.setText("2.00")
        streakInput.setText("7")
        statusText.text = "Ready to start"

        // Start button click listener
        startButton.setOnClickListener {
            if (isMonitoring) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    private fun startMonitoring() {
        // Get values from input fields
        val thresholdText = thresholdInput.text.toString()
        val streakText = streakInput.text.toString()

        // Validate inputs
        if (thresholdText.isEmpty() || streakText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val threshold = thresholdText.toFloatOrNull()
        val streak = streakText.toIntOrNull()

        if (threshold == null || streak == null) {
            Toast.makeText(this, "Invalid input format", Toast.LENGTH_SHORT).show()
            return
        }

        if (threshold <= 0 || streak <= 0) {
            Toast.makeText(this, "Values must be positive", Toast.LENGTH_SHORT).show()
            return
        }

        // Update UI
        isMonitoring = true
        startButton.text = "Stop Monitoring"
        statusText.text = "Monitoring active...\nThreshold: $threshold\nStreak: $streak"
        thresholdInput.isEnabled = false
        streakInput.isEnabled = false

        Toast.makeText(this, "Monitoring started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        isMonitoring = false
        startButton.text = "Start Monitoring"
        statusText.text = "Monitoring stopped"
        thresholdInput.isEnabled = true
        streakInput.isEnabled = true

        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
    }
}
