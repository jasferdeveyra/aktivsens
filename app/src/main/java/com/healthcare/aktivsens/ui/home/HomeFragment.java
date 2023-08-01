package com.healthcare.aktivsens.ui.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.healthcare.aktivsens.ui.home.GoalAlarm;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.healthcare.aktivsens.ui.home.GoalService;
import com.healthcare.aktivsens.MainActivity;
import com.healthcare.aktivsens.R;
import com.healthcare.aktivsens.SensorForegroundService;
import com.healthcare.aktivsens.walk;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.healthcare.aktivsens.yourhistory;


public class HomeFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int finCount = 0;

    //declare elements here
    private TextView stepCountTextView;
    public TextView dateText;
    public TextView distanceText;
    public TextView caloriesText;
    public TextView movementText;

    public  boolean walkTrue = false;
    public  int walkCount = 0;
    public boolean runTrue = false;
    public int runCount = 0;

    private static final float MOVEMENT_THRESHOLD = 12.5f; // Adjust this
    private static final long MOVEMENT_TIME_THRESHOLD = 500; // Adjust this (in milliseconds)

    private long lastMovementTime = 0;

    private SensorForegroundService sensorService;
    private boolean isServiceBound = false;
    private boolean isPaused = false;
    private boolean isFragmentVisible = false;
    private Handler handler;
    private Runnable updateWalkTrueRunnable;
    private Runnable updateRunTrueRunnable;
    public int visibility;
    public View myView;


    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String DIALOG_SHOWN_KEY = "dialog_shown";


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SensorForegroundService.LocalBinder binder = (SensorForegroundService.LocalBinder) iBinder;
            sensorService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            sensorService = null;
            isServiceBound = false;
        }
    };
    private Handler handlerq;
    private Runnable runnableq;

    private SharedPreferences sharedPrefsq;

    public TextView chater;
    public TextView goaler;
    public GoalAlarm goalAlarm;
    public double currentValue;
    public double targetValue;

    private Handler handlerG;
    private Runnable runnableG;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Hide the title bar
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        //SHOWS THE BATTERY SAVER NOTE
        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean dialogShown = settings.getBoolean(DIALOG_SHOWN_KEY, false);

        if (!dialogShown) {
            // If the dialog has not been shown, show it and set the flag to true
            showAlertDialog();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(DIALOG_SHOWN_KEY, true);
            editor.apply();
        }



        if (activity != null) {

            Window window = activity.getWindow();
            //getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getSupportActionBar().hide();
        }


        //LOAD THE GIFS
        ImageView gifImageView = root.findViewById(R.id.topimage);
        ImageView docSpeak = root.findViewById(R.id.docspeak);

        int desiredWidth = 100; // Set your desired width in pixels
        int desiredHeight = 100; // Set your desired height in pixels

        Glide.with(this)
                .asGif()
                .override(desiredWidth, desiredHeight)
                .load(R.drawable.rungif)
                .into(gifImageView);

        Glide.with(this)
                .asGif()
                .override(desiredWidth, desiredHeight)
                .load(R.drawable.docspeak)
                .into(docSpeak);

        myView = root.findViewById(R.id.movetoday);
        chater = root.findViewById(R.id.chat);
        goaler = root.findViewById(R.id.goalcount);


        visibility = myView.getVisibility();

        if (visibility == View.VISIBLE) {
            //setzero();
        } else {
            //setzero();
        }

        //sets walktrue to false
        if (getActivity() != null) {
            walkTrue = getActivity().getIntent().getBooleanExtra("walkTrue", false);
        }
        //sets walktrue to false
        if (getActivity() != null) {
            runTrue = getActivity().getIntent().getBooleanExtra("walkTrue", false);
        }

        //get elements here
        stepCountTextView = root.findViewById(R.id.stepscount);
        dateText = root.findViewById(R.id.dateNow);
        distanceText = root.findViewById(R.id.distancecount);
        caloriesText = root.findViewById(R.id.burncount);
        movementText = root.findViewById(R.id.timecount);

        stepCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), yourhistory.class);
                startActivity(intent);
            }
        });

        TextView yesCount = root.findViewById(R.id.yescount);

        //set date
        String dateToday = getFormattedDate();
        dateText.setText(dateToday);
        dateText.setAllCaps(true);
        saveGoal();
        yesCount.setText("YESTERDAY: "+String.valueOf(retrieveYesterdayFinCount()));


        // Retrieve the saved finCount value and sets them agad on create palang
        finCount = retrieveFinCount();
        if (finCount < 5) {
            connectToFirebaseForTodayCount();
        }
        docCheck(finCount);
        int count = Integer.parseInt(String.valueOf(finCount));
        String formattedCount = String.format(Locale.getDefault(), "%,d", count);
        stepCountTextView.setText(formattedCount);

        float distance = finCount / 1369f;
        String formattedDistance = String.format("%.1f", distance);
        distanceText.setText(formattedDistance);
        float stepsPerCalorie = 1f / 0.04f;
        float caloriesBurned = finCount / stepsPerCalorie;
        String formattedCaloriesBurned = String.format(Locale.getDefault(), "%.1f", caloriesBurned);
        caloriesText.setText(formattedCaloriesBurned);
        int minutes = finCount / (2 * 60); // Assuming there are 2 steps per second
        movementText.setText(String.valueOf(minutes));


        //check if goal is achieved
        goalAlarm = new GoalAlarm((Activity) requireContext());
        currentValue = (double) retrieveFinCount();
        targetValue = (double) retrieveGoal();


        // Start the gaolservice
        startGoalService();

        //WALK BUTTON
        ImageView walk = root.findViewById(R.id.walk);
        walk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // walkTrue = true;
               // walkCount = 0;
                //runCount = 0;

                //SharedPreferences sharedPrefswrsa = requireContext().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
                //if (sharedPrefswrsa.contains("WALK_" + getFormattedDate())) {
                 //   // The value already exists, so remove it
                  //  SharedPreferences.Editor editorwrsa = sharedPrefswrsa.edit();
                 //   editorwrsa.remove("WALK_" + getFormattedDate());
                  //  editorwrsa.apply();
                // }

                //myView.setVisibility(View.INVISIBLE);

               // Intent intent = new Intent(getActivity(), walk.class);
                // startActivity(intent);

                Toast.makeText(requireContext(), "Walk Counter Coming Soon!", Toast.LENGTH_SHORT).show();

            }
        });

        //RUN BUTTON
        ImageView run = root.findViewById(R.id.run);
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), "Run Counter Coming Soon!", Toast.LENGTH_SHORT).show();
            }
        });




        sharedPrefsq = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Create a handler and runnable for the periodic check
        handlerq = new Handler(Looper.getMainLooper());
        runnableq = new Runnable() {
            @Override
            public void run() {
                checkLastSavedDate();
                handlerq.postDelayed(this, 1000); // Run every 1 second
            }
        };

        return root;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startSensorListener();
        bindSensorService();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSensorListener();
        unbindSensorService();
        // Upload count to database community
        SharedPreferences sharedPrefUname = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedUsernameUanme = sharedPrefUname.getString("saved_username", "");
        uploadDataToFirebase(savedUsernameUanme);
    }

    private void startSensorListener() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
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

    private void bindSensorService() {
        Intent serviceIntent = new Intent(requireContext(), SensorForegroundService.class);
        requireActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindSensorService() {
        if (isServiceBound) {
            requireActivity().unbindService(serviceConnection);
            isServiceBound = false;
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

                // Start the gaolservice
                startGoalService();


                finCount++;
                int count = Integer.parseInt(String.valueOf(finCount));
                String formattedCount = String.format(Locale.getDefault(), "%,d", count);
                stepCountTextView.setText(formattedCount);
                // Save the updated finCount value
                saveFinCount(finCount);

                // Dito nalang other codes and logics
                float distance = finCount / 1369f;
                String formattedDistance = String.format("%.1f", distance);
                distanceText.setText(formattedDistance);

                float stepsPerCalorie = 1f / 0.04f;
                float caloriesBurned = finCount / stepsPerCalorie;
                String formattedCaloriesBurned = String.format(Locale.getDefault(), "%.1f", caloriesBurned);
                caloriesText.setText(formattedCaloriesBurned);

                int stepsPerSecond = 2; // Assuming there are 2 steps per second
                int stepsPerMinute = stepsPerSecond * 60; // Calculate the steps per minute
                int minutes = finCount / stepsPerMinute; // Calculate the number of minutes
                movementText.setText(String.valueOf(minutes));


                lastMovementTime = currentTime;

                // Notify SensorForegroundService about the step count update
                if (isServiceBound && sensorService != null) {
                    sensorService.notifyMovementDetected(finCount);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public void updateStepCount(int stepCount) {
        if (stepCountTextView != null) {
            stepCountTextView.setText(String.valueOf(stepCount));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        isFragmentVisible = false;

        SharedPreferences sharedPrefswrs = requireContext().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwrs = sharedPrefswrs.edit();
        editorwrs.remove("WALK_" + getFormattedDate());
        editorwrs.apply();
        // Start the gaolservice
        startGoalService();
       //stopGoalServiceContinuously();
        stopUpdatingFlags();
        updateNotification();
        handler.removeCallbacks(runnableq);

        // Upload count to database community
        SharedPreferences sharedPrefUname = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedUsernameUanme = sharedPrefUname.getString("saved_username", "");
        uploadDataToFirebase(savedUsernameUanme);
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        isFragmentVisible = true;
        // Start the gaolservice
        startGoalService();
        //startGoalServiceContinuously();
        SharedPreferences sharedPrefswrs = requireContext().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwrs = sharedPrefswrs.edit();
        editorwrs.remove("WALK_" + getFormattedDate());
        editorwrs.apply();

        startUpdatingFlags();
        updateNotification();
        handler.postDelayed(runnableq, 1000);

        // Upload count to database community
        SharedPreferences sharedPrefUname = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedUsernameUanme = sharedPrefUname.getString("saved_username", "");
        uploadDataToFirebase(savedUsernameUanme);

    }

    private void checkLastSavedDate() {
        String currentDate = getFormattedDate();

        // Retrieve the last saved date from SharedPreferences
        String lastSavedDate = sharedPrefsq.getString("last_saved_date", "");

        if (lastSavedDate.equals(currentDate)) {
            // The last saved date is the same as the current date
            // Perform any desired actions here
        } else {
            clearFinCount();

            SharedPreferences.Editor editorq = sharedPrefsq.edit();
            editorq.putString("last_saved_date", currentDate);
            editorq.apply();
        }
    }
    private void updateNotification() {
        if (isServiceBound && sensorService != null) {
            sensorService.updateNotification();
        }
    }

    public String getFormattedDate() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }

    public String getYesterdayFormattedDate() {
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1); // Subtract one day

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

        // Format the date as "JUN 23 2023"
        return dateFormat.format(calendar.getTime());
    }

    private boolean canUploadData = true;

    private void saveFinCount(int count) {
        if (!canUploadData) {
            return; // Ignore the call if 10 seconds have not passed yet
        }

        SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        String lastSavedDate = sharedPrefs.getString("last_saved_date", "");
        String currentDate = getFormattedDate();

        SharedPreferences sharedPrefsUsername = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedUsername = sharedPrefsUsername.getString("saved_username", "");

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference dataRef = databaseRef.child("DATA_" + savedUsername).child(currentDate);

        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataRef.setValue(count);
                } else {
                    databaseRef.child("DATA_" + savedUsername).child(getFormattedDate()).setValue(count);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle onCancelled if needed
            }
        });

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(currentDate, count);
        editor.putString("last_saved_date", currentDate);
        editor.apply();

        SharedPreferences sharedPrefswr = requireActivity().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwr = sharedPrefswr.edit();

        int visibility2 = myView.getVisibility();
        if (visibility2 == View.VISIBLE) {
            walkCount = 0;
            runCount = 0;
        } else {
            if (walkTrue) {
                editorwr.putInt("WALK_" + getFormattedDate(), walkCount);
                walkCount = walkCount + 1;
                editorwr.apply();
            } else {
                walkCount = 0;
                walkTrue = false;
            }

            if (runTrue) {
                editorwr.putInt("RUN_" + getFormattedDate(), runCount);
                runCount = runCount + 1;
                editorwr.apply();
            } else {
                runCount = 0;
                runTrue = false;
            }
        }

        // Disable data upload for 10 seconds
        canUploadData = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                canUploadData = true;
            }
        }, 60000); // 60 seconds delay
    }




    private int retrieveFinCount() {
        SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        return sharedPrefs.getInt(getFormattedDate(), 0);
    }

    private int retrieveYesterdayFinCount() {
        SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        return sharedPrefs.getInt(getYesterdayFormattedDate(), 0);
    }
    public void clearFinCount() {
        // Reset the finCount value to 0
        finCount = 0;
        stepCountTextView.setText(String.valueOf(finCount));

        // Clear the saved finCount value
        deleteFinCount();

        // Update other UI elements if necessary
        distanceText.setText("0");
        caloriesText.setText("0");
        movementText.setText("0");
    }

    private void deleteFinCount() {
        //SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        //SharedPreferences.Editor editor = sharedPrefs.edit();
        //editor.remove(getFormattedDate());
        //editor.apply();
    }


    //STOPS THE WALK
    public void setWalkTrueFalse() {
        walkTrue = false;
    }


    private void startUpdatingFlags() {
        handler = new Handler();

        updateWalkTrueRunnable = new Runnable() {
            @Override
            public void run() {
                walkTrue = false;
                if (isFragmentVisible) {
                    handler.postDelayed(this, 200); // Update walkTrue every 200 milliseconds
                }
            }
        };

        updateRunTrueRunnable = new Runnable() {
            @Override
            public void run() {
                runTrue = false;
                if (isFragmentVisible) {
                    handler.postDelayed(this, 200); // Update runTrue every 200 milliseconds
                }
            }
        };

        handler.post(updateWalkTrueRunnable);
        handler.post(updateRunTrueRunnable);
    }

    private void stopUpdatingFlags() {
        if (handler != null) {
            handler.removeCallbacks(updateWalkTrueRunnable);
            handler.removeCallbacks(updateRunTrueRunnable);
        }
    }


