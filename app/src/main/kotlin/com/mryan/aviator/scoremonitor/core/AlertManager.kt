package com.mryan.aviator.scoremonitor.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Manages alert actions (vibration) when threshold is breached.
 */
class AlertManager(private val context: Context) {
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    /**
     * Trigger vibration alert.
     */
    fun triggerAlert() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        800,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(800)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Trigger multiple vibrations for emphasis.
     */
    fun triggerMultipleVibrations(count: Int = 3, durationMs: Long = 200, delayMs: Long = 100) {
        for (i in 0 until count) {
            Thread {
                Thread.sleep(i * (durationMs + delayMs))
                triggerAlert()
            }.start()
        }
    }
}
