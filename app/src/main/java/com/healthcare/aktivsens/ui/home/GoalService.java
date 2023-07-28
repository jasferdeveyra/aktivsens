package com.healthcare.aktivsens.ui.home;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import com.healthcare.aktivsens.ui.home.GoalAlarm;

public class GoalService extends Service {
    private double currentValue;
    private double targetValue;
    private GoalAlarm goalAlarm;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (goalAlarm != null) {
                goalAlarm.startAlarm(currentValue, targetValue);
            }
            handler.postDelayed(this, 200); // Run every 200 milliseconds
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Retrieve the current and target values from the intent
        currentValue = intent.getDoubleExtra("currentValue", 0);
        targetValue = intent.getDoubleExtra("targetValue", 0);

        // Create an instance of the GoalAlarm class
        goalAlarm = new GoalAlarm(this);

        // Start the alarm every 200 milliseconds
        handler.post(runnable);

        // Return START_STICKY to let the system know to restart the service if it gets terminated
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the handler from posting new runnable callbacks
        handler.removeCallbacks(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
