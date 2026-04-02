package com.apix.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.apix.app.data.PlayerConfig
import com.apix.app.ui.theme.Gold
import com.apix.app.ui.theme.MediumRed
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    config: PlayerConfig,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentResizeMode by remember { mutableIntStateOf(0) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val resizeModes = remember {
        intArrayOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val player = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    // Initialize player
    LaunchedEffect(config) {
        try {
            player.stop()
            player.clearMediaItems()
            val dataSourceFactory = buildDataSourceFactory(config)
            val resolvedClearKey = if (config.drm != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    resolveClearKey(config)
                }
            } else null

            val mediaSource = buildMediaSource(config, dataSourceFactory, resolvedClearKey)
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            errorMessage = "فشل تشغيل البث: ${e.message}"
        }
    }

    // Player listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters().clearVideoSizeConstraints().build()
                    )
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "خطأ في الشبكة"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "البث غير متاح"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "تنسيق غير صالح"
                    else -> "خطأ في التشغيل"
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    // Position update
    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Force LTR for player
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            // ExoPlayer surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeModes[currentResizeMode]
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering spinner (center)
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MediumRed,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Error overlay
            errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = MediumRed, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(err, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        PlayerControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "إغلاق",
                            onClick = onBack
                        )
                    }
                }
            }

            // Controls overlay
            AnimatedVisibility(
                visible = showControls && errorMessage == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Top bar - back (left) + channel name (right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Black.copy(0.7f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            size = 40,
                            onClick = onBack
                        )
                        Text(
                            text = config.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom section - progress bar + controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.7f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Progress bar with time on sides
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.width(50.dp)
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { player.seekTo((it * duration).toLong()) },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MediumRed,
                                    inactiveTrackColor = Color(0xFF555555)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.width(50.dp)
                            )
                        }

                        // Bottom row: left (rewind/play/forward) + right (cast/volume/quality/resize/pip)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerControlButton(
                                    icon = Icons.Default.FastRewind,
                                    contentDescription = "Rewind",
                                    size = 40,
                                    onClick = { player.seekBack() }
                                )
                                PlayerControlButton(
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    size = 48,
                                    onClick = { player.playWhenReady = !player.playWhenReady }
                                )
                                PlayerControlButton(
                                    icon = Icons.Default.FastForward,
                                    contentDescription = "Forward",
                                    size = 40,
                                    onClick = { player.seekForward() }
                                )
                            }

                            // Right controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerControlButton(
                                    icon = Icons.Default.Cast,
                                    contentDescription = "Cast",
                                    size = 36
                                ) {}
                                PlayerControlButton(
                                    icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    size = 36
                                ) {
                                    isMuted = !isMuted
                                    player.volume = if (isMuted) 0f else 1f
                                }
                                PlayerControlButton(
                                    icon = Icons.Default.Settings,
                                    contentDescription = "Quality",
                                    size = 36
                                ) {
                                    showTrackDialog = true
                                }
                                PlayerControlButton(
                                    icon = Icons.Default.FitScreen,
                                    contentDescription = "Resize",
                                    size = 36
                                ) {
                                    currentResizeMode = (currentResizeMode + 1) % resizeModes.size
                                }
                                PlayerControlButton(
                                    icon = Icons.Default.PictureInPicture,
                                    contentDescription = "PiP",
                                    size = 36
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                        try {
                                            activity.enterPictureInPictureMode(
                                                PictureInPictureParams.Builder()
                                                    .setAspectRatio(Rational(16, 9))
                                                    .build()
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Track selection dialog
            if (showTrackDialog) {
                TrackSelectionDialog(
                    player = player,
                    trackSelector = trackSelector,
                    onDismiss = { showTrackDialog = false }
                )
            }
        }
    }
}

// ===== Track Selection Dialog =====

