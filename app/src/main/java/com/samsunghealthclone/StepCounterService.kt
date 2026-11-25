package com.samsunghealthclone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.samsunghealthclone.data.HealthDatabase
import com.samsunghealthclone.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepCounterService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stepCount = 0
    private var lastSavedSteps = 0
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeSensors()
        acquireWakeLock()
        createNotificationChannel()
        startForegroundService()
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StepCounterService::WakeLock")
        wakeLock?.acquire()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "step_counter_channel",
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "step_counter_channel")
            .setContentTitle("Samsung Health Clone")
            .setContentText("Tracking steps: $stepCount")
            .setSmallIcon(R.drawable.ic_steps)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, "step_counter_channel")
            .setContentTitle("Samsung Health Clone")
            .setContentText("Steps today: $stepCount")
            .setSmallIcon(R.drawable.ic_steps)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSteps = event.values[0].toInt()
            if (currentSteps > stepCount) {
                stepCount = currentSteps
                saveStepsIfNeeded()
                updateNotification()
            }
        }
    }

    private fun saveStepsIfNeeded() {
        if (stepCount - lastSavedSteps >= 10) { // Save every 10 steps
            lastSavedSteps = stepCount
            repositoryScope.launch {
                val app = applicationContext as HealthApplication
                val today = com.samsunghealthclone.data.StepEntry.getDayStart()
                val entry = app.getRepository().getEntryForDateSync(today) ?: com.samsunghealthclone.data.StepEntry(date = today)
                entry.steps = stepCount
                entry.lastUpdated = System.currentTimeMillis()
                app.getRepository().updateCalculatedFields(entry)
                app.getRepository().insertOrUpdate(entry)
            }
        }
    }

    private fun insertOrUpdate(entry: com.samsunghealthclone.data.StepEntry) {
        // This will be called from coroutine scope
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        wakeLock?.release()
        stopForeground(true)
        repositoryScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}