package com.apix.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Single Splash screen - shows app name + loading spinner only.
 * No app icon, no status text. Clean transition to ComposeActivity.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView statusText = findViewById(R.id.splash_status);
        ProgressBar progressBar = findViewById(R.id.splash_progress);
        TextView errorText = findViewById(R.id.splash_error);

        // Hide status text - just show loading bar
        statusText.setVisibility(View.GONE);

        // Run security check
        SecurityMonitor.getInstance(this).runInitialCheckAsync((passed, failReason) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (passed) {
                    startActivity(new Intent(SplashActivity.this, ComposeActivity.class));
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    SecurityMonitor.getInstance(SplashActivity.this).startMonitor();
                } else {
                    progressBar.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(failReason != null ? failReason : "فشل فحص الأمان");
                    
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        finishAffinity();
                        System.exit(0);
                    }, 3000);
                }
            });
        });
    }
}
