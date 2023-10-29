package com.sliit.blindvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private TextView directionTextView;
    private EditText ipEditText;
    private Button setIpButton;
    private static final String PREFS_NAME = "IP_PREFS";
    private static final String IP_KEY = "IP_KEY";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEditText = findViewById(R.id.ipEditText);
        setIpButton = findViewById(R.id.setIpButton);

        webView = findViewById(R.id.webView);


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String savedIp = settings.getString(IP_KEY, "192.168.1.2");
        ipEditText.setText(savedIp);

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        setIpButton.setOnClickListener(v -> {
            String newIp = ipEditText.getText().toString();
            if (!newIp.isEmpty()) {
                // Save the new IP
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(IP_KEY, newIp);
                editor.apply();

                // Update the WebView and direction fetcher with the new IP
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });


        WebSettings webSettings = webView.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        directionTextView = findViewById(R.id.directionTextView);

        // Load the video feed
        webView.loadUrl("http://"+savedIp+":5005/video_feed");

        // Fetch the direction results periodically
        new Thread(() -> {
            Log.d("direction", "starting");
            while (true) {
                try {
                    URL url = new URL("http://"+savedIp+":5005/direction");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    String result = Utils.streamToString(connection.getInputStream());
                    JSONObject jsonObject = new JSONObject(result);
                    Log.d("direction", jsonObject.toString());
//                    String direction = jsonObject.getString("direction");
//
//                    runOnUiThread(() -> directionTextView.setText(direction));
                    String direction = jsonObject.getString("direction");

                    runOnUiThread(() -> {
                        directionTextView.setText(direction);
                        tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null, null);  // Speak the direction
                    });

                    Thread.sleep(4000);  // Fetch every 5 seconds
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}