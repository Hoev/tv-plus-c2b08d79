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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
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

    val resizeModes = remember {
        intArrayOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
    }

    val player = remember {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd().build())
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    // Initialize player
    LaunchedEffect(config) {
        try {
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
                    (player.trackSelector as? DefaultTrackSelector)?.setParameters(
                        (player.trackSelector as DefaultTrackSelector)
                            .buildUponParameters().clearVideoSizeConstraints().build()
                    )
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

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
            delay(3000)
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
                        this.player = player@this@LaunchedEffect
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

            // Buffering spinner
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
                        PlayerIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "إغلاق",
                            onClick = onBack
                        )
                    }
                }
            }

            // Controls overlay
            if (showControls && errorMessage == null) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Text(
                        text = config.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIconButton(
                        icon = Icons.Default.FastRewind,
                        contentDescription = "Rewind",
                        size = 48,
                        onClick = { player.seekBack() }
                    )
                    PlayerIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        size = 64,
                        onClick = { player.playWhenReady = !player.playWhenReady }
                    )
                    PlayerIconButton(
                        icon = Icons.Default.FastForward,
                        contentDescription = "Forward",
                        size = 48,
                        onClick = { player.seekForward() }
                    )
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Progress bar
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { player.seekTo((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MediumRed,
                            inactiveTrackColor = Color(0xFF444444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Time + icons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time left
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color(0xAAFFFFFF),
                            fontSize = 13.sp
                        )

                        // Right icons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PlayerIconButton(Icons.Default.Cast, "Cast", size = 36) {}
                            PlayerIconButton(Icons.Default.VolumeUp, "Volume", size = 36) {
                                player.volume = if (player.volume > 0f) 0f else 1f
                            }
                            PlayerIconButton(Icons.Default.Settings, "Settings", size = 36) {
                                // Track selection handled by existing Java dialog logic
                            }
                            PlayerIconButton(Icons.Default.Fullscreen, "Resize", size = 36) {
                                currentResizeMode = (currentResizeMode + 1) % resizeModes.size
                            }
                            PlayerIconButton(Icons.Default.PictureInPicture, "PiP", size = 36) {
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
    }
}

@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int = 44,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "playerBtnScale")

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
            modifier = Modifier.size((size * 0.6f).dp)
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

    // Strategy 1: License URL API
    if (!licenseUrl.isNullOrEmpty() && licenseUrl.startsWith("http") && keyId.isNullOrEmpty()) {
        fetchClearKeyFromApi(licenseUrl)?.let { return it }
    }

    // Strategy 2: keyId contains URL
    if (!keyId.isNullOrEmpty() && keyId.contains("http")) {
        val apiUrl = if (keyId.contains(":http")) keyId.substring(keyId.indexOf("http")) else keyId
        fetchClearKeyFromApi(apiUrl)?.let { return it }
    }

    // Strategy 3: Direct hex
    if (!keyId.isNullOrEmpty() && !key.isNullOrEmpty()) {
        val cleanKid = keyId.replace(Regex("[^a-fA-F0-9]"), "").take(32)
        val cleanKey = key.replace(Regex("[^a-fA-F0-9]"), "").take(32)
        return buildClearKeyJson(cleanKid, cleanKey)
    }

    // Strategy 4: Combined
    if (!keyId.isNullOrEmpty() && keyId.contains(":") && !keyId.contains("http")) {
        val parts = keyId.split(":")
        if (parts.size >= 2) {
            val kid = parts[0].replace(Regex("[^a-fA-F0-9]"), "").take(32)
            val k = parts[1].replace(Regex("[^a-fA-F0-9]"), "").take(32)
            return buildClearKeyJson(kid, k)
        }
    }

    // Strategy 5: License URL fallback
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
    } finally {
        conn?.disconnect()
    }
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
