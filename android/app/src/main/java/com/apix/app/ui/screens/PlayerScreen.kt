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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

// ===== Custom Outline Icons =====

private val PlayOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PlayOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 6f); lineTo(8f, 18f); lineTo(18f, 12f); close()
        }
    }.build()
}

private val PauseOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PauseOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Left bar
            moveTo(6f, 7f)
            arcTo(2f, 2f, 0f, false, true, 10f, 7f)
            lineTo(10f, 17f)
            arcTo(2f, 2f, 0f, false, true, 6f, 17f)
            close()
            // Right bar
            moveTo(14f, 7f)
            arcTo(2f, 2f, 0f, false, true, 18f, 7f)
            lineTo(18f, 17f)
            arcTo(2f, 2f, 0f, false, true, 14f, 17f)
            close()
        }
    }.build()
}

private val ForwardOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ForwardOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 7f); lineTo(14f, 12f); lineTo(9f, 17f)
            moveTo(15f, 7f); lineTo(20f, 12f); lineTo(15f, 17f)
        }
    }.build()
}

private val RewindOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RewindOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15f, 7f); lineTo(10f, 12f); lineTo(15f, 17f)
            moveTo(9f, 7f); lineTo(4f, 12f); lineTo(9f, 17f)
        }
    }.build()
}

private val SettingsOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SettingsOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Gear path
            moveTo(19.14f, 12.94f)
            cubicTo(19.18f, 12.63f, 19.2f, 12.31f, 19.2f, 12f)
            cubicTo(19.2f, 11.69f, 19.18f, 11.37f, 19.14f, 11.06f)
            lineTo(21.17f, 9.48f)
            cubicTo(21.35f, 9.34f, 21.4f, 9.07f, 21.29f, 8.87f)
            lineTo(19.37f, 5.55f)
            cubicTo(19.25f, 5.33f, 19f, 5.26f, 18.78f, 5.33f)
            lineTo(16.39f, 6.29f)
            cubicTo(15.89f, 5.91f, 15.36f, 5.59f, 14.77f, 5.35f)
            lineTo(14.41f, 2.81f)
            cubicTo(14.37f, 2.57f, 14.17f, 2.4f, 13.93f, 2.4f)
            lineTo(10.09f, 2.4f)
            cubicTo(9.85f, 2.4f, 9.66f, 2.57f, 9.62f, 2.81f)
            lineTo(9.26f, 5.35f)
            cubicTo(8.67f, 5.59f, 8.13f, 5.92f, 7.64f, 6.29f)
            lineTo(5.25f, 5.33f)
            cubicTo(5.03f, 5.25f, 4.78f, 5.33f, 4.66f, 5.55f)
            lineTo(2.74f, 8.87f)
            cubicTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
            lineTo(4.89f, 11.06f)
            cubicTo(4.85f, 11.37f, 4.81f, 11.69f, 4.81f, 12f)
            cubicTo(4.81f, 12.31f, 4.83f, 12.63f, 4.87f, 12.94f)
            lineTo(2.84f, 14.52f)
            cubicTo(2.66f, 14.66f, 2.61f, 14.93f, 2.73f, 15.13f)
            lineTo(4.65f, 18.45f)
            cubicTo(4.77f, 18.67f, 5.02f, 18.74f, 5.24f, 18.67f)
            lineTo(7.63f, 17.71f)
            cubicTo(8.13f, 18.09f, 8.66f, 18.41f, 9.25f, 18.65f)
            lineTo(9.61f, 21.19f)
            cubicTo(9.66f, 21.43f, 9.85f, 21.6f, 10.09f, 21.6f)
            lineTo(13.93f, 21.6f)
            cubicTo(14.17f, 21.6f, 14.37f, 21.43f, 14.4f, 21.19f)
            lineTo(14.76f, 18.65f)
            cubicTo(15.35f, 18.41f, 15.89f, 18.09f, 16.38f, 17.71f)
            lineTo(18.77f, 18.67f)
            cubicTo(18.99f, 18.75f, 19.24f, 18.67f, 19.36f, 18.45f)
            lineTo(21.28f, 15.13f)
            cubicTo(21.4f, 14.91f, 21.35f, 14.66f, 21.16f, 14.52f)
            lineTo(19.14f, 12.94f)
            close()
            // Inner circle
            moveTo(12f, 15.6f)
            cubicTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f)
            cubicTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
            cubicTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f)
            cubicTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f)
            close()
        }
    }.build()
}

