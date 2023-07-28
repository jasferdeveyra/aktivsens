package com.healthcare.aktivsens;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.ArrayList;
import java.util.List;

public class StepCounterManager implements SensorEventListener {

    private static final float MOVEMENT_THRESHOLD = 12.5f; // Adjust this
    private static final long MOVEMENT_TIME_THRESHOLD = 500; // Adjust this (in milliseconds)

    static StepCounterManager instance;

    private int stepCount = 0;
    private long lastMovementTime = 0;
    private List<StepCountListener> listeners;

    private StepCounterManager() {
        listeners = new ArrayList<>();
    }

    public static synchronized StepCounterManager getInstance() {
        if (instance == null) {
            instance = new StepCounterManager();
        }
        return instance;
    }

    public void registerListener(StepCountListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(StepCountListener listener) {
        listeners.remove(listener);
    }

    public void notifyStepCountUpdated() {
        stepCount++;
        for (StepCountListener listener : listeners) {
            listener.onStepCountUpdated(stepCount);
        }
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
                lastMovementTime = currentTime;
                notifyStepCountUpdated();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
}
