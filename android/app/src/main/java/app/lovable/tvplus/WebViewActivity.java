package app.lovable.tvplus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Full-screen WebView Activity for android_action_type: webview
 * Used for embedded web content and iframe-style playback
 */
public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout topBar;
    private TextView titleText;
    private String url;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable immersive fullscreen
        enableFullscreen();
        
        setContentView(R.layout.activity_webview);

        // Get intent extras
        url = getIntent().getStringExtra("url");
        title = getIntent().getStringExtra("title");

        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        setupViews();
        setupWebView();
        
        webView.loadUrl(url);
    }

    private void enableFullscreen() {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void setupViews() {
        webView = findViewById(R.id.webView);
        topBar = findViewById(R.id.top_bar);
        titleText = findViewById(R.id.title_text);
        ImageButton backButton = findViewById(R.id.back_button);

        if (title != null && !title.isEmpty()) {
            titleText.setText(title);
        }

        backButton.setOnClickListener(v -> finish());

        // Auto-hide top bar after 3 seconds
        topBar.postDelayed(() -> topBar.setVisibility(View.GONE), 3000);

        // Show/hide on touch
        webView.setOnClickListener(v -> {
            if (topBar.getVisibility() == View.VISIBLE) {
                topBar.setVisibility(View.GONE);
            } else {
                topBar.setVisibility(View.VISIBLE);
                topBar.postDelayed(() -> topBar.setVisibility(View.GONE), 3000);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // Set user agent
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " TVPlusAndroid/1.0");
        
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
