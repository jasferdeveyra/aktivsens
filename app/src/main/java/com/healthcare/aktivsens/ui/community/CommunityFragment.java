package com.healthcare.aktivsens.ui.community;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.healthcare.aktivsens.R;
import com.healthcare.aktivsens.databinding.FragmentCommunityBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private SharedPreferences sharedPref;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            //Window window = activity.getWindow();
            //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //activity.getSupportActionBar().hide();
        }

        sharedPref = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        // Get the saved username and password from shared preferences
        String savedUsername = sharedPref.getString("saved_username", "");
        String savedPassword = sharedPref.getString("saved_password", "");

        webView = root.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new CustomWebViewClient());

        webView.loadUrl("file:///android_asset/html/community.html");

        uploadDataToFirebase(savedUsername);

        return root;
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // Handle the error and redirect to a custom page
            webView.loadUrl("file:///android_asset/html/error.html");
        }
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
    public String getFormattedDat2() {
        // Get the current date
        Date currentDate = new Date();

        // Define the desired date format with month as abbreviated name
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

        // Format the date as "MMM DD"
        return dateFormat.format(currentDate);
    }
    public int retrieveFinCount() {
        SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        return sharedPrefs.getInt(getFormattedDat2(), 0);
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

    public WebView webView;



}
