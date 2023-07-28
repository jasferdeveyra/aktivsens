package com.healthcare.aktivsens.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.widget.Toast;

public class GoalAlarm {
    private Vibrator vibrator;
    private Context context;

    public GoalAlarm(Context context) {
        this.context = context;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void startAlarm(double currentValue, double targetValue) {
        if (currentValue > targetValue) {
            stopAlarm();
        }
    }

    public void stopAlarm() {
        // Stop the alarm if it is running
        vibrator.cancel();
    }
}
