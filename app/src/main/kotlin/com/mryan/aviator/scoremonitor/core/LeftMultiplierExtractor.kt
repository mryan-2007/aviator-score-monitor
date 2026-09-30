package com.mryan.aviator.scoremonitor.core

/**
 * Extracts and parses only the LEFT (first) multiplier value from OCR text.
 * Aviator displays: "1.85× 12×" and we only want "1.85"
 */
class LeftMultiplierExtractor {

    /**
     * Extract only the LEFT multiplier from text like "1.85× 12×" → "1.85"
     */
    fun extractLeftMultiplier(rawText: String): Float? {
        if (rawText.isBlank()) return null

        // Split by common delimiters or multiple spaces
        val parts = rawText
            .replace("x", "X", ignoreCase = false)
            .split(Regex("[\\s×XxOo]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Get the first valid number
        for (part in parts) {
            val value = cleanAndParse(part)
            if (value != null && value > 0) {
                return value
            }
        }

        return null
    }

    /**
     * Clean up OCR errors and parse a single multiplier value.
     * Handles: "1.85", "l.85", "1,85", "185" (missing decimal), etc.
     */
    private fun cleanAndParse(raw: String): Float? {
        val cleaned = raw
            .replace(",", ".")          // Replace comma with dot
            .replace("O", "0")          // O → 0
            .replace("o", "0")          // o → 0
            .replace("I", "1")          // I → 1
            .replace("l", "1")          // l → 1
            .replace("S", "5")          // S → 5
            .replace("Z", "2")          // Z → 2
            .replace(Regex("[^0-9.]"), "") // Remove all non-digits and dots
            .trim()

        if (cleaned.isBlank()) return null

        // Handle missing decimal: "185" → "1.85"
        val normalized = if (cleaned.contains(".")) {
            cleaned
        } else if (cleaned.length >= 2) {
            cleaned.substring(0, cleaned.length - 2) + "." + cleaned.substring(cleaned.length - 2)
        } else {
            cleaned
        }

        return normalized.toFloatOrNull()
    }
}
