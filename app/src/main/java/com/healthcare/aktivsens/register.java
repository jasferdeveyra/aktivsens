package com.healthcare.aktivsens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class register extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private EditText usernameEditText, passwordEditText, password2EditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.register);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        TextView textView = findViewById(R.id.login);
        Button registerBtn = findViewById(R.id.registerBtn);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        password2EditText = findViewById(R.id.password2);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(register.this, login.class);
                startActivity(intent);
                finish();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String password2 = password2EditText.getText().toString();

                if (password.equals(password2)) {
                    saveToFirebase(username, password);
                } else {
                    Toast.makeText(register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    passwordEditText.setText("");
                    password2EditText.setText("");
                }
            }
        });
    }

    private void saveToFirebase(String username, String password) {
        if (password.length() < 8 || !password.matches(".*\\d.*")) {
            Toast.makeText(register.this, "Password should be at least 8 characters long and contain a number", Toast.LENGTH_SHORT).show();
            passwordEditText.setText("");
            password2EditText.setText("");
            return;
        }

        // Check if the username already exists
        mDatabase.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Username already exists
                    Toast.makeText(register.this, "Username already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // Username does not exist, proceed with registration
                    mDatabase.child("users").child(username).child("password").setValue(password);
                    Toast.makeText(register.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    usernameEditText.setText("");
                    passwordEditText.setText("");
                    password2EditText.setText("");
                    Intent intent = new Intent(register.this, login.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(register.this, "Failed to check username availability", Toast.LENGTH_SHORT).show();
            }
        });
    }


}