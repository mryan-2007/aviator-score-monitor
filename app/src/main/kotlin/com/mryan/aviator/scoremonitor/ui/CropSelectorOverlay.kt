package com.mryan.aviator.scoremonitor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Floating overlay view for selecting the crop area for the multiplier box.
 */
class CropSelectorOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var isSelecting = false

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        alpha = 100
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 16f
    }

    var onCropSelected: ((left: Int, top: Int, width: Int, height: Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isSelecting && startX > 0 && startY > 0) {
            val left = minOf(startX, endX).toInt()
            val top = minOf(startY, endY).toInt()
            val right = maxOf(startX, endX).toInt()
            val bottom = maxOf(startY, endY).toInt()

            val width = right - left
            val height = bottom - top

            // Draw semi-transparent selection
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), selectionPaint)

            // Draw border
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), borderPaint)

            // Draw dimensions text
            canvas.drawText("$width x $height", left + 10f, top + 30f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isSelecting = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                endX = event.x
                endY = event.y
                isSelecting = false

                val left = minOf(startX, endX).toInt()
                val top = minOf(startY, endY).toInt()
                val width = (maxOf(startX, endX) - minOf(startX, endX)).toInt()
                val height = (maxOf(startY, endY) - minOf(startY, endY)).toInt()

                if (width > 50 && height > 30) {
                    onCropSelected?.invoke(left, top, width, height)
                }
                invalidate()
                return true
            }
        }
        return false
    }

    fun reset() {
        startX = 0f
        startY = 0f
        endX = 0f
        endY = 0f
        isSelecting = false
        invalidate()
    }
}
