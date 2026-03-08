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
 * Splash screen with 3-second security check
 * Shows loading animation while performing all security checks
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

        statusText.setText("جارِ فحص الأمان...");

        // Run security check
        SecurityMonitor.getInstance(this).runInitialCheckAsync((passed, failReason) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (passed) {
                    statusText.setText("تم التحقق ✓");
                    // Wait a moment then launch main activity
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                        finish();
                        // Start continuous monitoring
                        SecurityMonitor.getInstance(SplashActivity.this).startMonitor();
                    }, 500);
                } else {
                    // Security check failed
                    progressBar.setVisibility(View.GONE);
                    statusText.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(failReason != null ? failReason : "فشل فحص الأمان");
                    
                    // Kill app after showing message
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        finishAffinity();
                        System.exit(0);
                    }, 3000);
                }
            });
        });
    }
}
