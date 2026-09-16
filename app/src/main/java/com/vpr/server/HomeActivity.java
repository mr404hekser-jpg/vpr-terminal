package com.vpr.server;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            setContentView(R.layout.activity_home);

            // Start VPR Service
            startService(new Intent(this, VPRService.class));

            // Storage info
            StorageManager sm = new StorageManager(this);
            TextView tvStorage = findViewById(R.id.tvStorageInfo);
            tvStorage.setText(
                "Storage: " + StorageManager.formatSize(sm.getVPRUsedBytes()) +
                " used | " + StorageManager.formatSize(sm.getFreeBytes()) + " free"
            );

            // Card VPR Server
            LinearLayout cardVPR = findViewById(R.id.cardVPR);
            cardVPR.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
            });

            // Card Hacking Suite
            LinearLayout cardHacking = findViewById(R.id.cardHacking);
            cardHacking.setOnClickListener(v -> {
                startActivity(new Intent(this, HackingActivity.class));
            });

        } catch (Exception e) { e.printStackTrace(); }
    }
}
