package app.lovable.tvplus;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Native ExoPlayer Activity with custom Gold & Black theme
 * Supports HLS, DASH, progressive streams with headers and DRM
 * TV Remote friendly with focusable controls
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private StreamConfig streamConfig;
    private Gson gson = new Gson();
    
    // Aspect ratio modes
    private int currentResizeMode = 0;
    private final int[] resizeModes = {
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force landscape and fullscreen
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        enableFullscreen();
        
        setContentView(R.layout.activity_player);
        
        playerView = findViewById(R.id.playerView);
        
        // Parse stream config from intent
        String configJson = getIntent().getStringExtra("streamConfig");
        if (configJson == null || configJson.isEmpty()) {
            Toast.makeText(this, "No stream configuration provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            streamConfig = gson.fromJson(configJson, StreamConfig.class);
            Log.d(TAG, "Stream URL: " + streamConfig.url);
            Log.d(TAG, "Has headers: " + streamConfig.hasHeaders());
            Log.d(TAG, "Has DRM: " + streamConfig.hasDrm());
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse stream config", e);
            Toast.makeText(this, "Invalid stream configuration", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupUI();
        initializePlayer();
    }

    private void enableFullscreen() {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void setupUI() {
        // Set title
        TextView titleView = playerView.findViewById(R.id.exo_title);
        if (titleView != null && streamConfig.title != null) {
            titleView.setText(streamConfig.title);
        }
        
        // Back button
        ImageButton backButton = playerView.findViewById(R.id.exo_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        // Resize button
        ImageButton resizeButton = playerView.findViewById(R.id.exo_resize);
        if (resizeButton != null) {
            resizeButton.setOnClickListener(v -> cycleResizeMode());
        }
        
        // PiP button (placeholder - requires Android O+)
        ImageButton pipButton = playerView.findViewById(R.id.exo_pip);
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> {
                Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Settings button for track selection
        ImageButton settingsButton = playerView.findViewById(R.id.exo_settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                // TODO: Show track selection dialog
                Toast.makeText(this, "Track selection coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void cycleResizeMode() {
        currentResizeMode = (currentResizeMode + 1) % resizeModes.length;
        playerView.setResizeMode(resizeModes[currentResizeMode]);
        
        String[] modeNames = {"Fit", "Fill", "Zoom"};
        Toast.makeText(this, modeNames[currentResizeMode], Toast.LENGTH_SHORT).show();
    }

    private void initializePlayer() {
        // Build data source factory with custom headers
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        
        // Build player
        player = new ExoPlayer.Builder(this)
            .build();
        
        playerView.setPlayer(player);
        
        // Build media source
        MediaSource mediaSource = buildMediaSource(dataSourceFactory);
        
        // Add error listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage(), error);
                Toast.makeText(PlayerActivity.this, 
                    "Playback error: " + error.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Playback ready");
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback ended");
                }
            }
        });
        
        // Prepare and play
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private DataSource.Factory buildDataSourceFactory() {
        DefaultHttpDataSource.Factory factory = new DefaultHttpDataSource.Factory();
        
        // Set timeouts
        factory.setConnectTimeoutMs(30000);
        factory.setReadTimeoutMs(30000);
        factory.setAllowCrossProtocolRedirects(true);
        
        // Apply custom headers if present
        if (streamConfig.hasHeaders()) {
            Map<String, String> headers = new HashMap<>();
            
            if (!streamConfig.getUserAgent().isEmpty()) {
                factory.setUserAgent(streamConfig.getUserAgent());
                Log.d(TAG, "Setting User-Agent: " + streamConfig.getUserAgent());
            }
            
            if (!streamConfig.getReferer().isEmpty()) {
                headers.put("Referer", streamConfig.getReferer());
                Log.d(TAG, "Setting Referer: " + streamConfig.getReferer());
            }
            
            if (!streamConfig.getCookie().isEmpty()) {
                headers.put("Cookie", streamConfig.getCookie());
                Log.d(TAG, "Setting Cookie: " + streamConfig.getCookie());
            }
            
            if (!streamConfig.getOrigin().isEmpty()) {
                headers.put("Origin", streamConfig.getOrigin());
                Log.d(TAG, "Setting Origin: " + streamConfig.getOrigin());
            }
            
            if (!headers.isEmpty()) {
                factory.setDefaultRequestProperties(headers);
            }
        }
        
        return factory;
    }

    private MediaSource buildMediaSource(DataSource.Factory dataSourceFactory) {
        String url = streamConfig.url;
        
        // IMPORTANT: Do NOT strip or modify URL - preserve tokens, commas, etc.
        Uri uri = Uri.parse(url);
        
        // Build MediaItem
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
            .setUri(uri);
        
        // Apply DRM if configured
        if (streamConfig.hasDrm() && streamConfig.drm != null) {
            MediaItem.DrmConfiguration.Builder drmBuilder;
            
            String scheme = streamConfig.drm.scheme != null ? 
                streamConfig.drm.scheme.toLowerCase() : "clearkey";
            
            if ("widevine".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID);
                if (streamConfig.drm.licenseUrl != null) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            } else if ("playready".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.PLAYREADY_UUID);
                if (streamConfig.drm.licenseUrl != null) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            } else {
                // ClearKey
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID);
                
                // Build ClearKey JSON if keyId and key are provided
                if (streamConfig.drm.keyId != null && streamConfig.drm.key != null) {
                    String clearKeyJson = buildClearKeyJson(
                        streamConfig.drm.keyId, 
                        streamConfig.drm.key
                    );
                    drmBuilder.setLicenseUri("data:application/json;base64," + 
                        Base64.encodeToString(clearKeyJson.getBytes(), Base64.NO_WRAP));
                } else if (streamConfig.drm.licenseUrl != null) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            }
            
            mediaItemBuilder.setDrmConfiguration(drmBuilder.build());
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        
        // Detect stream type and build appropriate source
        String path = uri.getPath();
        if (path == null) path = url;
        
        if (path.contains(".m3u8") || url.contains("m3u8")) {
            Log.d(TAG, "Building HLS source");
            return new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem);
        } else if (path.contains(".mpd") || url.contains("mpd")) {
            Log.d(TAG, "Building DASH source");
            return new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        } else {
            Log.d(TAG, "Building Progressive source");
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        }
    }

    private String buildClearKeyJson(String keyId, String key) {
        // Convert hex to base64url
        String keyIdB64 = hexToBase64Url(keyId);
        String keyB64 = hexToBase64Url(key);
        
        return String.format(
            "{\"keys\":[{\"kty\":\"oct\",\"k\":\"%s\",\"kid\":\"%s\"}],\"type\":\"temporary\"}",
            keyB64, keyIdB64
        );
    }

    private String hexToBase64Url(String hex) {
        // Remove any spaces or colons
        hex = hex.replaceAll("[:\\s-]", "");
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        
        // Base64URL encoding (no padding, url-safe)
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
        return base64;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
}
