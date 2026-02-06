package app.lovable.tvplus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.lovable.tvplus.databinding.ActivityMainBinding;

/**
 * Main Activity hosting the WebView that loads the TV Plus web app
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WebView webView;
    
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
        
        // Load the web app
        webView.loadUrl(WEB_APP_URL);
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
                // Keep navigation within the WebView
                if (url.startsWith(WEB_APP_URL) || url.contains("lovable.app")) {
                    return false;
                }
                return false;
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
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.putExtra("streamConfig", jsonConfig);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, 
                        "Error launching player: " + e.getMessage(), 
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
