package com.healthcare.aktivsens;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SensorForegroundService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "SensorForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final float MOVEMENT_THRESHOLD = 12.5f; // Adjust this
    private static final long MOVEMENT_TIME_THRESHOLD = 500; // Adjust this (in milliseconds)

    private long lastMovementTime = 0;
    private int stepCount = 0;

    private final IBinder binder = new LocalBinder();
    private boolean isPaused = false;
    private boolean isForegroundService = false;

    public class LocalBinder extends Binder {
        public SensorForegroundService getService() {
            return SensorForegroundService.this;
        }
    }

    private SensorCallback sensorCallback;

    public void setSensorCallback(SensorCallback callback) {
        this.sensorCallback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startSensorListener();
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        stepCount = retrieveFinCount();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSensorListener();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startSensorListener() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Accelerometer sensor not available on the device
            // Handle this case accordingly
        }
    }

    private void stopSensorListener() {
        sensorManager.unregisterListener(this);
    }

    public void notifyMovementDetected(int stepCount) {
        this.stepCount = stepCount;
        if (sensorCallback != null) {
            sensorCallback.onStepCountUpdated(stepCount);
        }
        Notification notification = createNotification();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate the magnitude of acceleration vector
            double magnitude = Math.sqrt(x * x + y * y + z * z);

            long currentTime = System.currentTimeMillis();
            if (magnitude > MOVEMENT_THRESHOLD && currentTime - lastMovementTime > MOVEMENT_TIME_THRESHOLD) {
                // Movement detected
                stepCount++;
                notifyMovementDetected(stepCount);

                lastMovementTime = currentTime;
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Foreground service channel");
            channel.setLightColor(Color.GREEN);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        int count = stepCount;
        String contentText;
        if (isPaused) {
            contentText = "PAUSED";
        } else {
            contentText = "Detecting Movement Count: " + count;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class); // Update with your MainActivity class

        // Use PendingIntent.FLAG_IMMUTABLE for Android S and later versions
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Aktivsens")
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false);

        return builder.build();
    }

    public void updateNotification() {
        if (isForegroundService) {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    private int retrieveFinCount() {
        SharedPreferences sharedPrefs = getSharedPreferences(getFormattedDate(), Context.MODE_PRIVATE);
        return sharedPrefs.getInt(getFormattedDate(), 0);
    }

    public String getFormattedDate() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }


}
