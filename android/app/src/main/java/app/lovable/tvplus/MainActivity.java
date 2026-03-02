package app.lovable.tvplus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import app.lovable.tvplus.databinding.ActivityMainBinding;

/**
 * Main Activity hosting the WebView that loads the TV Plus web app
 * Handles the split Web/Android architecture with different action types
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WebView webView;
    private Gson gson = new Gson();
    
    // Your web app URL
    private static final String WEB_APP_URL = "https://tv-plus.lovable.app";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        webView = binding.webView;
        setupWebView();
        
        // Load the web app FIRST, then start security after a delay
        webView.loadUrl(WEB_APP_URL);
        
        // Delay security monitor to let WebView initialize properly
        webView.postDelayed(() -> {
            SecurityMonitor.getInstance(MainActivity.this).startMonitor();
        }, 3000);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // Enable JavaScript
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // Enable caching
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        
        // Enable media playback
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // Enable mixed content (HTTP + HTTPS)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // User Agent
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " TVPlusAndroid/1.0");
        
        // WebView clients
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep all navigation within the WebView
                return false;
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("MainActivity", "WebView error: " + description + " url: " + failingUrl);
                // Retry loading after a short delay
                view.postDelayed(() -> view.loadUrl(WEB_APP_URL), 2000);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
        
        // Add JavaScript interface for native player
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
    }

    /**
     * JavaScript Interface exposed to the web app
     * Call from JS: window.Android.playVideo(jsonConfig)
     */
    public class AndroidBridge {
        
        @JavascriptInterface
        public void playVideo(String jsonConfig) {
            runOnUiThread(() -> {
                try {
                    StreamConfig config = gson.fromJson(jsonConfig, StreamConfig.class);
                    
                    if (config == null) {
                        showToast("Invalid stream configuration");
                        return;
                    }
                    
                    // Handle different action types
                    if (config.isIntentAction() && config.intentUri != null) {
                        // Launch external app via Intent URI
                        launchIntent(config.intentUri);
                    } else if (config.isWebViewAction()) {
                        // Open in WebView Activity
                        openWebView(config.url, config.title);
                    } else {
                        // Default: Native player
                        openNativePlayer(jsonConfig);
                    }
                    
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
        
        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }
        
        @JavascriptInterface
        public String getAppVersion() {
            return "1.0.0";
        }
    }
    
    private void openNativePlayer(String jsonConfig) {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        intent.putExtra("streamConfig", jsonConfig);
        startActivity(intent);
    }
    
    private void openWebView(String url, String title) {
        Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title != null ? title : "");
        startActivity(intent);
    }
    
    private void launchIntent(String intentUri) {
        try {
            Intent intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
            
            // Check if app is installed
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Try to open in Play Store
                String packageName = intent.getPackage();
                if (packageName != null) {
                    Intent storeIntent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("market://details?id=" + packageName));
                    startActivity(storeIntent);
                } else {
                    showToast("Application not found");
                }
            }
        } catch (Exception e) {
            showToast("Failed to launch: " + e.getMessage());
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
