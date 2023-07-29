# How Movement Detection Works in Aktivsens Android App?

The Aktivsens Android app utilizes the device's accelerometer to track physical activity and detect movements. This section provides an explanation of the code responsible for movement detection in the app.

The provided code snippet showcases the implementation of movement detection within the Aktivsens Android app. The app utilizes the device's accelerometer to track physical activity and count steps. Let's break down the code to understand how the movement detection process works:

## Code Explanation:

```java
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

```

The <b>onSensorChanged</b> method is called whenever there is a change in sensor data, and here we are specifically interested in the accelerometer data <b>(Sensor.TYPE_ACCELEROMETER)</b>.

The accelerometer data is represented by <b>x, y, and z</b> values, which denote the acceleration along the three axes.

The magnitude of the acceleration vector is calculated using the Euclidean distance formula <b>(Math.sqrt(x * x + y * y + z * z))</b>. This magnitude represents the overall acceleration experienced by the device.

A movement is detected if the magnitude of the acceleration vector (magnitude) is greater than a predefined threshold <b>(MOVEMENT_THRESHOLD)</b>, and a certain time interval <b>(MOVEMENT_TIME_THRESHOLD)</b> has elapsed since the last movement detection. This thresholding is essential to filter out minor fluctuations and prevent false detections.

When a movement is detected, several actions are performed:

* The <b>startGoalService()</b> method is called, presumably to initiate the tracking of movement goals or update related services.
* The step count <b>(finCount)</b> is incremented by one to reflect the detected step.
* The updated step count is formatted with commas and displayed on the <b>stepCountTextView</b> to provide real-time feedback to the user.
* The updated step count (finCount) is saved in the app's data storage using the <b>saveFinCount(finCount)</b> method.
* The distance traveled, calories burned, and movement time are calculated based on the updated step count and displayed on corresponding TextView elements <b>(distanceText, caloriesText, movementText)</b>.
* The time of the last movement detection <b>(lastMovementTime)</b> is updated to the current time to prevent immediate successive detections.
  
Additionally, the code checks if a SensorForegroundService is bound and notifies it about the updated step count (finCount) through the <b>sensorService.notifyMovementDetected(finCount)</b> method. This mechanism likely allows the foreground service to update the app's user interface or perform further actions based on the new step count.


```java
// Retrieve the saved finCount value and sets them agad on create palang
finCount = retrieveFinCount();
if (finCount < 5) {
    connectToFirebaseForTodayCount();
}
docCheck(finCount);
```

In this code snippet, the app retrieves the previously saved step count value (finCount) from its data storage. The step count represents the total number of steps recorded by the device's accelerometer.

The conditional statement <b>if (finCount < 5)</b> checks if the retrieved step count is less than 5. This condition serves as an indicator to determine whether the app needs to fetch today's step count from the <b>Firebase database</b>. A value of 5 is used as a threshold to trigger an update from Firebase. If the step count is below 5, it suggests that the user hasn't recorded significant movements for the current day, and fetching data from Firebase can provide more accurate and up-to-date information.

```java

int count = Integer.parseInt(String.valueOf(finCount));
String formattedCount = String.format(Locale.getDefault(), "%,d", count);
stepCountTextView.setText(formattedCount);
```

In the above code, the retrieved step count <b>(finCount)</b> is converted to an integer, and then it is formatted with commas using <b>String.format()</b>. This formatting improves the readability of large step counts by separating thousands with commas.

The formatted step count is then displayed on the stepCountTextView, one of the TextView elements in the app's user interface, which is responsible for showing the current step count to the user.

## Summary:
The movement detection process is a crucial aspect of the Aktivsens Android app, as it enables tracking of physical activity. By utilizing the device's accelerometer, the app captures movement data, calculates step counts, and presents real-time information to users. This functionality encourages users to maintain an active lifestyle and helps them monitor their progress effectively.

The code explained above forms the foundation of movement detection in the Aktivsens app, enhancing its usability for users seeking to monitor and improve their physical activity levels.
