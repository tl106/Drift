package com.drift.sleep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.drift.sleep.MainActivity
import com.drift.sleep.R
import com.drift.sleep.data.DriftDatabase
import com.drift.sleep.data.DriftPreferences
import com.drift.sleep.data.SleepRecord
import kotlin.math.sqrt

class DriftService : Service(), SensorEventListener {

    companion object {
        const val TAG = "DriftService"
        const val CHANNEL_ID = "drift_service"
        const val NOTIFICATION_ID = 1
        const val RESULT_CHANNEL_ID = "drift_result"
        const val RESULT_NOTIFICATION_ID = 2

        const val ACTION_STOP = "com.drift.sleep.STOP"

        var isRunning = false
            private set
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler

    private var waitMinutes = 25L
    private var fadeMinutes = 10L

    private var lastActivityTime = 0L
    private var isFading = false
    private var originalVolume = 0
    private var fadeStartTime = 0L
    private var startTime = 0L
    private var pausedApp: String? = null
    private var isRestoring = false
    private var restoreTargetVolume = 0

    // Shake detection
    private var lastShakeTime = 0L
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var lastAccelTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON - user active")
                    onActivityDetected("screen_on")
                }
            }
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isFading) {
                Log.d(TAG, "Volume changed by user")
                onActivityDetected("volume_change")
            }
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkInactivity()
            handler.postDelayed(this, 5000)
        }
    }

    private val fadeRunnable = object : Runnable {
        override fun run() {
            if (!isFading) return
            performFadeStep()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Read config from preferences
        val prefs = DriftPreferences(this)
        waitMinutes = prefs.waitMinutes
        fadeMinutes = prefs.fadeMinutes

        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildNotification("正在监听..."))

        startDetection()
        isRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
        isRunning = false

        if (isFading) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            isFading = false
        }

        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Detection Logic ---

    private fun startDetection() {
        lastActivityTime = System.currentTimeMillis()
        startTime = System.currentTimeMillis()
        isFading = false

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)

        val volumeFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, volumeFilter)

        handler.post(checkRunnable)

        Log.d(TAG, "Detection started. Wait: ${waitMinutes}min, Fade: ${fadeMinutes}min")
    }

    private fun stopDetection() {
        sensorManager.unregisterListener(this)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(volumeReceiver) } catch (_: Exception) {}
        handler.removeCallbacks(checkRunnable)
        handler.removeCallbacks(fadeRunnable)
        handler.removeCallbacks(restoreRunnable)
    }

    // --- Gradual Volume Restore ---

    private val restoreRunnable = object : Runnable {
        override fun run() {
            if (!isRestoring) return
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (current >= restoreTargetVolume) {
                isRestoring = false
                Log.d(TAG, "Volume restored to $restoreTargetVolume")
                return
            }
            // Increase by 1 step every 667ms → ~10 seconds to restore full volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current + 1, 0)
            handler.postDelayed(this, 667)
        }
    }

    private fun startGradualRestore() {
        restoreTargetVolume = originalVolume
        isRestoring = true
        handler.post(restoreRunnable)
    }

    private fun onActivityDetected(source: String) {
        lastActivityTime = System.currentTimeMillis()

        if (isFading) {
            Log.d(TAG, "Activity during fade ($source) - restoring volume gradually")
            isFading = false
            handler.removeCallbacks(fadeRunnable)
            startGradualRestore()
            updateNotification("检测到活动，重新开始等待...")
        } else {
            Log.d(TAG, "Activity detected ($source) - timer reset")
        }
    }

    private fun checkInactivity() {
        if (isFading) return

        val elapsed = System.currentTimeMillis() - lastActivityTime
        val waitMs = waitMinutes * 60 * 1000

        if (elapsed >= waitMs) {
            startFade()
        } else {
            val remainingMin = (waitMs - elapsed) / 60000
            updateNotification("已安静 ${elapsed / 60000} 分钟，还需 ${remainingMin} 分钟")
        }
    }

    // --- Fade Logic ---

    private fun startFade() {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (originalVolume == 0) {
            pauseMedia()
            saveRecord()
            showResultNotification()
            stopSelf()
            return
        }

        isFading = true
        fadeStartTime = System.currentTimeMillis()
        updateNotification("正在渐弱音量...")
        Log.d(TAG, "Starting fade. Original volume: $originalVolume, Fade time: ${fadeMinutes}min")

        handler.post(fadeRunnable)
    }

    private fun performFadeStep() {
        val elapsed = System.currentTimeMillis() - fadeStartTime
        val fadeMs = fadeMinutes * 60 * 1000
        val progress = (elapsed.toFloat() / fadeMs).coerceIn(0f, 1f)
        val targetVolume = (originalVolume * (1f - progress)).toInt()

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)

        if (progress >= 1f) {
            isFading = false
            handler.removeCallbacks(fadeRunnable)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            pauseMedia()
            saveRecord()
            showResultNotification()
            Log.d(TAG, "Fade complete. Media paused.")
            stopSelf()
        }
    }

    private fun pauseMedia() {
        pausedApp = MediaControlService.pauseCurrentMedia()
    }

    private fun saveRecord() {
        val record = SleepRecord(
            startTime = startTime,
            pauseTime = System.currentTimeMillis(),
            pausedApp = pausedApp
        )
        Thread {
            DriftDatabase.getInstance(this).sleepRecordDao().insert(record)
            Log.d(TAG, "Sleep record saved")
        }.start()
    }

    // --- Accelerometer / Shake Detection ---

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()

        if (lastAccelTime > 0) {
            val dt = now - lastAccelTime
            if (dt > 0) {
                val dx = x - lastAccelX
                val dy = y - lastAccelY
                val dz = z - lastAccelZ
                val acceleration = sqrt((dx * dx + dy * dy + dz * dz).toDouble()) / dt * 1000

                if (acceleration > 25 && now - lastShakeTime > 2000) {
                    lastShakeTime = now
                    onShakeDetected()
                }

                if (acceleration > 5 && now - lastActivityTime > 3000) {
                    onActivityDetected("accelerometer")
                }
            }
        }

        lastAccelX = x
        lastAccelY = y
        lastAccelZ = z
        lastAccelTime = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onShakeDetected() {
        Log.d(TAG, "Shake detected!")
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        onActivityDetected("shake")
    }

    // --- Notifications ---

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Drift 运行状态", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Drift正在后台监测睡眠状态" }
        manager.createNotificationChannel(serviceChannel)

        val resultChannel = NotificationChannel(
            RESULT_CHANNEL_ID, "Drift 睡眠报告", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Drift的睡眠检测结果" }
        manager.createNotificationChannel(resultChannel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, DriftService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drift")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_notification, "停止", stopPending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun showResultNotification() {
        val startHour = android.text.format.DateFormat.format("HH:mm", startTime)
        val endHour = android.text.format.DateFormat.format("HH:mm", System.currentTimeMillis())
        val appName = pausedApp ?: "媒体"

        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setContentTitle("晚安 \uD83C\uDF19")
            .setContentText("$startHour 开始监听，$endHour 帮你暂停了$appName。")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RESULT_NOTIFICATION_ID, notification)
    }
}
