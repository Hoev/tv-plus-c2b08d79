package app.lovable.tvplus;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native ExoPlayer Activity with Gold & Black theme
 * Features:
 * - HLS, DASH, MP4 support with proper URL handling (preserves tokens/commas)
 * - Custom headers (User-Agent, Referer, Cookie)
 * - ClearKey/Widevine DRM
 * - Track selection dialog (Video Quality & Audio)
 * - Aspect ratio cycling
 * - Picture-in-Picture (Android O+)
 * - TV Remote navigation with focusable controls
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private StreamConfig streamConfig;
    private Gson gson = new Gson();
    
    // Aspect ratio modes
    private int currentResizeMode = 0;
    private final int[] resizeModes = {
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    };
    private final String[] resizeModeNames = {"Fit", "Fill", "Zoom"};

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
        
        // Resize button - cycle through aspect ratios
        ImageButton resizeButton = playerView.findViewById(R.id.exo_resize);
        if (resizeButton != null) {
            resizeButton.setOnClickListener(v -> cycleResizeMode());
        }
        
        // PiP button
        ImageButton pipButton = playerView.findViewById(R.id.exo_pip);
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> enterPiPMode());
        }
        
        // Settings button - Track selection dialog
        ImageButton settingsButton = playerView.findViewById(R.id.exo_settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showTrackSelectionDialog());
        }
    }

    private void cycleResizeMode() {
        currentResizeMode = (currentResizeMode + 1) % resizeModes.length;
        playerView.setResizeMode(resizeModes[currentResizeMode]);
        Toast.makeText(this, resizeModeNames[currentResizeMode], Toast.LENGTH_SHORT).show();
    }

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational aspectRatio = new Rational(16, 9);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Log.e(TAG, "PiP failed", e);
                Toast.makeText(this, "PiP not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            // Hide controls in PiP
            playerView.setUseController(false);
        } else {
            // Show controls when exiting PiP
            playerView.setUseController(true);
        }
    }

    /**
     * Show track selection dialog with VIDEO and AUDIO tabs
     * Matches the reference design with gold accent
     */
    private void showTrackSelectionDialog() {
        if (player == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select tracks");
        
        // Get available tracks
        Tracks tracks = player.getCurrentTracks();
        List<String> videoTracks = new ArrayList<>();
        List<String> audioTracks = new ArrayList<>();
        List<TrackGroup> videoGroups = new ArrayList<>();
        List<TrackGroup> audioGroups = new ArrayList<>();
        List<Integer> videoIndexes = new ArrayList<>();
        List<Integer> audioIndexes = new ArrayList<>();
        
        // Populate track lists
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            TrackGroup group = trackGroup.getMediaTrackGroup();
            int trackType = trackGroup.getType();
            
            for (int i = 0; i < group.length; i++) {
                Format format = group.getFormat(i);
                
                if (trackType == C.TRACK_TYPE_VIDEO) {
                    String label = format.width + " × " + format.height;
                    if (format.bitrate > 0) {
                        label += ", " + String.format("%.2f Mbps", format.bitrate / 1000000f);
                    }
                    videoTracks.add(label);
                    videoGroups.add(group);
                    videoIndexes.add(i);
                } else if (trackType == C.TRACK_TYPE_AUDIO) {
                    String label = format.language != null ? format.language : "Audio";
                    if (format.label != null) {
                        label = format.label;
                    }
                    if (format.bitrate > 0) {
                        label += " (" + (format.bitrate / 1000) + " kbps)";
                    }
                    audioTracks.add(label);
                    audioGroups.add(group);
                    audioIndexes.add(i);
                }
            }
        }
        
        // Create dialog items
        List<String> items = new ArrayList<>();
        items.add("── VIDEO ──");
        items.add("Auto");
        items.addAll(videoTracks);
        items.add("");
        items.add("── AUDIO ──");
        items.add("Auto");
        items.addAll(audioTracks);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, items);
        
        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        
        final int videoOffset = 2; // After header and Auto
        final int audioOffset = videoOffset + videoTracks.size() + 3; // After video section, space, header, Auto
        
        builder.setView(listView);
        AlertDialog dialog = builder.create();
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0 || position == videoOffset + videoTracks.size() || 
                position == videoOffset + videoTracks.size() + 1) {
                // Header or spacer - ignore
                return;
            }
            
            if (position == 1) {
                // Video Auto
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                );
                Toast.makeText(this, "Video: Auto", Toast.LENGTH_SHORT).show();
            } else if (position > 1 && position < videoOffset + videoTracks.size()) {
                // Specific video track
                int trackIndex = position - videoOffset;
                if (trackIndex >= 0 && trackIndex < videoGroups.size()) {
                    TrackGroup group = videoGroups.get(trackIndex);
                    int formatIndex = videoIndexes.get(trackIndex);
                    TrackSelectionOverride override = new TrackSelectionOverride(
                        group, List.of(formatIndex));
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .addOverride(override)
                            .build()
                    );
                    Toast.makeText(this, "Video: " + videoTracks.get(trackIndex), Toast.LENGTH_SHORT).show();
                }
            } else if (position == audioOffset - 1) {
                // Audio Auto
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .build()
                );
                Toast.makeText(this, "Audio: Auto", Toast.LENGTH_SHORT).show();
            } else if (position >= audioOffset) {
                // Specific audio track
                int trackIndex = position - audioOffset;
                if (trackIndex >= 0 && trackIndex < audioGroups.size()) {
                    TrackGroup group = audioGroups.get(trackIndex);
                    int formatIndex = audioIndexes.get(trackIndex);
                    TrackSelectionOverride override = new TrackSelectionOverride(
                        group, List.of(formatIndex));
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .addOverride(override)
                            .build()
                    );
                    Toast.makeText(this, "Audio: " + audioTracks.get(trackIndex), Toast.LENGTH_SHORT).show();
                }
            }
            
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        dialog.show();
    }

    private void initializePlayer() {
        // Build data source factory with custom headers
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        
        // Create track selector for quality switching
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSizeSd() // Start with SD for faster loading
                .build()
        );
        
        // Build player with track selector
        player = new ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build();
        
        playerView.setPlayer(player);
        
        // Build media source
        MediaSource mediaSource = buildMediaSource(dataSourceFactory);
        
        // Add error listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage(), error);
                String errorMsg = "Playback error";
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    errorMsg = "Network error - check connection";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    errorMsg = "Stream unavailable (HTTP error)";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED) {
                    errorMsg = "Invalid stream format";
                }
                Toast.makeText(PlayerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Playback ready");
                    // Allow higher quality now
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .build()
                    );
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
        
        // Set generous timeouts for slow streams
        factory.setConnectTimeoutMs(30000);
        factory.setReadTimeoutMs(30000);
        factory.setAllowCrossProtocolRedirects(true);
        
        // Apply custom headers if present
        if (streamConfig.hasHeaders()) {
            Map<String, String> headers = new HashMap<>();
            
            if (!streamConfig.getUserAgent().isEmpty()) {
                factory.setUserAgent(streamConfig.getUserAgent());
                Log.d(TAG, "Setting User-Agent: " + streamConfig.getUserAgent());
            } else {
                // Default User-Agent for compatibility
                factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
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
        } else {
            // Default User-Agent even without custom headers
            factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        }
        
        return factory;
    }

    private MediaSource buildMediaSource(DataSource.Factory dataSourceFactory) {
        // CRITICAL: Preserve URL exactly as provided - DO NOT strip tokens, commas, or parameters
        String url = streamConfig.url;
        Log.d(TAG, "Building media source for URL: " + url);
        
        // Use Uri.parse which properly handles complex URLs with tokens
        Uri uri = Uri.parse(url);
        
        // Build MediaItem
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
            .setUri(uri);
        
        // Apply DRM if configured
        if (streamConfig.hasDrm() && streamConfig.drm != null) {
            MediaItem.DrmConfiguration.Builder drmBuilder;
            
            String scheme = streamConfig.drm.scheme != null ? 
                streamConfig.drm.scheme.toLowerCase() : "clearkey";
            
            Log.d(TAG, "Configuring DRM: " + scheme);
            
            if ("widevine".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID);
                if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            } else if ("playready".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.PLAYREADY_UUID);
                if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            } else {
                // ClearKey
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID);
                
                // Build ClearKey JSON if keyId and key are provided
                if (streamConfig.drm.keyId != null && !streamConfig.drm.keyId.isEmpty() &&
                    streamConfig.drm.key != null && !streamConfig.drm.key.isEmpty()) {
                    String clearKeyJson = buildClearKeyJson(
                        streamConfig.drm.keyId, 
                        streamConfig.drm.key
                    );
                    String dataUri = "data:application/json;base64," + 
                        Base64.encodeToString(clearKeyJson.getBytes(), Base64.NO_WRAP);
                    drmBuilder.setLicenseUri(dataUri);
                    Log.d(TAG, "ClearKey configured");
                } else if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            }
            
            mediaItemBuilder.setDrmConfiguration(drmBuilder.build());
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        
        // Detect stream type and build appropriate source
        // Check URL path and full URL for format indicators
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains("/hls/") || lowerUrl.contains("format=m3u8")) {
            Log.d(TAG, "Building HLS source");
            return new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem);
        } else if (lowerUrl.contains(".mpd") || lowerUrl.contains("/dash/") || lowerUrl.contains("format=mpd")) {
            Log.d(TAG, "Building DASH source");
            return new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        } else {
            Log.d(TAG, "Building Progressive source (MP4/other)");
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
        // Remove any spaces, colons, or dashes
        hex = hex.replaceAll("[:\\s\\-]", "");
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        
        // Base64URL encoding (no padding, url-safe)
        return Base64.encodeToString(data, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
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
        if (player != null && !checkPipMode()) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && !checkPipMode()) {
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
    
    private boolean checkPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return super.isInPictureInPictureMode();
        }
        return false;
    }
}
