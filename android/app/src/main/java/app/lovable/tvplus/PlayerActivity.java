package app.lovable.tvplus;

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
 * Features:
 * - HLS, DASH, MP4 support with proper URL handling
 * - Custom headers (User-Agent, Referer, Cookie)
 * - Full Widevine/ClearKey DRM support
 * - Multi-Server selection
 * - Track selection dialog (sorted by resolution)
 * - Chromecast support
 * - Aspect ratio cycling
 * - Picture-in-Picture (Android O+)
 * - TV Remote navigation
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
    private int selectedVideoTrackIndex = -1; // -1 = Auto
    private int selectedAudioTrackIndex = -1; // -1 = Auto
    
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
            Log.d(TAG, "Has servers: " + streamConfig.hasServers());
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
        // Resize button
        ImageButton resizeButton = playerView.findViewById(R.id.exo_resize);
        if (resizeButton != null) {
            resizeButton.setOnClickListener(v -> cycleResizeMode());
        }
        
        // PiP button
        ImageButton pipButton = playerView.findViewById(R.id.exo_pip);
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> enterPiPMode());
        }
        
        // Settings button - Track selection
        ImageButton settingsButton = playerView.findViewById(R.id.exo_settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showTrackSelectionDialog());
        }
        
        // Subtitle button - Show subtitle selection
        ImageButton subtitleButton = playerView.findViewById(R.id.exo_subtitle);
        if (subtitleButton != null) {
            subtitleButton.setOnClickListener(v -> showSubtitleSelectionDialog());
        }
        
        // Server button - Multi-server selection (only show if servers available)
        ImageButton serverButton = playerView.findViewById(R.id.exo_server);
        if (serverButton != null) {
            if (streamConfig.hasServers() && streamConfig.servers.size() > 1) {
                serverButton.setVisibility(View.VISIBLE);
                serverButton.setOnClickListener(v -> showServerSelectionDialog());
            } else {
                serverButton.setVisibility(View.GONE);
            }
        }
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
                    // Disable subtitles
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
    
    /**
     * Cast device selection is handled by the MediaRouteButton (exo_cast).
     * Keeping this method as a safe no-op to avoid build/runtime issues.
     */
    private void showCastDialog() {
        Toast.makeText(this, "Use the Cast button", Toast.LENGTH_SHORT).show();
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
        playerView.setUseController(!isInPictureInPictureMode);
    }

    /**
     * Show Multi-Server Selection Dialog
     */
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
        
        // Populate servers
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
        
        // Reinitialize player with new source
        if (player != null) {
            long position = player.getCurrentPosition();
            player.release();
            initializePlayer();
            player.seekTo(position);
        }
    }

    /**
     * Show Track Selection Dialog with VIDEO/AUDIO tabs
     * Sorted by resolution (highest first)
     */
    private void showTrackSelectionDialog() {
        if (player == null) return;
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_track_selection);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Calculate dialog size - width 60%, height max 70% of screen
        int dialogWidth = (int)(getResources().getDisplayMetrics().widthPixels * 0.6);
        int maxDialogHeight = (int)(getResources().getDisplayMetrics().heightPixels * 0.7);
        
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
        
        // Set indicator width to half
        tabIndicator.post(() -> {
            ViewGroup.LayoutParams params = tabIndicator.getLayoutParams();
            params.width = tabVideo.getWidth();
            tabIndicator.setLayoutParams(params);
        });
        
        // Collect tracks
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
        
        // Sort video tracks by resolution (highest first)
        Collections.sort(videoTracks, (a, b) -> {
            if (b.height != a.height) return b.height - a.height;
            return b.bitrate - a.bitrate;
        });
        
        // Populate video tracks
        // Add "None" option
        RadioButton rbNone = createRadioButton("None", -2, selectedVideoTrackIndex == -2);
        videoRadioGroup.addView(rbNone);
        
        // Add "Auto" option
        RadioButton rbAuto = createRadioButton("Auto", -1, selectedVideoTrackIndex == -1);
        videoRadioGroup.addView(rbAuto);
        
        for (int i = 0; i < videoTracks.size(); i++) {
            TrackInfo track = videoTracks.get(i);
            RadioButton rb = createRadioButton(track.label, i, selectedVideoTrackIndex == i);
            rb.setTag(R.id.tab_video, track);
            videoRadioGroup.addView(rb);
        }
        
        // Populate audio tracks
        RadioButton rbAudioAuto = createRadioButton("Auto", -1, selectedAudioTrackIndex == -1);
        audioRadioGroup.addView(rbAudioAuto);
        
        for (int i = 0; i < audioTracks.size(); i++) {
            TrackInfo track = audioTracks.get(i);
            RadioButton rb = createRadioButton(track.label, i, selectedAudioTrackIndex == i);
            rb.setTag(R.id.tab_audio, track);
            audioRadioGroup.addView(rb);
        }
        
        // Tab switching
        final List<TrackInfo> finalVideoTracks = videoTracks;
        final List<TrackInfo> finalAudioTracks = audioTracks;
        
        tabVideo.setOnClickListener(v -> {
            tabVideo.setTextColor(Color.parseColor("#FFD700"));
            tabAudio.setTextColor(Color.parseColor("#888888"));
            videoContainer.setVisibility(View.VISIBLE);
            audioContainer.setVisibility(View.GONE);
            animateIndicator(tabIndicator, 0);
        });
        
        tabAudio.setOnClickListener(v -> {
            tabAudio.setTextColor(Color.parseColor("#FFD700"));
            tabVideo.setTextColor(Color.parseColor("#888888"));
            audioContainer.setVisibility(View.VISIBLE);
            videoContainer.setVisibility(View.GONE);
            animateIndicator(tabIndicator, tabVideo.getWidth());
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnOk.setOnClickListener(v -> {
            // Apply video track selection
            int videoCheckedId = videoRadioGroup.getCheckedRadioButtonId();
            if (videoCheckedId != -1) {
                RadioButton videoRb = dialog.findViewById(videoCheckedId);
                int index = (int) videoRb.getTag();
                selectedVideoTrackIndex = index;
                
                if (index == -2) {
                    // Disable video
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                            .build()
                    );
                } else if (index == -1) {
                    // Auto
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
            
            // Apply audio track selection
            int audioCheckedId = audioRadioGroup.getCheckedRadioButtonId();
            if (audioCheckedId != -1) {
                RadioButton audioRb = dialog.findViewById(audioCheckedId);
                int index = (int) audioRb.getTag();
                selectedAudioTrackIndex = index;
                
                if (index == -1) {
                    // Auto
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
    
    private RadioButton createRadioButton(String text, int index, boolean checked) {
        RadioButton rb = new RadioButton(this);
        rb.setId(View.generateViewId());
        rb.setText(text);
        rb.setTextColor(index < 0 ? Color.BLACK : Color.parseColor("#666666"));
        rb.setTextSize(16);
        rb.setPadding(16, 20, 16, 20);
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
        // Build data source factory with custom headers
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        
        // Create track selector
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSizeSd()
                .build()
        );
        
        // Build player
        player = new ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build();
        
        playerView.setPlayer(player);
        
        // Build media source with DRM if configured
        MediaSource mediaSource = buildMediaSource(dataSourceFactory);
        
        // Add listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage(), error);
                String errorMsg = "Playback error";
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    errorMsg = "Network error";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    errorMsg = "Stream unavailable";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED) {
                    errorMsg = "DRM license failed";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED) {
                    errorMsg = "DRM not supported";
                }
                Toast.makeText(PlayerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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
        
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
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

    private MediaSource buildMediaSource(DataSource.Factory dataSourceFactory) {
        String url = streamConfig.url;
        Log.d(TAG, "Building media source for: " + url);
        
        Uri uri = Uri.parse(url);
        
        // Build MediaItem with DRM configuration
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);
        
        if (streamConfig.hasDrm() && streamConfig.drm != null) {
            String scheme = streamConfig.drm.scheme != null ? 
                streamConfig.drm.scheme.toLowerCase() : "clearkey";
            
            Log.d(TAG, "Configuring DRM: " + scheme);
            
            MediaItem.DrmConfiguration.Builder drmBuilder;
            
            if ("widevine".equals(scheme)) {
                drmBuilder = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID);
                
                if (streamConfig.drm.licenseUrl != null && !streamConfig.drm.licenseUrl.isEmpty()) {
                    drmBuilder.setLicenseUri(streamConfig.drm.licenseUrl);
                    Log.d(TAG, "Widevine License URL: " + streamConfig.drm.licenseUrl);
                }
                
                // Add license headers if present
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
        
        // Detect format
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
            Log.d(TAG, "Building Progressive source");
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        }
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