//Doc suggestions


        public void docCheck(int c) {
            int userStepCount = c; // Get the user's step count from your app or device

            // Calculate the percentage of 10,000 steps
            double percentage = (userStepCount / (float) retrieveGoal()) * 100;

            // Determine the health level description based on the percentage
            String healthLevel;
            if (percentage >= 140) {
                healthLevel = "Very Active";
            } else if (percentage >= 90) {
                healthLevel = "Active";
            } else if (percentage >= 40) {
                healthLevel = "Moderately Active";
            } else {
                healthLevel = "Not Quite Active";
            }

            // Define suggestions for each health level
            String[] suggestions;
            if (healthLevel.equals("Very Active")) {
                suggestions = new String[]{
                        "Challenge yourself with new activities or exercises.",
                        "Try a new sport or outdoor adventure for variety.",
                        "Maintain your high activity level to stay fit.",
                        "Discover new ways to keep yourself engaged.",
                        "Explore different fitness classes to stay motivated.",
                        "Take up a new hobby that involves physical activity.",
                        "Consider trying yoga or Pilates for flexibility.",
                        "Incorporate strength training to enhance your fitness.",
                        "Stay active and make movement a part of your lifestyle.",
                        "Engage in interval training for cardiovascular benefits.",
                        "Explore local hiking trails for a scenic workout.",
                        "Join a sports league to meet like-minded individuals.",
                        "Participate in charity walks or runs for a good cause.",
                        "Consider joining a dance class to stay active.",
                        "Take up swimming as a full-body workout.",
                        "Try rock climbing for a thrilling challenge.",
                        "Engage in team sports for competitive fun.",
                        "Experiment with CrossFit workouts for intensity.",
                        "Take martial arts classes for discipline and self-defense.",
                        "Join a boot camp or group fitness program.",
                        "Try outdoor activities like kayaking or paddleboarding.",
                        "Practice high-intensity circuit training.",
                        "Take up boxing or kickboxing for a dynamic workout.",
                        "Try obstacle course races for a test of strength and endurance.",
                        "Participate in group fitness challenges or events.",
                        "Incorporate plyometric exercises for explosive power.",
                        "Engage in regular mountain biking for adventure and fitness.",
                        "Try extreme sports like snowboarding or surfing.",
                        "Join a running club for camaraderie and motivation.",
                        // Add more suggestions...
                };
            } else if (healthLevel.equals("Active")) {
                suggestions = new String[]{
                        "Maintain your active lifestyle for long-term health.",
                        "Engage in regular aerobic exercises for cardiovascular fitness.",
                        "Incorporate strength training to build lean muscle.",
                        "Try high-intensity interval training for efficient workouts.",
                        "Join a gym or fitness center for access to various equipment.",
                        "Take up cycling for a low-impact cardiovascular exercise.",
                        "Consider joining a recreational sports team.",
                        "Explore local parks for outdoor fitness activities.",
                        "Incorporate yoga or stretching for flexibility.",
                        "Try kickboxing or martial arts for a fun workout.",
                        "Attend group fitness classes for motivation.",
                        "Participate in virtual fitness challenges.",
                        "Incorporate resistance bands for at-home workouts.",
                        "Try online workout programs for variety.",
                        "Make physical activity a priority in your daily routine.",
                        "Join a hiking group for outdoor exploration.",
                        "Incorporate swimming for a full-body workout.",
                        "Try rowing or paddleboarding for a challenging upper body workout.",
                        "Join a recreational sports league for friendly competition.",
                        "Experiment with different types of dance classes.",
                        "Incorporate HIIT (High-Intensity Interval Training) workouts.",
                        "Try trampoline workouts for a fun and low-impact option.",
                        "Join a local cycling group for group rides.",
                        "Incorporate circuit training for a full-body workout.",
                        "Try outdoor activities like hiking or trail running.",
                        "Join a running group for accountability and support.",
                        // Add more suggestions...
                };
            } else if (healthLevel.equals("Moderately Active")) {
                suggestions = new String[]{
                        "Gradually increase your activity level for better health.",
                        "Take regular walks to improve your step count.",
                        "Try biking as an enjoyable form of exercise.",
                        "Participate in group hikes for social interaction.",
                        "Explore local parks or trails for outdoor activities.",
                        "Take up gardening or yard work for physical activity.",
                        "Consider joining a recreational sports club.",
                        "Try bodyweight exercises for strength and toning.",
                        "Take breaks from sitting and incorporate stretching.",
                        "Incorporate dance or Zumba for an enjoyable workout.",
                        "Try online workout videos for guidance.",
                        "Engage in household chores for movement.",
                        "Take stairs instead of elevators for extra steps.",
                        "Use a pedometer to track your daily step count.",
                        "Join a walking or running group for accountability.",
                        "Incorporate yoga or Pilates for flexibility and core strength.",
                        "Try golfing as a leisurely form of exercise.",
                        "Participate in local community fitness events.",
                        "Join a local fitness class or group training.",
                        "Incorporate light weightlifting or resistance training.",
                        "Explore outdoor activities like kayaking or canoeing.",
                        "Try low-impact aerobics or water aerobics.",
                        "Incorporate balance exercises like tai chi or yoga.",
                        "Take up recreational swimming for overall fitness.",
                        "Try circuit training using bodyweight exercises.",
                        // Add more suggestions...
                };
            } else {
                suggestions = new String[]{
                        "Start by taking short walks to build endurance.",
                        "Try gentle stretching exercises to improve flexibility.",
                        "Incorporate small bursts of activity throughout the day.",
                        "Use household items as weights for resistance training.",
                        "Consider trying chair exercises for mobility.",
                        "Take breaks from sitting and move around regularly.",
                        "Try beginner-friendly workout routines or videos.",
                        "Join a local walking group for motivation.",
                        "Gradually increase your step count each day.",
                        "Incorporate walking meetings or phone calls.",
                        "Find a workout buddy for accountability and support.",
                        "Set small goals and celebrate your achievements.",
                        "Try water aerobics for a low-impact workout.",
                        "Take up gardening or light yard work for movement.",
                        "Listen to music or podcasts during walks for enjoyment.",
                        "Practice seated yoga or chair-based exercises.",
                        "Experiment with resistance bands for strength training.",
                        "Engage in gentle swimming or water exercises.",
                        "Try tai chi or gentle martial arts for balance and coordination.",
                        "Participate in local senior fitness classes or programs.",
                        "Join a community walking program or group.",
                        "Incorporate seated or standing stretching routines.",
                        "Take up gentle forms of dance like ballroom or line dancing.",
                        "Experiment with yoga or Pilates for flexibility and relaxation.",
                        // Add more suggestions...
                };
            }



            // Select a random suggestion from the array
            Random random = new Random();
            String randomSuggestion = suggestions[random.nextInt(suggestions.length)];

// Display the information and suggestion to the user with typing effect
            final String message = "I suggest, " + randomSuggestion + " Your movement percentage was " +
                    String.format("%.2f", percentage) + "% which is " + healthLevel + ". ";

            chater.setText(""); // Clear the TextView before typing

            final int delay = 20; // Delay between each character typing
            final int finalLength = message.length();
            final int[] index = {0};

            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (index[0] < finalLength) {
                        char currentChar = message.charAt(index[0]);
                        chater.append(String.valueOf(currentChar));
                        index[0]++;
                        if (!Character.isWhitespace(currentChar) && !Character.isLetterOrDigit(currentChar)) {
                            // Pause longer for punctuation marks
                            handler.postDelayed(this, delay * 4);
                        } else {
                            handler.postDelayed(this, delay);
                        }
                    }
                }
            };

            handler.postDelayed(runnable, delay);


        }

    private int retrieveGoal() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("goal", 10000); // Default value is set to 10,000 steps
    }
    private void saveGoal() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!sharedPreferences.contains("goal")) {
            editor.putInt("goal", 10000);
        }
        editor.apply();
        goaler.setText("GOAL COUNT: "+String.valueOf(retrieveGoal()));
    }


    private void startGoalServiceContinuously() {
        handlerG = new Handler();
        runnableG = new Runnable() {
            @Override
            public void run() {
                startGoalService();
                handlerG.postDelayed(this, 2000); // Run every 2 seconds
            }
        };
        handlerG.postDelayed(runnableG, 2000);
    }

    private void stopGoalServiceContinuously() {
        if (handlerG != null && runnableG != null) {
            handlerG.removeCallbacks(runnableG);
            handlerG = null;
            runnableG = null;
        }
    }
    public void startGoalService() {
        if (currentValue > targetValue) {
            // Vibrate the phone
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated method for older API levels
                    vibrator.vibrate(1000);
                }
            }

            // Create an alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Goal Exceeded");
            builder.setMessage("Your CURRENT MOVEMENTS has exceeded the GOAL value. Click PROCEED to continue");
            builder.setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Stop the vibration
                    if (vibrator != null) {
                        vibrator.cancel();
                    }

                    // Increase the goal count by 100
                    int newGoal = (int) (targetValue + 100);

                    // Save the new goal count in SharedPreferences
                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("goal", newGoal);
                    editor.apply();

                    // Start the GoalService with updated values
                    Intent intent = new Intent(requireContext(), GoalService.class);
                    intent.putExtra("currentValue", currentValue);
                    intent.putExtra("targetValue", targetValue);
                    requireContext().startService(intent);
                }
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            Intent intent = new Intent(requireContext(), GoalService.class);
            intent.putExtra("currentValue", currentValue);
            intent.putExtra("targetValue", targetValue);
            requireContext().startService(intent);
        }
    }


    private SharedPreferences sharedPreferences;
    private void connectToFirebaseForTodayCount() {
        // TODO: Implement Firebase connection to get today's count
        // You can use the getFormattedDate() method to retrieve the date in the required format
        // and then fetch the count for that date from Firebase
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("saved_username", "");

        // Assuming you have a Firebase database reference named "countRef"
        DatabaseReference countRef = FirebaseDatabase.getInstance().getReference("DATA_"+username);

        String formattedDate = getFormattedDate(); // Retrieve the formatted date

        // Assume the count for today is stored under the child node with today's date
        countRef.child(formattedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int todayCount = dataSnapshot.getValue(Integer.class);
                    // Update the finCount value with today's count
                    finCount = todayCount;
                    // Update the UI with the new finCount value
                    stepCountTextView.setText(String.valueOf(finCount));
                    float distance = finCount / 1369f;
                    String formattedDistance = String.format("%.1f", distance);
                    distanceText.setText(formattedDistance);
                    float stepsPerCalorie = 1f / 0.04f;
                    float caloriesBurned = finCount / stepsPerCalorie;
                    String formattedCaloriesBurned = String.format(Locale.getDefault(), "%.1f", caloriesBurned);
                    caloriesText.setText(formattedCaloriesBurned);
                    int minutes = finCount / (2 * 60); // Assuming there are 2 steps per second
                    movementText.setText(String.valueOf(minutes));

                    // Save the updated finCount value in SharedPreferences
                    saveFinCount(finCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors during database retrieval
            }
        });
    }




    public String getFormattedDat2() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }

    private void uploadDataToFirebase(String username) {
        int finCount = retrieveFinCount();

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("community");
        DatabaseReference userRef = databaseRef.child(username);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //int finCount =  retrieveFinCount();
                    // Update data if it exists and the new value is greater
                    int existingCount = snapshot.child("movcount").getValue(Integer.class);
                    if (finCount > existingCount) {
                        userRef.child("movcount").setValue(finCount);
                        userRef.child("date").setValue(getFormattedDate());
                    }
                } else {
                    // Create new data if it doesn't exist
                    userRef.child("movcount").setValue(finCount);
                    userRef.child("date").setValue(getFormattedDate());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                Toast.makeText(requireContext(), "Failed to retrieve data from Firebase", Toast.LENGTH_SHORT).show();
            }
        });

    }



    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(R.layout.alert_dialog);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