private val PipOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PipOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Main screen
            moveTo(9f, 19f); lineTo(5f, 19f)
            arcTo(2f, 2f, 0f, false, true, 3f, 17f)
            lineTo(3f, 7f)
            arcTo(2f, 2f, 0f, false, true, 5f, 5f)
            lineTo(19f, 5f)
            arcTo(2f, 2f, 0f, false, true, 21f, 7f)
            lineTo(21f, 10f)
            // PiP window
            moveTo(13f, 13f); lineTo(19f, 13f)
            arcTo(2f, 2f, 0f, false, true, 21f, 15f)
            lineTo(21f, 17f)
            arcTo(2f, 2f, 0f, false, true, 19f, 19f)
            lineTo(13f, 19f)
            arcTo(2f, 2f, 0f, false, true, 11f, 17f)
            lineTo(11f, 15f)
            arcTo(2f, 2f, 0f, false, true, 13f, 13f)
            close()
        }
    }.build()
}

private val ResizeOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ResizeOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Expand arrows
            moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f)
            moveTo(9f, 21f); lineTo(3f, 21f); lineTo(3f, 15f)
            moveTo(21f, 3f); lineTo(14f, 10f)
            moveTo(3f, 21f); lineTo(10f, 14f)
        }
    }.build()
}

private val CastOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CastOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 16.1f)
            arcTo(5f, 5f, 0f, false, true, 5.9f, 20f)
            moveTo(2f, 12.05f)
            arcTo(9f, 9f, 0f, false, true, 9.95f, 20f)
            moveTo(2f, 8f)
            arcTo(13f, 13f, 0f, false, true, 14f, 20f)
            moveTo(2f, 20f); lineTo(2.01f, 20f)
            moveTo(20f, 4f); lineTo(4f, 4f)
            moveTo(20f, 4f); lineTo(20f, 20f); lineTo(14f, 20f)
        }
    }.build()
}

private val BackOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BackOutline", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f); lineTo(5f, 12f)
            moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
        }
    }.build()
}

