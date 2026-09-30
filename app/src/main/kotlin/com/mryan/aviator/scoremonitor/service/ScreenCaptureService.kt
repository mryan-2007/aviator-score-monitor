package com.mryan.aviator.scoremonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.Display
import android.view.WindowManager
import com.mryan.aviator.scoremonitor.R
import com.mryan.aviator.scoremonitor.core.AlertManager
import com.mryan.aviator.scoremonitor.core.MultiplierWatcher
import com.mryan.aviator.scoremonitor.core.OcrProcessor
import com.mryan.aviator.scoremonitor.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Background service for continuous screen monitoring.
 */
class ScreenCaptureService : Service() {
    private val binder = LocalBinder()
    private lateinit var displayManager: DisplayManager
    private var imageReader: ImageReader? = null
    private lateinit var ocrProcessor: OcrProcessor
    private lateinit var watcher: MultiplierWatcher
    private lateinit var alertManager: AlertManager

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var isRunning = false
    private var captureCallback: ((String, Float?, Int, Int) -> Unit)? = null

    // ROI coordinates (adjust based on your screen)
    private var roiLeft = 10
    private var roiTop = 10
    private var roiWidth = 200
    private var roiHeight = 80

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        ocrProcessor = OcrProcessor()
        watcher = MultiplierWatcher()
        alertManager = AlertManager(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startMonitoring(
        threshold: Float = 2.0f,
        consecutiveCount: Int = 7,
        roiLeft: Int = 10,
        roiTop: Int = 10,
        roiWidth: Int = 200,
        roiHeight: Int = 80,
        callback: (text: String, lastValue: Float?, streak: Int, alertCount: Int) -> Unit
    ) {
        if (isRunning) return

        this.roiLeft = roiLeft
        this.roiTop = roiTop
        this.roiWidth = roiWidth
        this.roiHeight = roiHeight
        captureCallback = callback

        watcher.updateThreshold(threshold)
        watcher.updateRequiredStreak(consecutiveCount)

        startForegroundNotification()
        setupScreenCapture()
        isRunning = true
        startCapturingFrames()
    }

    fun stopMonitoring() {
        isRunning = false
        stopForegroundNotification()
        releaseScreenCapture()
        watcher.resetStreak()
    }

    private fun setupScreenCapture() {
        captureThread = HandlerThread("CaptureThread").apply { start() }
        captureHandler = Handler(captureThread!!.looper)

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val width = display.mode.physicalWidth
        val height = display.mode.physicalHeight

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    }

    private fun releaseScreenCapture() {
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
    }

    private fun startCapturingFrames() {
        scope.launch {
            while (isActive && isRunning) {
                try {
                    captureAndProcess()
                    delay(100) // ~10 captures per second
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun captureAndProcess() {
        val imageReader = imageReader ?: return
        val image = imageReader.acquireLatestImage() ?: return

        try {
            val bitmap = imageToBitmap(image)
            val croppedBitmap = ocrProcessor.cropBitmap(bitmap, roiLeft, roiTop, roiWidth, roiHeight)

            val text = ocrProcessor.extractText(croppedBitmap, preprocess = true)
            val shouldAlert = watcher.processValue(text)

            if (shouldAlert) {
                alertManager.triggerAlert()
            }

            captureCallback?.invoke(
                text,
                watcher.getLastValue(),
                watcher.getCurrentStreak(),
                watcher.alertTriggeredCount
            )
        } finally {
            image.close()
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        buffer.rewind()
        val pixelStride = planes[0].pixelStride
        val padding = planes[0].rowPadding
        val w = image.width + padding / pixelStride
        val bitmap = Bitmap.createBitmap(w, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun startForegroundNotification() {
        val notificationId = 1
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "aviator_channel")
                .setContentTitle("Aviator Monitor")
                .setContentText("Monitoring score multipliers...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Aviator Monitor")
                .setContentText("Monitoring score multipliers...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build()
        }

        startForeground(notificationId, notification)
    }

    private fun stopForegroundNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "aviator_channel",
                "Aviator Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Aviator score monitor"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }
}
