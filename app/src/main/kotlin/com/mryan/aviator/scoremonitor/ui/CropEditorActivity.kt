package com.mryan.aviator.scoremonitor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mryan.aviator.scoremonitor.R
import kotlin.math.abs

class CropEditorActivity : AppCompatActivity() {

    private lateinit var cropView: CropEditorView
    private var cropRect = RectF(50f, 50f, 350f, 150f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_editor)

        cropView = findViewById(R.id.cropView)
        val confirmButton = findViewById<View>(R.id.confirmButton)
        val cancelButton = findViewById<View>(R.id.cancelButton)

        // Set initial crop area
        cropView.setCropRect(cropRect)

        // Capture the current screen
        captureScreenshot()

        confirmButton.setOnClickListener {
            val finalRect = cropView.getCropRect()
            val intent = Intent()
            intent.putExtra("roiLeft", finalRect.left.toInt())
            intent.putExtra("roiTop", finalRect.top.toInt())
            intent.putExtra("roiWidth", finalRect.width().toInt())
            intent.putExtra("roiHeight", finalRect.height().toInt())
            setResult(RESULT_OK, intent)
            finish()
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun captureScreenshot() {
        // Note: Requires android.permission.READ_LOGS or accessibility service
        // For now, we'll create a placeholder
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        cropView.setScreenBitmap(bitmap)
    }
}

class CropEditorView(context: android.content.Context, attrs: android.util.AttributeSet? = null) :
    View(context, attrs) {

    private var screenBitmap: Bitmap? = null
    private var cropRect = RectF(50f, 50f, 350f, 150f)
    private var selectedHandle: Handle? = null

    private val cropPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 3f
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
        alpha = 50
    }

    private val handlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    private val handleRadius = 30f
    private val minWidth = 50f
    private val minHeight = 30f

    enum class Handle {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT, CENTER
    }

    fun setScreenBitmap(bitmap: Bitmap) {
        screenBitmap = bitmap
        invalidate()
    }

    fun setCropRect(rect: RectF) {
        cropRect = rect
        invalidate()
    }

    fun getCropRect(): RectF = cropRect

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw dim background
        canvas.drawColor(Color.parseColor("#00000080"))

        // Draw crop area with semi-transparent fill
        canvas.drawRect(cropRect, fillPaint)
        canvas.drawRect(cropRect, cropPaint)

        // Draw dimension text
        val text = "${cropRect.width().toInt()} × ${cropRect.height().toInt()}"
        canvas.drawText(text, cropRect.centerX(), cropRect.top - 20f, textPaint)

        // Draw corner handles
        drawHandle(canvas, cropRect.left, cropRect.top)
        drawHandle(canvas, cropRect.right, cropRect.top)
        drawHandle(canvas, cropRect.left, cropRect.bottom)
        drawHandle(canvas, cropRect.right, cropRect.bottom)

        // Draw edge handles
        drawHandle(canvas, cropRect.centerX(), cropRect.top)
        drawHandle(canvas, cropRect.centerX(), cropRect.bottom)
        drawHandle(canvas, cropRect.left, cropRect.centerY())
        drawHandle(canvas, cropRect.right, cropRect.centerY())
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedHandle = getHandleAtPoint(x, y)
                return selectedHandle != null
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedHandle != null) {
                    updateCropRect(x, y)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedHandle = null
                return true
            }
        }
        return false
    }

    private fun getHandleAtPoint(x: Float, y: Float): Handle? {
        return when {
            abs(x - cropRect.left) < handleRadius && abs(y - cropRect.top) < handleRadius -> Handle.TOP_LEFT
            abs(x - cropRect.right) < handleRadius && abs(y - cropRect.top) < handleRadius -> Handle.TOP_RIGHT
            abs(x - cropRect.left) < handleRadius && abs(y - cropRect.bottom) < handleRadius -> Handle.BOTTOM_LEFT
            abs(x - cropRect.right) < handleRadius && abs(y - cropRect.bottom) < handleRadius -> Handle.BOTTOM_RIGHT
            abs(x - cropRect.centerX()) < handleRadius && abs(y - cropRect.top) < handleRadius -> Handle.TOP
            abs(x - cropRect.centerX()) < handleRadius && abs(y - cropRect.bottom) < handleRadius -> Handle.BOTTOM
            abs(x - cropRect.left) < handleRadius && abs(y - cropRect.centerY()) < handleRadius -> Handle.LEFT
            abs(x - cropRect.right) < handleRadius && abs(y - cropRect.centerY()) < handleRadius -> Handle.RIGHT
            x > cropRect.left && x < cropRect.right && y > cropRect.top && y < cropRect.bottom -> Handle.CENTER
            else -> null
        }
    }

    private fun updateCropRect(x: Float, y: Float) {
        when (selectedHandle) {
            Handle.TOP_LEFT -> {
                cropRect.left = minOf(x, cropRect.right - minWidth)
                cropRect.top = minOf(y, cropRect.bottom - minHeight)
            }
            Handle.TOP_RIGHT -> {
                cropRect.right = maxOf(x, cropRect.left + minWidth)
                cropRect.top = minOf(y, cropRect.bottom - minHeight)
            }
            Handle.BOTTOM_LEFT -> {
                cropRect.left = minOf(x, cropRect.right - minWidth)
                cropRect.bottom = maxOf(y, cropRect.top + minHeight)
            }
            Handle.BOTTOM_RIGHT -> {
                cropRect.right = maxOf(x, cropRect.left + minWidth)
                cropRect.bottom = maxOf(y, cropRect.top + minHeight)
            }
            Handle.TOP -> cropRect.top = minOf(y, cropRect.bottom - minHeight)
            Handle.BOTTOM -> cropRect.bottom = maxOf(y, cropRect.top + minHeight)
            Handle.LEFT -> cropRect.left = minOf(x, cropRect.right - minWidth)
            Handle.RIGHT -> cropRect.right = maxOf(x, cropRect.left + minWidth)
            Handle.CENTER -> {
                val dx = x - cropRect.centerX()
                val dy = y - cropRect.centerY()
                cropRect.offset(dx, dy)
            }
            null -> {}
        }
    }
}
