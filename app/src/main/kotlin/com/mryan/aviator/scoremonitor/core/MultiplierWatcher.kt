package com.mryan.aviator.scoremonitor.core

/**
 * Core logic for monitoring multiplier values and detecting consecutive low values.
 */
class MultiplierWatcher(
    private var threshold: Float = 2.0f,
    private var requiredStreak: Int = 7
) {
    private var lastValue: Float? = null
    private var lowStreak = 0
    var alertTriggeredCount = 0
        private set

    fun updateThreshold(newThreshold: Float) {
        threshold = newThreshold
    }

    fun updateRequiredStreak(newStreak: Int) {
        requiredStreak = newStreak
    }

    /**
     * Process a recognized multiplier value and return true if alert should trigger.
     */
    fun processValue(rawText: String): Boolean {
        val parsed = parseMultiplier(rawText) ?: return false

        // Only count when value changes
        if (parsed != lastValue) {
            if (parsed < threshold) {
                lowStreak += 1
            } else {
                lowStreak = 0
            }
        }

        lastValue = parsed

        if (lowStreak >= requiredStreak) {
            lowStreak = 0
            alertTriggeredCount += 1
            return true
        }
        return false
    }

    fun getLastValue(): Float? = lastValue

    fun getCurrentStreak(): Int = lowStreak

    fun resetStreak() {
        lowStreak = 0
        lastValue = null
    }

    /**
     * Clean and parse multiplier string like "1.85x" to Float.
     */
    private fun parseMultiplier(raw: String): Float? {
        if (raw.isBlank()) return null

        val cleaned = raw
            .replace("x", "", ignoreCase = true)
            .replace("X", "", ignoreCase = true)
            .replace(",", ".")
            .replace(" ", "")
            .replace("O", "0")
            .replace("o", "0")
            .replace("I", "1")
            .replace("l", "1")
            .trim()

        return cleaned.toFloatOrNull()
    }
}
