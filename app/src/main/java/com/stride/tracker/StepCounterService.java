package com.stride.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import java.util.Locale;

public final class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "stride_steps";
    private static final int NOTIFICATION_ID = 42;

    private SensorManager sensorManager;
    private Sensor stepDetector;
    private boolean listenerRegistered;
    private long lastNotificationUpdate;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetector = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (stepDetector == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (!listenerRegistered) {
            listenerRegistered = sensorManager.registerListener(
                    this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_STEP_DETECTOR) return;
        int steps = event.values.length == 0 ? 1 : Math.max(1, Math.round(event.values[0]));
        StepStore.recordSensorSteps(this, steps);

        long now = System.currentTimeMillis();
        if (now - lastNotificationUpdate >= 2500L) {
            lastNotificationUpdate = now;
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Step detector accuracy does not change the saved count.
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        listenerRegistered = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Подсчёт шагов", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Подсчёт шагов в фоновом режиме");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Шаги")
                .setContentText("Сегодня: " + String.format(Locale.getDefault(), "%,d", StepStore.todaySteps(this)))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .build();
    }
}