// ===== Main Player Screen =====

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

            // Buffering spinner (center) - Red
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MediumRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(44.dp)
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
                            icon = BackOutlineIcon,
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
                    // Top bar
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
                            icon = BackOutlineIcon,
                            contentDescription = "Back",
                            size = 36,
                            onClick = onBack
                        )
                        Text(
                            text = config.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 300.dp)
                        )
                    }

                    // Bottom section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.8f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Progress bar row: time - slider - time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { player.seekTo((it * duration).toLong()) },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFFE50914),
                                    inactiveTrackColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .focusable()
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Bottom row: left (rewind/play/forward) + right (cast/settings/resize/pip)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerControlButton(
                                    icon = RewindOutlineIcon,
                                    contentDescription = "Rewind",
                                    size = 38,
                                    onClick = { player.seekBack() }
                                )
                                PlayerControlButton(
                                    icon = if (isPlaying) PauseOutlineIcon else PlayOutlineIcon,
                                    contentDescription = "Play/Pause",
                                    size = 44,
                                    onClick = { player.playWhenReady = !player.playWhenReady }
                                )
                                PlayerControlButton(
                                    icon = ForwardOutlineIcon,
                                    contentDescription = "Forward",
                                    size = 38,
                                    onClick = { player.seekForward() }
                                )
                            }

                            // Right controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerControlButton(
                                    icon = CastOutlineIcon,
                                    contentDescription = "Cast",
                                    size = 32
                                ) {}
                                PlayerControlButton(
                                    icon = SettingsOutlineIcon,
                                    contentDescription = "Quality",
                                    size = 32
                                ) {
                                    showTrackDialog = true
                                }
                                PlayerControlButton(
                                    icon = ResizeOutlineIcon,
                                    contentDescription = "Resize",
                                    size = 32
                                ) {
                                    currentResizeMode = (currentResizeMode + 1) % resizeModes.size
                                }
                                PlayerControlButton(
                                    icon = PipOutlineIcon,
                                    contentDescription = "PiP",
                                    size = 32
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

// ===== Track Selection Dialog (Rebuilt - Clean, Sharp) =====

@OptIn(UnstableApi::class)
@Composable
fun TrackSelectionDialog(
    player: ExoPlayer,
    trackSelector: DefaultTrackSelector,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الجودة", "الصوت")

    // Collect video tracks safely
    val videoTracks = remember(player.currentTracks) {
        val tracks = mutableListOf<TrackInfo>()
        tracks.add(TrackInfo("تلقائي", -1, -1, true))
        try {
            player.currentTracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val height = format.height
                        val width = format.width
                        val bitrate = format.bitrate
                        val label = buildString {
                            if (height > 0) {
                                append("${height}p")
                                if (height >= 2160) append(" (4K)")
                                else if (height >= 1440) append(" (2K)")
                                else if (height >= 1080) append(" (FHD)")
                                else if (height >= 720) append(" (HD)")
                            } else append("Track ${i + 1}")
                            if (bitrate > 0) append(" · ${bitrate / 1000}kbps")
                        }
                        tracks.add(TrackInfo(label, groupIndex, i, group.isTrackSelected(i)))
                    }
                }
            }
        } catch (_: Exception) {}
        tracks
    }

    // Collect audio tracks safely
    val audioTracks = remember(player.currentTracks) {
        val tracks = mutableListOf<TrackInfo>()
        try {
            player.currentTracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language ?: "Unknown"
                        val label = format.label ?: lang.uppercase()
                        val bitrate = format.bitrate
                        val displayLabel = if (bitrate > 0) "$label · ${bitrate / 1000}kbps" else label
                        tracks.add(TrackInfo(displayLabel, groupIndex, i, group.isTrackSelected(i)))
                    }
                }
            }
        } catch (_: Exception) {}
        if (tracks.isEmpty()) tracks.add(TrackInfo("افتراضي", -1, -1, true))
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
                .fillMaxWidth(0.45f)
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111))
        ) {
            Column(Modifier.fillMaxSize()) {
                // Tab headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A))
                        .padding(top = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isActive = selectedTab == index
                        val tabInteraction = remember { MutableInteractionSource() }
                        val tabFocused by tabInteraction.collectIsFocusedAsState()

                        Column(
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
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                color = if (isActive) Gold else Color(0xFF888888),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isActive) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .width(32.dp)
                                        .height(2.dp)
                                        .background(Gold, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)

                // Track list
                val currentTracks = if (selectedTab == 0) videoTracks else audioTracks
                val currentSelected = if (selectedTab == 0) selectedVideoIndex else selectedAudioIndex

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(currentTracks) { index, track ->
                        val itemInteraction = remember { MutableInteractionSource() }
                        val itemFocused by itemInteraction.collectIsFocusedAsState()
                        val isItemSelected = index == currentSelected

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isItemSelected -> Color(0xFF2A2A2A)
                                        itemFocused -> Color(0xFF1E1E1E)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (itemFocused) Modifier.border(1.5.dp, Gold, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = null
                                ) {
                                    try {
                                        if (selectedTab == 0) {
                                            selectedVideoIndex = index
                                            applyVideoTrackSafe(trackSelector, track, player)
                                        } else {
                                            selectedAudioIndex = index
                                            applyAudioTrackSafe(trackSelector, track, player)
                                        }
                                    } catch (_: Exception) {}
                                }
                                .focusable(interactionSource = itemInteraction)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = track.label,
                                color = if (isItemSelected) Gold else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isItemSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isItemSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Gold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Close button
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                val closeInteraction = remember { MutableInteractionSource() }
                val closeFocused by closeInteraction.collectIsFocusedAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (closeFocused) Modifier.border(1.5.dp, Gold, RoundedCornerShape(4.dp))
                            else Modifier
                        )
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null
                        ) { onDismiss() }
                        .focusable(interactionSource = closeInteraction)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("إغلاق", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

// ===== Safe Track Selection (no crash) =====

@OptIn(UnstableApi::class)
private fun applyVideoTrackSafe(trackSelector: DefaultTrackSelector, track: TrackInfo, player: ExoPlayer) {
    try {
        if (track.groupIndex == -1) {
            // Auto mode
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearVideoSizeConstraints()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    .build()
            )
            return
        }

        // Use currentTracks to get the correct TrackGroup
        val groups = player.currentTracks.groups
        var videoGroupCount = 0
        for (group in groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                if (videoGroupCount == 0) {
                    // Use the first video group's mediaTrackGroup
                    val override = TrackSelectionOverride(
                        group.mediaTrackGroup,
                        listOf(track.trackIndex)
                    )
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .setOverrideForType(override)
                            .build()
                    )
                    return
                }
                videoGroupCount++
            }
        }
    } catch (e: Exception) {
        Log.e("PlayerScreen", "Error applying video track", e)
    }
}

@OptIn(UnstableApi::class)
private fun applyAudioTrackSafe(trackSelector: DefaultTrackSelector, track: TrackInfo, player: ExoPlayer) {
    try {
        if (track.groupIndex == -1) return

        val groups = player.currentTracks.groups
        var audioGroupIndex = 0
        for (group in groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                if (audioGroupIndex == track.groupIndex) {
                    val override = TrackSelectionOverride(
                        group.mediaTrackGroup,
                        listOf(track.trackIndex)
                    )
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setOverrideForType(override)
                            .build()
                    )
                    return
                }
                audioGroupIndex++
            }
        }
    } catch (e: Exception) {
        Log.e("PlayerScreen", "Error applying audio track", e)
    }
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
