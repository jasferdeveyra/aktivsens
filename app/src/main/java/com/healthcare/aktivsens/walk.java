package com.healthcare.aktivsens;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.healthcare.aktivsens.ui.home.HomeFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class walk extends AppCompatActivity {

    private int stepCount = 0;
    private TextView stepCountTextView;
    private TextView lastStepCountTextView;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private Runnable updateStepCountRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Toast.makeText(walk.this, "This is an experimental module. If count does not reset, please restart the app", Toast.LENGTH_LONG).show();

        setContentView(R.layout.walk);

        checkAndDeleteWalkData();

        // Set date
        TextView dateText = findViewById(R.id.dateNow);
        String dateToday = getFormattedDate();
        dateText.setText(dateToday);
        dateText.setAllCaps(true);

        stepCountTextView = findViewById(R.id.stepscount);
        lastStepCountTextView = findViewById(R.id.lastwalk);

        sharedPreferences = getSharedPreferences("WALKRUN", MODE_PRIVATE);
        handler = new Handler();
        updateStepCountRunnable = new Runnable() {
            @Override
            public void run() {
                updateStepCount();
                handler.postDelayed(this, 600);
            }
        };

        handler.postDelayed(updateStepCountRunnable, 600);

        SharedPreferences sharedPreferencesx = getSharedPreferences("LASTWALKRUN", MODE_PRIVATE);
        String currentDate = getFormattedDate();
        int lastWalkCountOnce = sharedPreferencesx.getInt("LASTWALK_" + currentDate, 0);

        String lastWalkCountText = "LAST WALK: " + lastWalkCountOnce + " STEPS";
        lastStepCountTextView.setText(lastWalkCountText);

        Button stopWalk = findViewById(R.id.stopBtn);
        stopWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPrefswr2 = getSharedPreferences("LASTWALKRUN", MODE_PRIVATE);
                SharedPreferences.Editor editorw2r = sharedPrefswr2.edit();
                String stepCountText = stepCountTextView.getText().toString();
                int stepCounter = Integer.parseInt(stepCountText);

                editorw2r.putInt("LASTWALK_" + getFormattedDate(), stepCounter);
                editorw2r.apply();

                setZero();

                Intent intent = new Intent(walk.this, MainActivity.class);
                intent.putExtra("walkTrue", false);
                startActivity(intent);
            }
        });

        Button resetWalk = findViewById(R.id.resetBtn);
        resetWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPrefswr2 = getSharedPreferences("LASTWALKRUN", MODE_PRIVATE);
                SharedPreferences.Editor editorw2r = sharedPrefswr2.edit();
                String stepCountText = stepCountTextView.getText().toString();
                int stepCounter = Integer.parseInt(stepCountText);

                editorw2r.putInt("LASTWALK_" + getFormattedDate(), stepCounter);
                editorw2r.apply();

                setZero();

                Intent intent = new Intent(walk.this, MainActivity.class);
                intent.putExtra("walkTrue", false);
                startActivity(intent);
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setZero();
        handler.removeCallbacks(updateStepCountRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        setZero();
        stepCountTextView.setText("0");
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateStepCountRunnable);
        setZero();
    }

    public void setZero() {
        SharedPreferences sharedPrefswrs = getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwrs = sharedPrefswrs.edit();
        editorwrs.remove("WALK_" + getFormattedDate());
        editorwrs.apply();
    }

    private void checkAndDeleteWalkData() {
        SharedPreferences sharedPrefswrsa = getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        if (sharedPrefswrsa.contains("WALK_" + getFormattedDate())) {
            // The value already exists, so remove it
            SharedPreferences.Editor editorwrsa = sharedPrefswrsa.edit();
            editorwrsa.remove("WALK_" + getFormattedDate());
            editorwrsa.apply();
        }
    }

    private String getFormattedDate() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }

    private void updateStepCount() {
        int storedStepCount = sharedPreferences.getInt("WALK_" + getFormattedDate(), 0);
        stepCount = storedStepCount;
        stepCountTextView.setText(String.valueOf(stepCount));
    }
}
