package com.healthcare.aktivsens;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.net.Uri;

import java.security.AccessControlContext;


public class login extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private EditText usernameEditText, passwordEditText;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.login);

        showPrivacyPolicyDialog();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        TextView textView = findViewById(R.id.register);
        Button loginBtn = findViewById(R.id.loginBtn);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);

        // Get the saved username and password from shared preferences
        String savedUsername = sharedPref.getString("saved_username", "");
        String savedPassword = sharedPref.getString("saved_password", "");

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(login.this, register.class);
                startActivity(intent);
                finish();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    // Username or password is empty, display error message
                    Toast.makeText(login.this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                } else {
                    if (savedUsername.isEmpty() || savedPassword.isEmpty()) {
                        // No saved credentials, validate the entered credentials directly
                        mDatabase.child("users").child(username).child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String storedPassword = dataSnapshot.getValue(String.class);

                                if (storedPassword != null && storedPassword.equals(password)) {
                                    // Credentials match, save them to shared preferences
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("saved_username", username);
                                    editor.putString("saved_password", password);
                                    editor.apply();

                                    // Proceed to MainActivity
                                    Intent intent = new Intent(login.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Credentials don't match, display error message and clear password field
                                    Toast.makeText(login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                                    passwordEditText.setText("");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Handle database error, if any
                                Toast.makeText(login.this, "Error connecting to database", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Saved credentials exist, no need to validate again, proceed to MainActivity
                        Intent intent = new Intent(login.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });



        // Check if both saved username and password exist
        if (!savedUsername.isEmpty() && !savedPassword.isEmpty()) {
            Intent intent = new Intent(login.this, MainActivity.class);
            startActivity(intent);
            finish();
            //validateCredentialsFromFirebase(savedUsername, savedPassword);
        }
    }

    private void validateCredentialsFromFirebase(final String savedUsername, final String savedPassword) {
        mDatabase.child("users").child(savedUsername).child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String storedPassword = dataSnapshot.getValue(String.class);

                if (storedPassword != null && storedPassword.equals(savedPassword)) {
                    // Username and password match, proceed to MainActivity
                    Intent intent = new Intent(login.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Username or password doesn't match, display error message and clear fields
                    Toast.makeText(login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    passwordEditText.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database error, if any
                Toast.makeText(login.this, "Error connecting to database", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void validateCredentials(final String username, final String password) {
        mDatabase.child("users").child(username).child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String storedPassword = dataSnapshot.getValue(String.class);

                if (storedPassword != null && storedPassword.equals(password)) {
                    // Password matches, save username and password, and proceed to MainActivity
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("saved_username", username);
                    editor.putString("saved_password", password);
                    editor.apply();

                    Intent intent = new Intent(login.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Password doesn't match, display error message and clear fields
                    Toast.makeText(login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    usernameEditText.setText("");
                    passwordEditText.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database error, if any
                Toast.makeText(login.this, "Error connecting to database", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showPrivacyPolicyDialog() {
        Context context = this; // Use 'this' as the context directly from the activity
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean hasAcceptedPrivacyPolicy = sharedPreferences.getBoolean("PrivacyPolicyAccepted", false);

        if (!hasAcceptedPrivacyPolicy) {
            // Inflate the custom layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.custom_privacy_policy_dialog, null);
            TextView textViewPrivacyPolicyContent = dialogView.findViewById(R.id.textViewPrivacyPolicyContent);
            CheckBox checkBoxAccept = dialogView.findViewById(R.id.checkBoxAccept);

            // Set the content of the privacy policy with HTML formatting
            textViewPrivacyPolicyContent.setText(Html.fromHtml(getString(R.string.privacy_policy_content)));

            // Create the AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            builder.setPositiveButton("I ACCEPT", (dialog, which) -> {
                // Handle user acceptance if the checkbox is checked
                if (checkBoxAccept.isChecked()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("PrivacyPolicyAccepted", true);
                    editor.apply();
                } else {
                    // Show a message indicating that acceptance is required
                    Toast.makeText(context, "Please accept the Privacy Policy to continue.", Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }


}
