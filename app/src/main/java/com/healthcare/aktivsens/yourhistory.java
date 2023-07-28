package com.healthcare.aktivsens;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class yourhistory extends AppCompatActivity {
    private SharedPreferences sharedPref;
    public int selectedValue;
    public String savedUsername;
    public int savedValue;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.yourhistory);

        sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        // Get the saved username and password from shared preferences
        savedUsername = sharedPref.getString("saved_username", "");
        String savedPassword = sharedPref.getString("saved_password", "");
        savedValue = sharedPref.getInt("selectedValue", 0);

        WebView webView = findViewById(R.id.webview);
        TextView days = findViewById(R.id.days);
        webView.setWebViewClient(new CustomWebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);



        webView.loadUrl("file:///android_asset/html/graph.html?user="+savedUsername+"&days="+savedValue);



        SeekBar seekBar = findViewById(R.id.seekbar);
        seekBar.setMax(31);

        int progressValue = sharedPref.getInt("selectedValue", 0);; // Set the desired progress value
        seekBar.setProgress(progressValue);
        days.setText(String.valueOf(progressValue)+" DAYS");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedValue = seekBar.getProgress();
                days.setText(String.valueOf(selectedValue)+" DAYS");



            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Called when the user starts interacting with the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Called when the user stops interacting with the SeekBar
                // You can access the final progress value using seekBar.getProgress()
                // Save selectedValue in SharedPreferences
                SharedPreferences sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt("selectedValue", selectedValue);
                editor.apply();

                webView.loadUrl("file:///android_asset/html/graph.html?user="+savedUsername+"&days="+selectedValue);

            }
        });



    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}