@OptIn(UnstableApi::class)
@Composable
fun TrackSelectionDialog(
    player: ExoPlayer,
    trackSelector: DefaultTrackSelector,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Video, 1 = Audio
    val tabs = listOf("الجودة", "الصوت")

    // Collect video tracks
    val videoTracks = remember(player.currentTracks) {
        val tracks = mutableListOf<TrackInfo>()
        tracks.add(TrackInfo("Auto", -1, -1, true))
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val height = format.height
                    val label = if (height > 0) "${height}p" else "Track ${i + 1}"
                    tracks.add(TrackInfo(label, groupIndex, i, group.isTrackSelected(i)))
                }
            }
        }
        tracks
    }

    // Collect audio tracks
    val audioTracks = remember(player.currentTracks) {
        val tracks = mutableListOf<TrackInfo>()
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language ?: "Unknown"
                    val label = format.label ?: lang.uppercase()
                    tracks.add(TrackInfo(label, groupIndex, i, group.isTrackSelected(i)))
                }
            }
        }
        if (tracks.isEmpty()) tracks.add(TrackInfo("Default", -1, -1, true))
        tracks
    }

    var selectedVideoIndex by remember {
        mutableIntStateOf(videoTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0))
    }
    var selectedAudioIndex by remember {
        mutableIntStateOf(audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            Column(Modifier.fillMaxSize()) {
                // Tab headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isActive = selectedTab == index
                        val tabInteraction = remember { MutableInteractionSource() }
                        val tabFocused by tabInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = tabInteraction,
                                    indication = null
                                ) { selectedTab = index }
                                .focusable(interactionSource = tabInteraction)
                                .then(
                                    if (tabFocused) Modifier.border(2.dp, Gold, RoundedCornerShape(4.dp))
                                    else Modifier
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    color = if (isActive) Gold else Color(0xFF888888),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isActive) {
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier
                                            .width(40.dp)
                                            .height(3.dp)
                                            .background(Gold, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF333333), thickness = 1.dp)

                // Track list
                val currentTracks = if (selectedTab == 0) videoTracks else audioTracks
                val currentSelected = if (selectedTab == 0) selectedVideoIndex else selectedAudioIndex

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    itemsIndexed(currentTracks) { index, track ->
                        val itemInteraction = remember { MutableInteractionSource() }
                        val itemFocused by itemInteraction.collectIsFocusedAsState()
                        val isItemSelected = index == currentSelected

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isItemSelected -> Color(0xFF333333)
                                        itemFocused -> Color(0xFF2A2A2A)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (itemFocused) Modifier.border(2.dp, Gold, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = null
                                ) {
                                    if (selectedTab == 0) {
                                        selectedVideoIndex = index
                                        applyVideoTrack(trackSelector, track)
                                    } else {
                                        selectedAudioIndex = index
                                        applyAudioTrack(trackSelector, track, player)
                                    }
                                }
                                .focusable(interactionSource = itemInteraction)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    color = if (isItemSelected) Gold else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isItemSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isItemSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Gold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Close button
                Divider(color = Color(0xFF333333), thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("إغلاق", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class TrackInfo(
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
private fun applyVideoTrack(trackSelector: DefaultTrackSelector, track: TrackInfo) {
    if (track.groupIndex == -1) {
        // Auto
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearVideoSizeConstraints()
                .setMaxVideoSizeSd() // let player decide
                .clearVideoSizeConstraints()
                .build()
        )
    } else {
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearVideoSizeConstraints()
                .setOverrideForType(
                    TrackSelectionOverride(
                        trackSelector.currentMappedTrackInfo!!.getTrackGroups(C.TRACK_TYPE_VIDEO)
                            .get(0),
                        listOf(track.trackIndex)
                    )
                )
                .build()
        )
    }
}

@OptIn(UnstableApi::class)
private fun applyAudioTrack(trackSelector: DefaultTrackSelector, track: TrackInfo, player: ExoPlayer) {
    if (track.groupIndex == -1) return
    trackSelector.setParameters(
        trackSelector.buildUponParameters()
            .setOverrideForType(
                TrackSelectionOverride(
                    trackSelector.currentMappedTrackInfo!!.getTrackGroups(C.TRACK_TYPE_AUDIO)
                        .get(track.groupIndex),
                    listOf(track.trackIndex)
                )
            )
            .build()
    )
}

// ===== Player Control Button =====

@Composable
fun PlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int = 44,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHighlighted = isFocused || isPressed
    val scale by animateFloatAsState(if (isHighlighted) 1.2f else 1f, label = "playerBtnScale")

    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .then(
                if (isFocused) Modifier.border(2.dp, Gold, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size((size * 0.65f).dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}

// ===== Player utilities =====

@OptIn(UnstableApi::class)
private fun buildDataSourceFactory(config: PlayerConfig): DefaultHttpDataSource.Factory {
    val factory = DefaultHttpDataSource.Factory()
    factory.setConnectTimeoutMs(30000)
    factory.setReadTimeoutMs(30000)
    factory.setAllowCrossProtocolRedirects(true)

    val headers = mutableMapOf<String, String>()
    config.headers?.let { h ->
        h.userAgent?.let { factory.setUserAgent(it) }
            ?: factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        h.referer?.let { headers["Referer"] = it }
        h.cookie?.let { headers["Cookie"] = it }
        h.origin?.let { headers["Origin"] = it }
    } ?: factory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

    if (headers.isNotEmpty()) factory.setDefaultRequestProperties(headers)
    return factory
}

@OptIn(UnstableApi::class)
private fun buildMediaSource(
    config: PlayerConfig,
    factory: DefaultHttpDataSource.Factory,
    clearKeyJson: String?
): MediaSource {
    val uri = Uri.parse(config.url)
    val builder = MediaItem.Builder().setUri(uri)
    val format = detectStreamFormat(config.url)

    when (format) {
        "dash" -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        "hls" -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }

    config.drm?.let { drm ->
        val scheme = drm.scheme?.lowercase() ?: "clearkey"
        val drmBuilder = when (scheme) {
            "widevine" -> MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).apply {
                drm.licenseUrl?.let { setLicenseUri(it) }
            }
            "playready" -> MediaItem.DrmConfiguration.Builder(C.PLAYREADY_UUID).apply {
                drm.licenseUrl?.let { setLicenseUri(it) }
            }
            else -> MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).apply {
                if (clearKeyJson != null) {
                    val dataUri = "data:application/json;base64," +
                            Base64.encodeToString(clearKeyJson.toByteArray(), Base64.NO_WRAP)
                    setLicenseUri(dataUri)
                } else drm.licenseUrl?.let { setLicenseUri(it) }
            }
        }
        builder.setDrmConfiguration(drmBuilder.build())
    }

    val mediaItem = builder.build()
    return when (format) {
        "hls" -> HlsMediaSource.Factory(factory).setAllowChunklessPreparation(true).createMediaSource(mediaItem)
        "dash" -> DashMediaSource.Factory(factory).createMediaSource(mediaItem)
        else -> ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
    }
}

private fun detectStreamFormat(url: String): String {
    val lower = url.lowercase()
    val path = lower.substringBefore("?").substringBefore("#")

    return when {
        path.endsWith(".m3u8") -> "hls"
        path.endsWith(".mpd") -> "dash"
        lower.contains(".m3u8") -> "hls"
        lower.contains(".mpd") -> "dash"
        lower.contains("/hls/") || lower.contains("format=m3u8") -> "hls"
        lower.contains("/dash/") || lower.contains("format=mpd") ||
                lower.contains("/pltv/") || lower.contains("manifest(format=mpd") -> "dash"
        else -> "progressive"
    }
}

private fun resolveClearKey(config: PlayerConfig): String? {
    val drm = config.drm ?: return null
    val keyId = drm.keyId
    val key = drm.key
    val licenseUrl = drm.licenseUrl

    if (!licenseUrl.isNullOrEmpty() && licenseUrl.startsWith("http") && keyId.isNullOrEmpty()) {
        fetchClearKeyFromApi(licenseUrl)?.let { return it }
    }
    if (!keyId.isNullOrEmpty() && keyId.contains("http")) {
        val apiUrl = if (keyId.contains(":http")) keyId.substring(keyId.indexOf("http")) else keyId
        fetchClearKeyFromApi(apiUrl)?.let { return it }
    }
    if (!keyId.isNullOrEmpty() && !key.isNullOrEmpty()) {
        val cleanKid = keyId.replace(Regex("[^a-fA-F0-9]"), "").take(32)
        val cleanKey = key.replace(Regex("[^a-fA-F0-9]"), "").take(32)
        return buildClearKeyJson(cleanKid, cleanKey)
    }
    if (!keyId.isNullOrEmpty() && keyId.contains(":") && !keyId.contains("http")) {
        val parts = keyId.split(":")
        if (parts.size >= 2) {
            val kid = parts[0].replace(Regex("[^a-fA-F0-9]"), "").take(32)
            val k = parts[1].replace(Regex("[^a-fA-F0-9]"), "").take(32)
            return buildClearKeyJson(kid, k)
        }
    }
    if (!licenseUrl.isNullOrEmpty()) {
        fetchClearKeyFromApi(licenseUrl)?.let { return it }
    }
    return null
}

private fun fetchClearKeyFromApi(apiUrl: String): String? {
    var conn: HttpURLConnection? = null
    try {
        conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.instanceFollowRedirects = true

        if (conn.responseCode == 200) {
            val body = BufferedReader(InputStreamReader(conn.inputStream)).readText().trim()
            if (body.contains("\"keys\"") && body.contains("\"kty\"")) return body
            try {
                val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                val keyIdNames = arrayOf("keyid", "keyId", "key_id", "kid", "KID")
                val keyNames = arrayOf("key", "Key", "KEY", "k")
                var kid: String? = null
                var k: String? = null
                for (n in keyIdNames) if (json.has(n)) { kid = json[n].asString; break }
                for (n in keyNames) if (json.has(n) && n != "kid") { k = json[n].asString; break }
                if (kid != null && k != null) {
                    return buildClearKeyJson(
                        kid.replace(Regex("[^a-fA-F0-9]"), ""),
                        k.replace(Regex("[^a-fA-F0-9]"), "")
                    )
                }
            } catch (_: Exception) {}
            if (body.contains(":") && !body.contains("{")) {
                val parts = body.split(":")
                if (parts.size == 2) {
                    val kid = parts[0].replace(Regex("[^a-fA-F0-9]"), "")
                    val k = parts[1].replace(Regex("[^a-fA-F0-9]"), "")
                    if (kid.length >= 16 && k.length >= 16) return buildClearKeyJson(kid, k)
                }
            }
        }
    } catch (_: Exception) {
    } finally { conn?.disconnect() }
    return null
}

private fun buildClearKeyJson(keyId: String, key: String): String {
    val keyIdB64 = hexToBase64Url(keyId)
    val keyB64 = hexToBase64Url(key)
    return """{"keys":[{"kty":"oct","k":"$keyB64","kid":"$keyIdB64"}],"type":"temporary"}"""
}

private fun hexToBase64Url(hex: String): String {
    val clean = hex.replace(Regex("[:\\s-]"), "")
    val data = ByteArray(clean.length / 2) { i ->
        ((Character.digit(clean[i * 2], 16) shl 4) + Character.digit(clean[i * 2 + 1], 16)).toByte()
    }
    return Base64.encodeToString(data, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
}
