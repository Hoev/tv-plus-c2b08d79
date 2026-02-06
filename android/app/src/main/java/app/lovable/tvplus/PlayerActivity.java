package app.lovable.tvplus;

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
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import app.lovable.tvplus.databinding.ActivityPlayerBinding;

/**
 * Full-screen video player using ExoPlayer (Media3)
 * Supports HLS, DASH, MP4 with custom headers and ClearKey DRM
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    
    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    private PlayerView playerView;
    private StreamConfig streamConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Full screen immersive mode
        hideSystemUI();
        
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        playerView = binding.playerView;
        
        // Parse stream configuration from intent
        String jsonConfig = getIntent().getStringExtra("streamConfig");
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            Toast.makeText(this, "No stream configuration provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            Gson gson = new Gson();
            streamConfig = gson.fromJson(jsonConfig, StreamConfig.class);
            
            if (streamConfig.url == null || streamConfig.url.isEmpty()) {
                Toast.makeText(this, "Invalid stream URL", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Set title
            setupCustomControls();
            
            // Initialize player
            initializePlayer();
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing stream config", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupCustomControls() {
        // Find custom controller views
        TextView titleView = playerView.findViewById(R.id.exo_title);
        ImageButton backButton = playerView.findViewById(R.id.exo_back);
        
        if (titleView != null && streamConfig.title != null) {
            titleView.setText(streamConfig.title);
        }
        
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void initializePlayer() {
        // Build ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // Create data source factory with custom headers
        DataSource.Factory dataSourceFactory = createDataSourceFactory();
        
        // Create media source based on URL type
        MediaSource mediaSource = createMediaSource(dataSourceFactory);
        
        // Set up player
        player.setMediaSource(mediaSource);
        player.setPlayWhenReady(true);
        player.prepare();
        
        // Error listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());
                Toast.makeText(PlayerActivity.this, 
                    "Playback error: " + error.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    finish();
                }
            }
        });
    }

    private DataSource.Factory createDataSourceFactory() {
        DefaultHttpDataSource.Factory factory = new DefaultHttpDataSource.Factory();
        
        // Set connection timeouts
        factory.setConnectTimeoutMs(30000);
        factory.setReadTimeoutMs(30000);
        factory.setAllowCrossProtocolRedirects(true);
        
        // Apply custom headers
        if (streamConfig.hasHeaders()) {
            Map<String, String> headers = new HashMap<>();
            
            if (!streamConfig.getUserAgent().isEmpty()) {
                factory.setUserAgent(streamConfig.getUserAgent());
            }
            
            if (!streamConfig.getReferer().isEmpty()) {
                headers.put("Referer", streamConfig.getReferer());
            }
            
            if (!streamConfig.getCookie().isEmpty()) {
                headers.put("Cookie", streamConfig.getCookie());
            }
            
            if (!streamConfig.getOrigin().isEmpty()) {
                headers.put("Origin", streamConfig.getOrigin());
            }
            
            if (!headers.isEmpty()) {
                factory.setDefaultRequestProperties(headers);
            }
            
            Log.d(TAG, "Applied custom headers: UA=" + streamConfig.getUserAgent() + 
                ", Referer=" + streamConfig.getReferer());
        }
        
        return factory;
    }

    private MediaSource createMediaSource(DataSource.Factory dataSourceFactory) {
        String url = streamConfig.url.toLowerCase();
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
            .setUri(streamConfig.url);
        
        // Apply ClearKey DRM if provided
        if (streamConfig.hasDrm()) {
            String drm = streamConfig.drm;
            
            // Check if it's a URL or key:id format
            if (drm.startsWith("http")) {
                // License URL - not directly supported, need custom handling
                Log.d(TAG, "DRM License URL provided: " + drm);
            } else if (drm.contains(":")) {
                // ClearKey format: keyId:key
                try {
                    String[] parts = drm.split(":");
                    if (parts.length >= 2) {
                        String keyId = parts[0].trim();
                        String key = parts[1].trim();
                        
                        // Create ClearKey license
                        String clearKeyJson = createClearKeyLicense(keyId, key);
                        String clearKeyBase64 = Base64.encodeToString(
                            clearKeyJson.getBytes(), Base64.NO_WRAP);
                        
                        String licenseUri = "data:application/json;base64," + clearKeyBase64;
                        
                        mediaItemBuilder.setDrmConfiguration(
                            new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                                .setLicenseUri(licenseUri)
                                .build()
                        );
                        
                        Log.d(TAG, "Applied ClearKey DRM");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error applying DRM", e);
                }
            }
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        
        // Determine source type from URL
        if (url.contains(".m3u8") || url.contains("hls")) {
            return new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        } else if (url.contains(".mpd") || url.contains("dash")) {
            return new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        } else {
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        }
    }

    private String createClearKeyLicense(String keyId, String key) {
        // Convert hex to base64url
        String keyIdB64 = hexToBase64Url(keyId);
        String keyB64 = hexToBase64Url(key);
        
        return "{\"keys\":[{\"kty\":\"oct\",\"k\":\"" + keyB64 + 
               "\",\"kid\":\"" + keyIdB64 + "\"}],\"type\":\"temporary\"}";
    }

    private String hexToBase64Url(String hex) {
        // Remove any non-hex characters
        hex = hex.replaceAll("[^0-9A-Fa-f]", "");
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        
        // Base64 URL encode (no padding, URL safe chars)
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onStop() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
