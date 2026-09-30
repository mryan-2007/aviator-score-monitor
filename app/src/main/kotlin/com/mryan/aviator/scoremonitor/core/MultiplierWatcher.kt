package com.mryan.aviator.scoremonitor.core

/**
 * Core logic for monitoring multiplier values and detecting consecutive low values.
 * Tracks ONLY the LEFT (first) multiplier from the score bar.
 */
class MultiplierWatcher(
    private var threshold: Float = 2.0f,
    private var requiredStreak: Int = 7
) {
    private val extractor = LeftMultiplierExtractor()
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
     * Process OCR text and return true if alert should trigger.
     * Only counts the LEFT multiplier value.
     */
    fun processValue(rawText: String): Boolean {
        val parsed = extractor.extractLeftMultiplier(rawText) ?: return false

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
}
