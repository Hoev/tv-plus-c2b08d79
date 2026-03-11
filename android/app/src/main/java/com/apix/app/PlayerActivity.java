package com.apix.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Rational;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native ExoPlayer Activity with Gold & Black theme
 * Full MPD/DASH reliability: proper player cleanup between streams,
 * isolated error handling, and robust format detection.
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private StreamConfig streamConfig;
    private Gson gson = new Gson();
    private int currentServerIndex = 0;
    
    // Track selection state
    private int selectedVideoTrackIndex = -1;
    private int selectedAudioTrackIndex = -1;
    
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
        
        SecurityMonitor.getInstance(this).startMonitor();
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        enableFullscreen();
        
        setContentView(R.layout.activity_player);
        
        playerView = findViewById(R.id.playerView);
        
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
        ImageButton backButton = playerView.findViewById(R.id.exo_back);
        if (backButton != null) backButton.setOnClickListener(v -> finish());
        
        ImageButton resizeButton = playerView.findViewById(R.id.exo_resize);
        if (resizeButton != null) resizeButton.setOnClickListener(v -> cycleResizeMode());
        
        ImageButton pipButton = playerView.findViewById(R.id.exo_pip);
        if (pipButton != null) pipButton.setOnClickListener(v -> enterPiPMode());
        
        ImageButton settingsButton = playerView.findViewById(R.id.exo_settings);
        if (settingsButton != null) settingsButton.setOnClickListener(v -> showTrackSelectionDialog());
        
        ImageButton subtitleButton = playerView.findViewById(R.id.exo_subtitle);
        if (subtitleButton != null) subtitleButton.setOnClickListener(v -> showSubtitleSelectionDialog());
        
        ImageButton serverButton = playerView.findViewById(R.id.exo_server);
        if (serverButton != null) {
            if (streamConfig.hasServers() && streamConfig.servers.size() > 1) {
                serverButton.setVisibility(View.VISIBLE);
                serverButton.setOnClickListener(v -> showServerSelectionDialog());
            } else {
                serverButton.setVisibility(View.GONE);
            }
        }
    }
    
    private void showSubtitleSelectionDialog() {
        if (player == null) return;
        
        Tracks tracks = player.getCurrentTracks();
        List<TrackInfo> subtitleTracks = new ArrayList<>();
        
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            TrackGroup group = trackGroup.getMediaTrackGroup();
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getFormat(i);
                    String label = format.label != null ? format.label : 
                                   (format.language != null ? format.language : "Subtitle " + (i + 1));
                    subtitleTracks.add(new TrackInfo(label, group, i, 0, 0));
                }
            }
        }
        
        if (subtitleTracks.isEmpty()) {
            Toast.makeText(this, "No subtitles available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options = new String[subtitleTracks.size() + 1];
        options[0] = "Off";
        for (int i = 0; i < subtitleTracks.size(); i++) {
            options[i + 1] = subtitleTracks.get(i).label;
        }
        
        new AlertDialog.Builder(this, R.style.GoldDialogTheme)
            .setTitle("Subtitles")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    );
                } else {
                    TrackInfo track = subtitleTracks.get(which - 1);
                    TrackSelectionOverride override = new TrackSelectionOverride(
                        track.group, Collections.singletonList(track.formatIndex));
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .addOverride(override)
                            .build()
                    );
                }
            })
            .show();
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
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        playerView.setUseController(!isInPictureInPictureMode);
    }

    private void showServerSelectionDialog() {
        if (!streamConfig.hasServers()) return;
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_server_selection);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.6),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        
        RadioGroup radioGroup = dialog.findViewById(R.id.server_radio_group);
        
        for (int i = 0; i < streamConfig.servers.size(); i++) {
            StreamConfig.Server server = streamConfig.servers.get(i);
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(server.name != null ? server.name : "Server " + (i + 1));
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(16);
            rb.setPadding(16, 24, 16, 24);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
            rb.setChecked(i == currentServerIndex);
            rb.setFocusable(true);
            rb.setTag(i);
            radioGroup.addView(rb);
        }
        
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_ok);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnOk.setOnClickListener(v -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton selected = dialog.findViewById(checkedId);
                int index = (int) selected.getTag();
                if (index != currentServerIndex) {
                    currentServerIndex = index;
                    switchServer(index);
                }
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void switchServer(int index) {
        if (index < 0 || index >= streamConfig.servers.size()) return;
        
        StreamConfig.Server server = streamConfig.servers.get(index);
        streamConfig.url = server.url;
        
        Toast.makeText(this, "Switching to: " + (server.name != null ? server.name : "Server " + (index + 1)), Toast.LENGTH_SHORT).show();
        
        // CRITICAL: Fully release old player before creating new one
        releasePlayer();
        initializePlayer();
    }

    private void showTrackSelectionDialog() {
        if (player == null) return;
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_track_selection);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        int dialogWidth = (int)(getResources().getDisplayMetrics().widthPixels * 0.6);
        int maxDialogHeight = (int)(getResources().getDisplayMetrics().heightPixels * 0.75);
        
        dialog.getWindow().setLayout(dialogWidth, maxDialogHeight);
        
        TextView tabVideo = dialog.findViewById(R.id.tab_video);
        TextView tabAudio = dialog.findViewById(R.id.tab_audio);
        View tabIndicator = dialog.findViewById(R.id.tab_indicator);
        ScrollView videoContainer = dialog.findViewById(R.id.video_container);
        ScrollView audioContainer = dialog.findViewById(R.id.audio_container);
        RadioGroup videoRadioGroup = dialog.findViewById(R.id.video_radio_group);
        RadioGroup audioRadioGroup = dialog.findViewById(R.id.audio_radio_group);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_ok);
        
        tabIndicator.post(() -> {
            ViewGroup.LayoutParams params = tabIndicator.getLayoutParams();
            params.width = tabVideo.getWidth();
            tabIndicator.setLayoutParams(params);
        });
        
        Tracks tracks = player.getCurrentTracks();
        List<TrackInfo> videoTracks = new ArrayList<>();
        List<TrackInfo> audioTracks = new ArrayList<>();
        
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
                    videoTracks.add(new TrackInfo(label, group, i, format.height, format.bitrate));
                } else if (trackType == C.TRACK_TYPE_AUDIO) {
                    String label = format.label != null ? format.label : 
                                   (format.language != null ? format.language : "Audio");
                    if (format.bitrate > 0) {
                        label += " (" + (format.bitrate / 1000) + " kbps)";
                    }
                    audioTracks.add(new TrackInfo(label, group, i, 0, format.bitrate));
                }
            }
        }
        
        Collections.sort(videoTracks, (a, b) -> {
            if (b.height != a.height) return b.height - a.height;
            return b.bitrate - a.bitrate;
        });
        
        RadioButton rbNone = createDarkRadioButton("None", -2, selectedVideoTrackIndex == -2);
        videoRadioGroup.addView(rbNone);
        
        RadioButton rbAuto = createDarkRadioButton("Auto", -1, selectedVideoTrackIndex == -1);
        audioRadioGroup.addView(rbAuto);
        
        for (int i = 0; i < videoTracks.size(); i++) {
            TrackInfo track = videoTracks.get(i);
            RadioButton rb = createDarkRadioButton(track.label, i, selectedVideoTrackIndex == i);
            rb.setTag(R.id.tab_video, track);
            videoRadioGroup.addView(rb);
        }
        
        RadioButton rbAudioAuto = createDarkRadioButton("Auto", -1, selectedAudioTrackIndex == -1);
        audioRadioGroup.addView(rbAudioAuto);
        
        for (int i = 0; i < audioTracks.size(); i++) {
            TrackInfo track = audioTracks.get(i);
            RadioButton rb = createDarkRadioButton(track.label, i, selectedAudioTrackIndex == i);
            rb.setTag(R.id.tab_audio, track);
            audioRadioGroup.addView(rb);
        }
        
        final List<TrackInfo> finalVideoTracks = videoTracks;
        final List<TrackInfo> finalAudioTracks = audioTracks;
        
        tabVideo.setOnClickListener(v -> {
            tabVideo.setTextColor(Color.parseColor("#FFD700"));
            tabAudio.setTextColor(Color.parseColor("#666666"));
            videoContainer.setVisibility(View.VISIBLE);
            audioContainer.setVisibility(View.GONE);
            animateIndicator(tabIndicator, 0);
        });
        
        tabAudio.setOnClickListener(v -> {
            tabAudio.setTextColor(Color.parseColor("#FFD700"));
            tabVideo.setTextColor(Color.parseColor("#666666"));
            audioContainer.setVisibility(View.VISIBLE);
            videoContainer.setVisibility(View.GONE);
            animateIndicator(tabIndicator, tabVideo.getWidth());
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnOk.setOnClickListener(v -> {
            int videoCheckedId = videoRadioGroup.getCheckedRadioButtonId();
            if (videoCheckedId != -1) {
                RadioButton videoRb = dialog.findViewById(videoCheckedId);
                int index = (int) videoRb.getTag();
                selectedVideoTrackIndex = index;
                
                if (index == -2) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                            .build()
                    );
                } else if (index == -1) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .build()
                    );
                } else if (index < finalVideoTracks.size()) {
                    TrackInfo track = finalVideoTracks.get(index);
                    TrackSelectionOverride override = new TrackSelectionOverride(
                        track.group, Collections.singletonList(track.formatIndex));
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                            .addOverride(override)
                            .build()
                    );
                }
            }
            
            int audioCheckedId = audioRadioGroup.getCheckedRadioButtonId();
            if (audioCheckedId != -1) {
                RadioButton audioRb = dialog.findViewById(audioCheckedId);
                int index = (int) audioRb.getTag();
                selectedAudioTrackIndex = index;
                
                if (index == -1) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .build()
                    );
                } else if (index < finalAudioTracks.size()) {
                    TrackInfo track = finalAudioTracks.get(index);
                    TrackSelectionOverride override = new TrackSelectionOverride(
                        track.group, Collections.singletonList(track.formatIndex));
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .addOverride(override)
                            .build()
                    );
                }
            }
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private RadioButton createDarkRadioButton(String text, int index, boolean checked) {
        RadioButton rb = new RadioButton(this);
        rb.setId(View.generateViewId());
        rb.setText(text);
        rb.setTextColor(Color.WHITE);
        rb.setTextSize(16);
        rb.setPadding(16, 24, 16, 24);
        rb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
        rb.setChecked(checked);
        rb.setFocusable(true);
        rb.setTag(index);
        return rb;
    }
    
    private void animateIndicator(View indicator, float translationX) {
        indicator.animate()
            .translationX(translationX)
            .setDuration(200)
            .start();
    }
    
    private static class TrackInfo {
        String label;
        TrackGroup group;
        int formatIndex;
        int height;
        int bitrate;
        
        TrackInfo(String label, TrackGroup group, int formatIndex, int height, int bitrate) {
            this.label = label;
            this.group = group;
            this.formatIndex = formatIndex;
            this.height = height;
            this.bitrate = bitrate;
        }
    }

    private void initializePlayer() {
        // CRITICAL: Always create fresh instances - never reuse stale player/trackSelector
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSizeSd()
                .build()
        );
        
        player = new ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build();
        
        playerView.setPlayer(player);
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage() + " code=" + error.errorCode, error);
                String errorMsg = "Playback error";
                
                int code = error.errorCode;
                if (code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    errorMsg = "Network error";
                } else if (code == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    errorMsg = "Stream unavailable (HTTP error)";
                } else if (code == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED) {
                    errorMsg = "DRM license failed";
                } else if (code == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED) {
                    errorMsg = "DRM not supported";
                } else if (code == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED) {
                    errorMsg = "Invalid stream format";
                } else if (code == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                           code == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    errorMsg = "Unsupported stream format";
                }
                
                Toast.makeText(PlayerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                
                // Don't auto-finish - let user try server switch or back out manually
            }
            
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .build()
                    );
                }
            }
        });
        
        // Build media source async (ClearKey may need network)
        buildMediaSourceAsync(dataSourceFactory, mediaSource -> {
            runOnUiThread(() -> {
                if (player != null && !isFinishing()) {
                    player.setMediaSource(mediaSource);
                    player.prepare();
                    player.setPlayWhenReady(true);
                }
            });
        });
    }
    
    private interface MediaSourceCallback {
        void onReady(MediaSource source);
    }
    
    private void buildMediaSourceAsync(DataSource.Factory factory, MediaSourceCallback callback) {
        if (!streamConfig.hasDrm() || streamConfig.drm == null) {
            callback.onReady(buildMediaSource(factory, null));
            return;
        }
        
        String scheme = streamConfig.drm.scheme != null ? 
            streamConfig.drm.scheme.toLowerCase() : "clearkey";
        
        if (!"clearkey".equals(scheme)) {
            callback.onReady(buildMediaSource(factory, null));
            return;
        }
        
        new Thread(() -> {
            try {
                String resolvedClearKeyJson = resolveClearKey();
                callback.onReady(buildMediaSource(factory, resolvedClearKeyJson));
            } catch (Exception e) {
                Log.e(TAG, "ClearKey resolution failed", e);
                // Still try to build without ClearKey
                callback.onReady(buildMediaSource(factory, null));
            }
        }).start();
    }
    
    /**
     * Resolve ClearKey JSON from various sources (runs on background thread)
     */
    private String resolveClearKey() {
        if (streamConfig.drm == null) return null;
        
        String keyId = streamConfig.drm.keyId;
        String key = streamConfig.drm.key;
        String licenseUrl = streamConfig.drm.licenseUrl;
        
        // Strategy 1: License URL API endpoint (no keyId provided)
        if (licenseUrl != null && !licenseUrl.isEmpty() && 
            licenseUrl.startsWith("http") && (keyId == null || keyId.isEmpty())) {
            Log.d(TAG, "ClearKey: Fetching from license API: " + licenseUrl);
            String json = fetchClearKeyFromApi(licenseUrl);
            if (json != null) return json;
        }
        
        // Strategy 2: keyId contains URL
        if (keyId != null && keyId.contains("http")) {
            String apiUrl = keyId.contains(":http") ? 
                keyId.substring(keyId.indexOf("http")) : keyId;
            Log.d(TAG, "ClearKey: Extracted API from keyId: " + apiUrl);
            String json = fetchClearKeyFromApi(apiUrl);
            if (json != null) return json;
        }
        
        // Strategy 3: Direct hex keyId + key
        if (keyId != null && !keyId.isEmpty() && key != null && !key.isEmpty()) {
            keyId = keyId.replaceAll("[^a-fA-F0-9]", "");
            key = key.replaceAll("[^a-fA-F0-9]", "");
            if (keyId.length() >= 32) keyId = keyId.substring(0, 32);
            if (key.length() >= 32) key = key.substring(0, 32);
            Log.d(TAG, "ClearKey: Building from hex keyId+key");
            return buildClearKeyJson(keyId, key);
        }
        
        // Strategy 4: Combined "keyId:key" format
        if (keyId != null && keyId.contains(":") && !keyId.contains("http")) {
            String[] parts = keyId.split(":");
            if (parts.length >= 2) {
                String kid = parts[0].replaceAll("[^a-fA-F0-9]", "");
                String k = parts[1].replaceAll("[^a-fA-F0-9]", "");
                if (kid.length() >= 32) kid = kid.substring(0, 32);
                if (k.length() >= 32) k = k.substring(0, 32);
                Log.d(TAG, "ClearKey: Building from combined format");
                return buildClearKeyJson(kid, k);
            }
        }
        
        // Strategy 5: License URL with keyid/key params
        if (licenseUrl != null && !licenseUrl.isEmpty()) {
            Log.d(TAG, "ClearKey: Fetching from licenseUrl: " + licenseUrl);
            String json = fetchClearKeyFromApi(licenseUrl);
            if (json != null) return json;
        }
        
        return null;
    }

    private DataSource.Factory buildDataSourceFactory() {
        DefaultHttpDataSource.Factory factory = new DefaultHttpDataSource.Factory();
        
        factory.setConnectTimeoutMs(30000);
        factory.setReadTimeoutMs(30000);
        factory.setAllowCrossProtocolRedirects(true);
        
        if (streamConfig.hasHeaders()) {
            Map<String, String> headers = new HashMap<>();
            
            if (!streamConfig.getUserAgent().isEmpty()) {
                factory.setUserAgent(streamConfig.getUserAgent());
            } else {
                factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
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
        } else {
            factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        }
        
        return factory;
    }

    /**
     * Build MediaSource with optional pre-resolved ClearKey JSON
     */
    private MediaSource buildMediaSource(DataSource.Factory dataSourceFactory, String resolvedClearKeyJson) {
        String url = streamConfig.url;
        Log.d(TAG, "Building media source for: " + url);
        
        Uri uri = Uri.parse(url);
        
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);
        
        // Detect format FIRST - needed for MIME type hint
        String format = detectStreamFormat(url);
        
        // Set MIME type hint to help ExoPlayer with ambiguous URLs
        if ("dash".equals(format)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD);
        } else if ("hls".equals(format)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        
        if (streamConfig.hasDrm() && streamConfig.drm != null) {
            String scheme = streamConfig.drm.scheme != null ? 
                streamConfig.drm.scheme.toLowerCase() : "clearkey";
            
            Log.d(TAG, "Configuring DRM: " + scheme);
            
            MediaItem.DrmConfiguration.Builder drmBuilder;
            
            if ("widevine".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID);
                
                if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
                
                if (streamConfig.hasHeaders()) {
                    Map<String, String> licenseHeaders = new HashMap<>();
                    if (!streamConfig.getUserAgent().isEmpty()) {
                        licenseHeaders.put("User-Agent", streamConfig.getUserAgent());
                    }
                    if (!streamConfig.getOrigin().isEmpty()) {
                        licenseHeaders.put("Origin", streamConfig.getOrigin());
                    }
                    if (!streamConfig.getReferer().isEmpty()) {
                        licenseHeaders.put("Referer", streamConfig.getReferer());
                    }
                    if (!licenseHeaders.isEmpty()) {
                        drmBuilder.setLicenseRequestHeaders(licenseHeaders);
                    }
                }
                
            } else if ("playready".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.PLAYREADY_UUID);
                if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                }
            } else {
                // ClearKey
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID);
                
                if (resolvedClearKeyJson != null) {
                    String dataUri = "data:application/json;base64," + 
                        Base64.encodeToString(resolvedClearKeyJson.getBytes(), Base64.NO_WRAP);
                    drmBuilder.setLicenseUri(dataUri);
                    Log.d(TAG, "ClearKey: Applied pre-resolved JSON");
                } else if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                    Log.d(TAG, "ClearKey: Passing license URL directly as fallback");
                }
            }
            
            mediaItemBuilder.setDrmConfiguration(drmBuilder.build());
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        
        if ("hls".equals(format)) {
            Log.d(TAG, "Building HLS source");
            return new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem);
        } else if ("dash".equals(format)) {
            Log.d(TAG, "Building DASH source");
            return new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        } else {
            Log.d(TAG, "Building Progressive source");
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        }
    }
    
    /**
     * Smart stream format detection that handles complex URLs with tokens,
     * query parameters, fragments, and CDN paths.
     * IMPORTANT: Must handle URLs like:
     *   .../file.mpd?token=abc&params=xyz
     *   .../PLTV/86/224/.../file.mpd?accountinfo=...
     */
    private String detectStreamFormat(String url) {
        if (url == null || url.isEmpty()) return "progressive";
        
        String lower = url.toLowerCase();
        
        // 1. Extract path before query string
        String path = lower;
        int queryIdx = lower.indexOf('?');
        if (queryIdx > 0) {
            path = lower.substring(0, queryIdx);
        }
        int fragIdx = path.indexOf('#');
        if (fragIdx > 0) {
            path = path.substring(0, fragIdx);
        }
        
        // 2. Check path extension directly
        if (path.endsWith(".m3u8")) return "hls";
        if (path.endsWith(".mpd")) return "dash";
        if (path.endsWith(".mp4") || path.endsWith(".mkv") || 
            path.endsWith(".webm") || path.endsWith(".ts")) return "progressive";
        
        // 3. Check for format hints anywhere in the full URL
        if (lower.contains(".m3u8")) return "hls";
        if (lower.contains(".mpd")) return "dash";
        
        // 4. Path-based hints
        if (lower.contains("/hls/") || lower.contains("format=m3u8") || 
            lower.contains("type=hls")) return "hls";
        if (lower.contains("/dash/") || lower.contains("format=mpd") || 
            lower.contains("type=dash") || lower.contains("manifest(format=mpd") || 
            lower.contains("output=mpd") || lower.contains("/pltv/")) return "dash";
        
        // 5. Content-type hints
        if (lower.contains("application/x-mpegurl") || lower.contains("vnd.apple.mpegurl")) return "hls";
        if (lower.contains("application/dash+xml")) return "dash";
        
        return "progressive";
    }
    
    /**
     * Fetch ClearKey JSON from an API endpoint
     */
    private String fetchClearKeyFromApi(String apiUrl) {
        java.net.HttpURLConnection conn = null;
        try {
            Log.d(TAG, "Fetching ClearKey from: " + apiUrl);
            java.net.URL url = new java.net.URL(apiUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setInstanceFollowRedirects(true);
            
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "ClearKey API response code: " + responseCode);
            
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String body = response.toString().trim();
                Log.d(TAG, "ClearKey API response: " + body);
                
                // Already valid ClearKey JSON
                if (body.contains("\"keys\"") && body.contains("\"kty\"")) {
                    return body;
                }
                
                // Try JSON with keyId/key fields
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    
                    String keyId = null;
                    String key = null;
                    
                    String[] keyIdNames = {"keyid", "keyId", "key_id", "kid", "KID"};
                    String[] keyNames = {"key", "Key", "KEY", "k"};
                    
                    for (String name : keyIdNames) {
                        if (json.has(name) && !json.get(name).isJsonNull()) {
                            keyId = json.get(name).getAsString();
                            break;
                        }
                    }
                    for (String name : keyNames) {
                        if (json.has(name) && !json.get(name).isJsonNull() && !name.equals("kid")) {
                            key = json.get(name).getAsString();
                            break;
                        }
                    }
                    
                    if (keyId != null && key != null) {
                        keyId = keyId.replaceAll("[^a-fA-F0-9]", "");
                        key = key.replaceAll("[^a-fA-F0-9]", "");
                        Log.d(TAG, "Parsed API response - keyId: " + keyId.length() + " chars, key: " + key.length() + " chars");
                        return buildClearKeyJson(keyId, key);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "ClearKey API response not JSON: " + e.getMessage());
                }
                
                // Plain text "keyId:key"
                if (body.contains(":") && !body.contains("{")) {
                    String[] parts = body.split(":");
                    if (parts.length == 2) {
                        String kid = parts[0].replaceAll("[^a-fA-F0-9]", "");
                        String k = parts[1].replaceAll("[^a-fA-F0-9]", "");
                        if (kid.length() >= 16 && k.length() >= 16) {
                            return buildClearKeyJson(kid, k);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch ClearKey from API: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private String buildClearKeyJson(String keyId, String key) {
        String keyIdB64 = hexToBase64Url(keyId);
        String keyB64 = hexToBase64Url(key);
        
        return String.format(
            "{\"keys\":[{\"kty\":\"oct\",\"k\":\"%s\",\"kid\":\"%s\"}],\"type\":\"temporary\"}",
            keyB64, keyIdB64
        );
    }

    private String hexToBase64Url(String hex) {
        hex = hex.replaceAll("[:\\s\\-]", "");
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        
        return Base64.encodeToString(data, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
    }

    /** Fully release the player and all associated resources */
    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.release();
            player = null;
        }
        trackSelector = null;
        playerView.setPlayer(null);
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
        releasePlayer();
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
