package com.healthcare.aktivsens.ui.options;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.healthcare.aktivsens.R;
import com.healthcare.aktivsens.databinding.FragmentOptionsBinding;
import com.healthcare.aktivsens.login;
import com.healthcare.aktivsens.ui.home.HomeFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OptionsFragment extends Fragment {

    private FragmentOptionsBinding binding;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    public DatabaseReference communityRef;
    public String valueToDelete;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOptionsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("saved_username", "");

        // Set the username to the username TextView
        binding.username.setText(username);

        if (!username.isEmpty()) {
            // Get the first character of the username
            String firstChar = username.substring(0, 1).toUpperCase();

            // Set the first character to the circleTextView
            binding.circleTextView.setText(firstChar);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        communityRef = FirebaseDatabase.getInstance().getReference("community");

        // CHANGE PASSWORD
        binding.changepass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        // ENABLE CUSTOM GOAL
        binding.customgoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Set Goal")
                        .setMessage("Do you want to set a Custom Goal or use the Default Goal?")
                        .setPositiveButton("Custom Goal", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showCustomGoalPrompt();
                            }
                        })
                        .setNegativeButton("Default Goal", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setDefaultGoal();
                            }
                        })
                        .show();
            }
        });




        // RESET TODAY
        binding.resettoday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Confirmation")
                        .setMessage("Are you sure you want to clear today's count?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearTodayCount();
                                Toast.makeText(requireContext(), "Today's count has been reset", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        // LOG OUT
        binding.logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

        // DELETE ACCOUNT
        binding.deleteAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountConfirmationDialog();
            }
        });

        return root;
    }

    private void clearTodayCount() {
        SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        sharedPrefs.edit().putInt(getFormattedDate(), 0).apply();

        SharedPreferences sharedPrefswr = requireActivity().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwr = sharedPrefswr.edit();
        editorwr.clear().apply();
        // Delete the value in Firebase
        String savedusername = sharedPreferences.getString("saved_username", "");
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference().child("DATA_"+savedusername).child(getFormattedDate());
        firebaseRef.removeValue();
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to log out? All app data will be cleared.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllData();
                        navigateToLogin();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }



    private void showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Account")
                .setMessage("Enter your password to delete your account:");

        final EditText passwordEditText = new EditText(requireContext());
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordEditText);

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = passwordEditText.getText().toString().trim();

                if (isPasswordValid(password)) {
                    confirmDeleteAccount();
                } else {
                    Toast.makeText(requireContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private boolean isPasswordValid(String password) {
        String savedPassword = sharedPreferences.getString("saved_password", "");
        return password.equals(savedPassword);
    }

    private void confirmDeleteAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccount();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void deleteAccount() {
        String savedUsername = sharedPreferences.getString("saved_username", "");
        valueToDelete = savedUsername;
        Query query = usersRef.orderByKey().equalTo(savedUsername);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        userSnapshot.getRef().removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    currentUser.delete().addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {

                                            deleteDataNodeForUser(valueToDelete);
                                            deleteCommunityValue(valueToDelete);


                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.clear().apply();

                                            SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
                                            sharedPrefs.edit().putInt(getFormattedDate(), 0).apply();

                                            SharedPreferences sharedPrefswr = requireActivity().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editorwr = sharedPrefswr.edit();
                                            editorwr.clear().apply();


                                            Intent intent = new Intent(requireContext(), login.class);
                                            startActivity(intent);
                                            requireActivity().finish();


                                            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                                        } else {
                                            clearAllData();
                                            navigateToLogin();
                                            Toast.makeText(requireContext(), "Failed to delete account (0x2)", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    clearAllData();
                                    navigateToLogin();
                                    deleteCommunityValue(valueToDelete);
                                    deleteDataNodeForUser(valueToDelete);
                                    Toast.makeText(requireContext(), "Account Deleted", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                clearAllData();
                                navigateToLogin();
                                Toast.makeText(requireContext(), "Failed to delete account (0x4)", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid username", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
            }
        });
    }






    private void deleteCommunityValue(String valueToDelete) {
        Query query = communityRef.orderByKey().equalTo(valueToDelete);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Value successfully deleted
                                        Toast.makeText(requireContext(), "Value deleted from community", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Failed to delete value
                                        Toast.makeText(requireContext(), "Failed to delete value from community", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                } else {
                    // Value not found in community
                    Toast.makeText(requireContext(), "Value not found in community", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Error occurred
                Toast.makeText(requireContext(), "Error occurred while deleting value from community", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void deleteDataNodeForUser(String username) {
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference("DATA_" + username);
        dataRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Node successfully deleted
                        Toast.makeText(requireContext(), "DATA_" + username + " node deleted from Firebase", Toast.LENGTH_SHORT).show();
                    } else {
                        // Failed to delete node
                        Toast.makeText(requireContext(), "Failed to delete DATA_" + username + " node from Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }







    // Method to show the custom goal prompt
    private void showCustomGoalPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Custom Goal")
                .setMessage("Enter your custom goal steps count:");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_number_input, null);
        final EditText numberEditText = view.findViewById(R.id.numberEditText);
        Button minusButton = view.findViewById(R.id.minusButton);
        Button plusButton = view.findViewById(R.id.plusButton);

        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentNumber = Integer.parseInt(numberEditText.getText().toString().trim());
                if (currentNumber > 0) {
                    currentNumber--;
                    numberEditText.setText(String.valueOf(currentNumber));
                }
            }
        });

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentNumber = Integer.parseInt(numberEditText.getText().toString().trim());
                currentNumber++;
                numberEditText.setText(String.valueOf(currentNumber));
            }
        });

        builder.setView(view);

        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the entered goal steps count from the input field
                String customGoalText = numberEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(customGoalText)) {
                    int customGoal = Integer.parseInt(customGoalText);
                    saveGoal(customGoal);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    // Method to save the goal to shared preferences
    private void saveGoal(int goal) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (sharedPreferences.contains("goal")) {
            // Update the goal value
            editor.putInt("goal", goal);
        } else {
            // Create the goal and set it to the default value (10,000 steps)
            editor.putInt("goal", 10000);
        }

        editor.apply();
    }


    // Method to set the default goal (10,000 steps)
    private void setDefaultGoal() {
        saveGoal(10000);
    }






    private void changePassword() {
        String savedUsername = sharedPreferences.getString("saved_username", "");
        String savedPassword = sharedPreferences.getString("saved_password", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password")
                .setMessage("Are you sure you want to change your password?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Prompt for current password
                        AlertDialog.Builder passwordPromptBuilder = new AlertDialog.Builder(requireContext());
                        passwordPromptBuilder.setTitle("Enter Current Password");

                        // Create an EditText for password input
                        final EditText passwordEditText = new EditText(requireContext());
                        passwordPromptBuilder.setView(passwordEditText);

                        passwordPromptBuilder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String currentPassword = passwordEditText.getText().toString().trim();

                                // Check if current password matches
                                if (currentPassword.equals(savedPassword)) {
                                    // Prompt for new password
                                    AlertDialog.Builder newPasswordPromptBuilder = new AlertDialog.Builder(requireContext());
                                    newPasswordPromptBuilder.setTitle("Enter New Password");

                                    // Create an EditText for new password input
                                    final EditText newPasswordEditText = new EditText(requireContext());
                                    newPasswordPromptBuilder.setView(newPasswordEditText);

                                    newPasswordPromptBuilder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String newPassword = newPasswordEditText.getText().toString().trim();

                                            // Check if the password meets the requirements
                                            if (isValidPassword(newPassword)) {
                                                // Update password in Firebase
                                                usersRef.child(savedUsername).child("password").setValue(newPassword)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    // Update saved_password in SharedPreferences
                                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                                    editor.putString("saved_password", newPassword);
                                                                    editor.apply();

                                                                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(requireContext(), "Password must be 8 characters long and contain a number", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        private boolean isValidPassword(String password) {
                                            // Check if the password is 8 characters long
                                            if (password.length() < 8) {
                                                return false;
                                            }

                                            // Check if the password contains a number
                                            boolean containsNumber = false;
                                            for (char c : password.toCharArray()) {
                                                if (Character.isDigit(c)) {
                                                    containsNumber = true;
                                                    break;
                                                }
                                            }

                                            return containsNumber;
                                        }

                                    });

                                    newPasswordPromptBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });

                                    newPasswordPromptBuilder.show();
                                } else {
                                    Toast.makeText(requireContext(), "Invalid current password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        passwordPromptBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        passwordPromptBuilder.show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void clearAllData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();

        SharedPreferences sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        sharedPrefs.edit().putInt(getFormattedDate(), 0).apply();

        SharedPreferences sharedPrefswr = requireActivity().getSharedPreferences("WALKRUN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorwr = sharedPrefswr.edit();
        editorwr.clear().apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), login.class);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public String getFormattedDate() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }

}
