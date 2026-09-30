package com.mryan.aviator.scoremonitor.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Handles OCR text recognition on screen captures.
 */
class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from a bitmap, optionally preprocessing it.
     */
    suspend fun extractText(bitmap: Bitmap, preprocess: Boolean = true): String {
        return try {
            val processed = if (preprocess) preprocessBitmap(bitmap) else bitmap
            val image = InputImage.fromBitmap(processed, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Crop a specific ROI (region of interest) from the bitmap.
     * Parameters are in pixels.
     */
    fun cropBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        val right = minOf(left + width, bitmap.width)
        val bottom = minOf(top + height, bitmap.height)
        val actualWidth = right - left
        val actualHeight = bottom - top

        return if (actualWidth > 0 && actualHeight > 0) {
            Bitmap.createBitmap(bitmap, left, top, actualWidth, actualHeight)
        } else {
            bitmap
        }
    }

    /**
     * Preprocess bitmap for better OCR results:
     * - Convert to grayscale
     * - Increase contrast
     * - Threshold to binary
     */
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // Convert to grayscale
        val gray = toGrayscale(bitmap)

        // Apply contrast enhancement
        val enhanced = enhanceContrast(gray, 1.5f)

        // Apply threshold
        return thresholdBitmap(enhanced, 150)
    }

    /**
     * Convert bitmap to grayscale.
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * Enhance contrast of the bitmap.
     */
    private fun enhanceContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

            val adjusted = ((gray - 128) * contrast + 128).toInt().coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (adjusted shl 16) or (adjusted shl 8) or adjusted
        }

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }

    /**
     * Apply binary threshold to bitmap.
     */
    private fun thresholdBitmap(bitmap: Bitmap, threshold: Int): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = ((pixel shr 16) and 0xFF)
            val binary = if (gray > threshold) 255 else 0
            pixels[i] = (0xFF shl 24) or (binary shl 16) or (binary shl 8) or binary
        }

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
}
