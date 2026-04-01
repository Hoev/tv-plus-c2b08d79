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
 * Splash screen - shows loading bar only, no text feedback
 * Now launches ComposeActivity instead of HomeActivity
 */
public class SplashActivity extends AppCompatActivity {

    private TextView statusText;
    private ProgressBar progressBar;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusText = findViewById(R.id.splash_status);
        progressBar = findViewById(R.id.splash_progress);
        errorText = findViewById(R.id.splash_error);

        // Hide status text - just show loading bar
        statusText.setVisibility(View.GONE);

        // Run security check
        SecurityMonitor.getInstance(this).runInitialCheckAsync((passed, failReason) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (passed) {
                    // Launch Compose UI instead of legacy HomeActivity
                    startActivity(new Intent(SplashActivity.this, ComposeActivity.class));
                    finish();
                    SecurityMonitor.getInstance(SplashActivity.this).startMonitor();
                } else {
                    // Security check failed
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
