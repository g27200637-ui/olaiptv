package com.example.feature_player

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.XtreamStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    stream: XtreamStream,
    type: String = "live",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val streamUrl by viewModel.streamUrl.collectAsState()
    val reconnectTrigger by viewModel.reconnectTrigger.collectAsState()
    val playlistName = remember { viewModel.getPlaylistName() }

    // Core Player States
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reconnectAttempts by remember { mutableStateOf(0) }
    
    // Auto-Hide HUD State
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Position Tracking
    var currentPosition by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Continue Watching Progress States
    var dbResumePosition by remember { mutableStateOf(0L) }
    var showResumePrompt by remember { mutableStateOf(false) }
    var resumeDecisionMade by remember { mutableStateOf(false) }

    // Advanced Selections (Default configurations)
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    var selectedSubtitle by remember { mutableStateOf("Off") }
    var subtitleSize by remember { mutableStateOf("Medium") }
    var subtitleColor by remember { mutableStateOf("White") }
    var videoQuality by remember { mutableStateOf("Auto") }
    var aspectOption by remember { mutableStateOf("Fit") }
    var selectedAudioTrack by remember { mutableStateOf("English [Original]") }

    // Configurable Skip Margins (Manual Settings)
    var introDurationConfig by remember { mutableStateOf(90000L) } // Default 90 seconds
    var creditsDurationConfig by remember { mutableStateOf(120000L) } // Default 2 minutes before end

    // Storyboard Timeline Seeker Preview
    var isScrubSeekShowing by remember { mutableStateOf(false) }
    var tempScrubPosition by remember { mutableStateOf(0L) }

    // Settings Sidebar Selector
    var showSettingsDrawer by remember { mutableStateOf(false) }

    // Simulated Metadata Speed and Subtitles
    var liveNetworkSpeed by remember { mutableStateOf("26.8 Mbps") }

    // Key event tracking for long D-pad presses (Accelerated Continuous Seek)
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var activeHeldKey by remember { mutableStateOf<Key?>(null) }

    val mainScreenFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val settingsFirstItemFocusRequester = remember { FocusRequester() }
    val skipIntroFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = true) {
        if (showSettingsDrawer) {
            showSettingsDrawer = false
        } else if (showControls) {
            showControls = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(100)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures during layout switches
            }
        } else {
            try {
                mainScreenFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(showSettingsDrawer) {
        if (showSettingsDrawer) {
            delay(100)
            try {
                settingsFirstItemFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore
            }
        } else if (showControls) {
            delay(100)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Base ExoPlayer initialization with premium safety
    val exoPlayer = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    // Trigger loading of media stream URL
    LaunchedEffect(stream.id, type) {
        viewModel.loadStream(stream.id, type, stream.containerExtension)
    }

    // Load DB Continue Watching progression on launch
    LaunchedEffect(stream.id) {
        if (type == "movies" || type == "series") {
            val progress = viewModel.getPlaybackProgress(stream.id)
            if (progress != null && progress.positionMs > 5000L) {
                dbResumePosition = progress.positionMs
                showResumePrompt = true
            } else {
                resumeDecisionMade = true
            }
        } else {
            resumeDecisionMade = true
        }
    }

    // Prepare stream and start play cycle when everything is checked
    LaunchedEffect(streamUrl, reconnectTrigger, resumeDecisionMade) {
        if (streamUrl.isNotBlank() && resumeDecisionMade) {
            errorMessage = null
            isBuffering = true

            val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
            if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            } else if (streamUrl.contains(".mp4", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
            }
            val mediaItem = mediaItemBuilder.build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Periodic position checker loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                bufferedPosition = exoPlayer.bufferedPosition
                duration = exoPlayer.duration
                delay(250)
            }
        }
    }

    // Dynamic Playback speed controller binding
    LaunchedEffect(selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    // Periodic network speed fluctuation simulator
    LaunchedEffect(isPlaying) {
        while (true) {
            delay(2000)
            val randomSpeed = (220 + (System.currentTimeMillis() % 120) - 50) / 10.0
            liveNetworkSpeed = "%.1f Mbps".format(randomSpeed)
        }
    }

    // Continuous automatic database progress preservation
    LaunchedEffect(Unit) {
        viewModel.savePlaybackProgress(
            id = stream.id,
            name = stream.name,
            streamIcon = stream.streamIcon,
            type = type,
            containerExtension = stream.containerExtension,
            positionMs = 1L,
            durationMs = 0L
        )
    }

    // Continuous automatic database progress preservation
    LaunchedEffect(isPlaying) {
        if (isPlaying && (type == "movies" || type == "series")) {
            while (true) {
                delay(5000)
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0 && pos > 0) {
                    viewModel.savePlaybackProgress(
                        id = stream.id,
                        name = stream.name,
                        streamIcon = stream.streamIcon,
                        type = type,
                        containerExtension = stream.containerExtension,
                        positionMs = pos,
                        durationMs = dur
                    )
                }
            }
        }
    }

    // Autohide controller overlay triggered after 3 seconds of peace
    LaunchedEffect(showControls, isPlaying, lastInteractionTime) {
        if (showControls && isPlaying && !showSettingsDrawer) {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - lastInteractionTime
                if (elapsed >= 3000L) {
                    showControls = false
                    isScrubSeekShowing = false
                    break
                }
            }
        }
    }

    // Lifecycle observer for pause/release sync
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val pos = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    if (dur > 0 && pos > 0 && (type == "movies" || type == "series")) {
                        viewModel.savePlaybackProgress(
                            id = stream.id,
                            name = stream.name,
                            streamIcon = stream.streamIcon,
                            type = type,
                            containerExtension = stream.containerExtension,
                            positionMs = pos,
                            durationMs = dur
                        )
                    }
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (streamUrl.isNotBlank() && errorMessage == null && resumeDecisionMade) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Player Core Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isBuffering = false
                    errorMessage = null
                    reconnectAttempts = 0
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        reconnectAttempts = 0
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        if (type == "movies" || type == "series") {
                            viewModel.deletePlaybackProgress(stream.id)
                        }
                    }
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Stream disconnected (${error.errorCodeName}). Reconnecting..."
                if (reconnectAttempts < 5) {
                    scope.launch {
                        isBuffering = true
                        delay(3500)
                        reconnectAttempts++
                        viewModel.triggerReconnect()
                    }
                } else {
                    errorMessage = "Streaming link failed. Please sync with your iptv server."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Time Formatting Utility
    fun formatMs(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSecs = ms / 1000
        val secs = totalSecs % 60
        val totalMins = totalSecs / 60
        val mins = totalMins % 60
        val hours = totalMins / 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    // Helpers to parse next episode details
    fun produceNextEpisodeObject(currentStream: XtreamStream): XtreamStream {
        val nameToParse = currentStream.name
        val regex = Regex("E(\\d+)", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(nameToParse)
        val nextName = if (matchResult != null) {
            val epNumStr = matchResult.groupValues[1]
            val epNumber = (epNumStr.toIntOrNull() ?: 1) + 1
            val paddedNextEp = "%0${epNumStr.length}d".format(epNumber)
            nameToParse.replaceRange(matchResult.range.first + 1, matchResult.range.last + 1, paddedNextEp)
        } else {
            "$nameToParse [Season Next Episode]"
        }
        return currentStream.copy(
            streamId = currentStream.streamId + 1,
            name = nextName
        )
    }

    // Segment Conditions
    val isIntroSegmentActive = type == "series" && duration > 0 && currentPosition < introDurationConfig
    val isCreditsSegmentActive = type != "live" && duration > 0 && currentPosition >= (duration - creditsDurationConfig)

    // Automatically focus on Skip Intro when starting playback if it is present and active
    LaunchedEffect(isIntroSegmentActive) {
        if (isIntroSegmentActive) {
            delay(800) // Give short window for media load and UI layout transition to settle
            try {
                skipIntroFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus failures
            }
        }
    }

    // Subtitle Line Mock Generator
    val dialogSubtitleLines = remember {
        listOf(
            "For centuries, our home systems remained peaceful.",
            "But a massive subspace wave has altered the coordinates.",
            "Initializing standard frequency diagnostic relays.",
            "The network path is synchronized. Fast bandwidth locks detected.",
            "Remember, we must secure the connection prior to transition.",
            "All video streams are streaming safely to home screens.",
            "There is no alternative. Prepare to skip direct boundaries."
        )
    }
    val currentActiveSimSub = if (selectedSubtitle != "Off" && duration > 0) {
        val lineIndex = ((currentPosition / 5500) % dialogSubtitleLines.size).toInt()
        dialogSubtitleLines[lineIndex]
    } else ""

    // Keyboard navigation and continuous seeking handler on main container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(mainScreenFocusRequester)
            .focusable(enabled = !showControls)
            .onKeyEvent { keyEvent ->
                val isKeyDown = keyEvent.type == KeyEventType.KeyDown
                val isKeyUp = keyEvent.type == KeyEventType.KeyUp

                // Always handle key release first to prevent stuck states
                if (isKeyUp && activeHeldKey != null && keyEvent.key == activeHeldKey) {
                    activeHeldKey = null
                    seekJob?.cancel()
                    seekJob = null

                    if (isScrubSeekShowing) {
                        exoPlayer.seekTo(tempScrubPosition)
                        isScrubSeekShowing = false
                    } else {
                        val target = if (keyEvent.key == Key.DirectionLeft) {
                            (currentPosition - 10000L).coerceAtLeast(0L)
                        } else {
                            (currentPosition + 10000L).coerceAtMost(duration)
                        }
                        exoPlayer.seekTo(target)
                    }
                    showControls = true
                    lastInteractionTime = System.currentTimeMillis()
                    return@onKeyEvent true
                }

                if (!showControls) {
                    if (isKeyDown) {
                        lastInteractionTime = System.currentTimeMillis()

                        if (activeHeldKey == null && (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionRight)) {
                            activeHeldKey = keyEvent.key
                            isScrubSeekShowing = false
                            tempScrubPosition = currentPosition

                            seekJob?.cancel()
                            seekJob = scope.launch {
                                delay(350)
                                isScrubSeekShowing = true
                                var scrubberStep = 5000L
                                while (true) {
                                    val jump = if (keyEvent.key == Key.DirectionLeft) -scrubberStep else scrubberStep
                                    tempScrubPosition = (tempScrubPosition + jump).coerceIn(0L, duration)
                                    exoPlayer.seekTo(tempScrubPosition)
                                    delay(120)
                                    scrubberStep = (scrubberStep + 1000L).coerceAtMost(25000L)
                                }
                            }
                            return@onKeyEvent true
                        } else if (keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown || keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
                            showControls = true
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    lastInteractionTime = System.currentTimeMillis()
                    showControls = !showControls
                }
            )
    ) {
        // ExoPlayer Canvas Surface Interop view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (aspectOption) {
                    "Stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    "Zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "16:9" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    "4:3" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // General Vignette shadow backdrop for cinema look
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        radius = 2000f
                    )
                )
        )

        // Custom Simulated Dialog Subtitles Overlay (bottom aligned with responsive settings values)
        if (currentActiveSimSub.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showControls) 155.dp else 45.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val fontSizeVal = when (subtitleSize) {
                    "Small" -> 14.sp
                    "Large" -> 23.sp
                    "Extra Large" -> 28.sp
                    else -> 18.sp // Medium
                }
                val fontColorVal = when (subtitleColor) {
                    "Yellow" -> Color(0xFFFFD700)
                    "Green" -> Color(0xFF22C55E)
                    "Cyan" -> Color(0xFF06B6D4)
                    else -> Color.White
                }

                Text(
                    text = currentActiveSimSub,
                    color = fontColorVal,
                    fontSize = fontSizeVal,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .widthIn(max = 620.dp)
                )
            }
        }

        // Live Loader Buffer Activity Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFFE50914), // Netflix Red
                        strokeWidth = 4.5.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Buffering digital streams...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Animated Interactive Dashboard HUD HUD Panel (Netflix UI style)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 42.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Playback Information Overlays
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stream.name,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (type == "series") {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE50914).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "S01 | EP0${if (stream.streamId > 0) (stream.streamId % 5 + 1) else 1}",
                                        color = Color(0xFFE50914),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Code details metadata bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dynamic Live Speed Track badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = liveNetworkSpeed,
                                    color = Color(0xFF22C55E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text("•", color = Color.White.copy(alpha = 0.2f))
                            Text("Audio: ${selectedAudioTrack}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("•", color = Color.White.copy(alpha = 0.2f))
                            Text("Subtitles: ${selectedSubtitle}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                            // UHD Formats Badges row
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = if (videoQuality == "4K") "4K UHD" else "1080P", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = "HDR10+", color = Color(0xFFFFBF00), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Right Side: Back Actions button
                    val topBackInteraction = remember { MutableInteractionSource() }
                    val isTopBackFocused by topBackInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isTopBackFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = topBackInteraction,
                                indication = LocalIndication.current,
                                onClick = { onBack() }
                            )
                            .focusable(interactionSource = topBackInteraction)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Timeline visual storyboard thumbnail preview card
        if (isScrubSeekShowing && type != "live") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140D10)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color(0xFFE50914)),
                    modifier = Modifier.width(280.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                        ) {
                            if (!stream.streamIcon.isNullOrBlank()) {
                                AsyncImage(
                                    model = stream.streamIcon,
                                    contentDescription = "Scrub position outline",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFFE50914).copy(alpha = 0.2f), Color(0xFF140D10))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                                }
                            }
                            // Seeker direction pointers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("--", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                                Text("--", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.padding(end = 12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${formatMs(tempScrubPosition - 30000L).coerceAtLeast("00:00")}  ←  [ ${formatMs(tempScrubPosition)} ]  →  ${formatMs((tempScrubPosition + 30000L).coerceAtMost(duration))}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Animated Playback controls deck + seek timeline slide from bottom
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 42.dp, vertical = 32.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    
                    // ================= REQUIRED CONTROLS: PROGRESS TIMELINE BAR =================
                    if (type != "live" && duration > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Time counters (Position, Duration, Remaining)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMs(currentPosition),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val leftTimeMs = (duration - currentPosition).coerceAtLeast(0L)
                                Text(
                                    text = "-${formatMs(leftTimeMs)}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Progress Track Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                // Buffered content overlay
                                val bufferPercent = if (duration > 0) bufferedPosition.toFloat() / duration else 0.0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(bufferPercent)
                                        .background(Color.White.copy(alpha = 0.25f))
                                )

                                // Current runtime progress overlay
                                val playPercent = if (duration > 0) currentPosition.toFloat() / duration else 0.0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(playPercent)
                                        .background(Color(0xFFE50914)) // Netflix Red
                                )
                            }
                        }
                    }

                    // Bottom Control Deck (Button HUD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left buttons: Speed/Subtitles indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Speed: ${selectedSpeed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Aspect Ratio: $aspectOption", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Center buttons: Playback seek controllers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Skip Back 30s
                            ControlIconButton(
                                icon = Icons.Default.Replay30 ?: Icons.Default.FastRewind,
                                description = "Skip Back 30s",
                                onClick = {
                                    val target = (currentPosition - 30000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Rewind 10s
                            ControlIconButton(
                                icon = Icons.Default.Replay10 ?: Icons.Default.FastRewind,
                                description = "Rewind 10s",
                                onClick = {
                                    val target = (currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Play / Pause Toggle button
                            val playPauseInteraction = remember { MutableInteractionSource() }
                            val isPlayPauseFocused by playPauseInteraction.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE50914)) // Netflix Red core focus
                                    .focusRequester(playPauseFocusRequester)
                                    .clickable(
                                        interactionSource = playPauseInteraction,
                                        indication = LocalIndication.current,
                                        onClick = {
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                    )
                                    .focusable(interactionSource = playPauseInteraction)
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isPlayPauseFocused) Color.White else Color.Transparent
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Forward 10s
                            ControlIconButton(
                                icon = Icons.Default.Forward10 ?: Icons.Default.FastForward,
                                description = "Skip Forward 10s",
                                onClick = {
                                    val target = (currentPosition + 10000L).coerceAtMost(duration)
                                    exoPlayer.seekTo(target)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Skip Forward 30s
                            ControlIconButton(
                                icon = Icons.Default.Forward30 ?: Icons.Default.FastForward,
                                description = "Skip Forward 30s",
                                onClick = {
                                    val target = (currentPosition + 30000L).coerceAtMost(duration)
                                    exoPlayer.seekTo(target)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        }

                        // Right buttons: Settings and Volume
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Settings Button (Activates sidebar)
                            val settingsInteraction = remember { MutableInteractionSource() }
                            val isSettingsFocused by settingsInteraction.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSettingsFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.08f))
                                    .clickable(
                                        interactionSource = settingsInteraction,
                                        indication = LocalIndication.current,
                                        onClick = {
                                            showSettingsDrawer = true
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                    )
                                    .focusable(interactionSource = settingsInteraction),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Subtitle fast toggles
                            val subToggleInteraction = remember { MutableInteractionSource() }
                            val isSubFocused by subToggleInteraction.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSubFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.08f))
                                    .clickable(
                                        interactionSource = subToggleInteraction,
                                        indication = LocalIndication.current,
                                        onClick = {
                                            selectedSubtitle = if (selectedSubtitle == "Off") "English" else "Off"
                                            Toast.makeText(context, "Subtitles: $selectedSubtitle", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    .focusable(interactionSource = subToggleInteraction),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Cycles subtitles",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= INTRO SKIP FLOATING BUTTON =================
        if (isIntroSegmentActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showControls) 170.dp else 40.dp, end = 40.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val skipIntroInteraction = remember { MutableInteractionSource() }
                val isSkipIntroFocused by skipIntroInteraction.collectIsFocusedAsState()
                Box(
                    modifier = Modifier
                        .focusRequester(skipIntroFocusRequester)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSkipIntroFocused) Color(0xFFE50914) else Color.Black.copy(alpha = 0.75f))
                        .clickable(
                            interactionSource = skipIntroInteraction,
                            indication = LocalIndication.current,
                            onClick = {
                                exoPlayer.seekTo(introDurationConfig)
                                Toast.makeText(context, "Intro Skipped!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        .focusable(interactionSource = skipIntroInteraction)
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isSkipIntroFocused) Color.White else Color.White.copy(alpha = 0.25f)
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Skip Intro",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // ================= CREDITS SKIP & NEXT EPISODE FLOATING BUTTONS =================
        if (isCreditsSegmentActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showControls) 170.dp else 40.dp, end = 40.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Credits Button
                    val skipCreditsInteraction = remember { MutableInteractionSource() }
                    val isSkipCreditsFocused by skipCreditsInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSkipCreditsFocused) Color(0xFFE50914) else Color.Black.copy(alpha = 0.75f))
                            .clickable(
                                interactionSource = skipCreditsInteraction,
                                indication = LocalIndication.current,
                                onClick = {
                                    val endBound = (duration - 1500L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(endBound)
                                    Toast.makeText(context, "Credits Skipped!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            .focusable(interactionSource = skipCreditsInteraction)
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    if (isSkipCreditsFocused) Color.White else Color.White.copy(alpha = 0.25f)
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Skip Credits",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    // Next Episode Button
                    val nextEpInteraction = remember { MutableInteractionSource() }
                    val isNextEpFocused by nextEpInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNextEpFocused) Color(0xFFE50914) else Color.Black.copy(alpha = 0.75f))
                            .clickable(
                                interactionSource = nextEpInteraction,
                                indication = LocalIndication.current,
                                onClick = {
                                    val nextStream = produceNextEpisodeObject(stream)
                                    Toast.makeText(context, "Loading ${nextStream.name}...", Toast.LENGTH_LONG).show()
                                    viewModel.loadStream(nextStream.streamId, type, nextStream.containerExtension)
                                }
                            )
                            .focusable(interactionSource = nextEpInteraction)
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    if (isNextEpFocused) Color.White else Color.White.copy(alpha = 0.25f)
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Next Episode",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ================= ADVANCED CONTROLS SIDE SETTINGS DRAWER =================
        AnimatedVisibility(
            visible = showSettingsDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F070A).copy(alpha = 0.96f)),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFE50914))
                            Text(
                                text = "PLAY PANEL",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { showSettingsDrawer = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Settings Group List Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play Speed Cycle
                        SidebarMenuItem(
                            title = "Playback Speed",
                            currentValue = "${selectedSpeed}x",
                            modifier = Modifier.focusRequester(settingsFirstItemFocusRequester),
                            onClick = {
                                selectedSpeed = when (selectedSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    0.5f -> 0.75f
                                    else -> 1.0f
                                }
                            }
                        )

                        // Aspect ratio cycles
                        SidebarMenuItem(
                            title = "Aspect Ratio",
                            currentValue = aspectOption,
                            onClick = {
                                aspectOption = when (aspectOption) {
                                    "Fit" -> "Stretch"
                                    "Stretch" -> "Zoom"
                                    "Zoom" -> "16:9"
                                    "16:9" -> "4:3"
                                    else -> "Fit"
                                }
                            }
                        )

                        // Video quality cycles
                        SidebarMenuItem(
                            title = "Video Quality",
                            currentValue = videoQuality,
                            onClick = {
                                videoQuality = when (videoQuality) {
                                    "Auto" -> "1080p"
                                    "1080p" -> "720p"
                                    "720p" -> "4K"
                                    else -> "Auto"
                                }
                            }
                        )

                        // Audio track selector
                        SidebarMenuItem(
                            title = "Audio Language",
                            currentValue = selectedAudioTrack,
                            onClick = {
                                selectedAudioTrack = when (selectedAudioTrack) {
                                    "English [Original]" -> "Arabic [Sim]"
                                    "Arabic [Sim]" -> "Spanish"
                                    "Spanish" -> "French"
                                    else -> "English [Original]"
                                }
                            }
                        )

                        // Subtitle choosing selector
                        SidebarMenuItem(
                            title = "Sim Subtitle",
                            currentValue = selectedSubtitle,
                            onClick = {
                                selectedSubtitle = when (selectedSubtitle) {
                                    "Off" -> "English [SRT]"
                                    "English [SRT]" -> "Arabic"
                                    "Arabic" -> "French"
                                    else -> "Off"
                                }
                            }
                        )

                        // Subtitle Size options
                        SidebarMenuItem(
                            title = "Subtitle Size",
                            currentValue = subtitleSize,
                            onClick = {
                                subtitleSize = when (subtitleSize) {
                                    "Medium" -> "Large"
                                    "Large" -> "Extra Large"
                                    "Extra Large" -> "Small"
                                    else -> "Medium"
                                }
                            }
                        )

                        // Subtitle Color option cycles
                        SidebarMenuItem(
                            title = "Subtitle Color",
                            currentValue = subtitleColor,
                            onClick = {
                                subtitleColor = when (subtitleColor) {
                                    "White" -> "Yellow"
                                    "Yellow" -> "Green"
                                    "Green" -> "Cyan"
                                    else -> "White"
                                }
                            }
                        )

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        // Intro limit segments cycles
                        SidebarMenuItem(
                            title = "Intro Duration Limit",
                            currentValue = "${introDurationConfig / 1000}s",
                            onClick = {
                                introDurationConfig = when (introDurationConfig) {
                                    90000L -> 120000L
                                    120000L -> 30000L
                                    30000L -> 60000L
                                    else -> 90000L
                                }
                            }
                        )

                        // Credits duration cycles
                        SidebarMenuItem(
                            title = "Credits Limit Duration",
                            currentValue = "${creditsDurationConfig / 1000}s",
                            onClick = {
                                creditsDurationConfig = when (creditsDurationConfig) {
                                    120000L -> 180000L
                                    180000L -> 60000L
                                    60000L -> 90000L
                                    else -> 120000L
                                }
                            }
                        )
                    }

                    // Bottom info label
                    Text(
                        text = "Press BACK remote key to close the menu",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ================= CONTINUE WATCHING OVERLAY DIALOG =================
        if (showResumePrompt && dbResumePosition > 0L) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0709)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color(0xFFE50914)),
                    modifier = Modifier.width(460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "CONTINUE WATCHING",
                            color = Color(0xFFE50914),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = stream.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "You left off watching at ${formatMs(dbResumePosition)}. Would you like to resume playing from there?",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val focusRequesterResume = remember { FocusRequester() }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Resume button option
                            val actionResumeInteraction = remember { MutableInteractionSource() }
                            val isActionResumeFocused by actionResumeInteraction.collectIsFocusedAsState()
                            Button(
                                onClick = {
                                    showResumePrompt = false
                                    exoPlayer.seekTo(dbResumePosition)
                                    resumeDecisionMade = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActionResumeFocused) Color(0xFFE50914) else Color(0xFF1F1215)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequesterResume)
                                    .focusable(interactionSource = actionResumeInteraction)
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isActionResumeFocused) Color.White else Color.White.copy(alpha = 0.1f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(
                                    text = "Resume at ${formatMs(dbResumePosition)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Start Over button option
                            val actionStartInteraction = remember { MutableInteractionSource() }
                            val isActionStartFocused by actionStartInteraction.collectIsFocusedAsState()
                            Button(
                                onClick = {
                                    showResumePrompt = false
                                    exoPlayer.seekTo(0L)
                                    resumeDecisionMade = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActionStartFocused) Color(0xFFE50914) else Color(0xFF1F1215)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusable(interactionSource = actionStartInteraction)
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isActionStartFocused) Color.White else Color.White.copy(alpha = 0.1f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(
                                    text = "Start Over",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        LaunchedEffect(Unit) {
                            focusRequesterResume.requestFocus()
                        }
                    }
                }
            }
        }
    }

    // Direct launch focus request
    LaunchedEffect(Unit) {
        if (!showResumePrompt) {
            mainScreenFocusRequester.requestFocus()
        }
    }
}

@Composable
fun SidebarMenuItem(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFFE50914).copy(alpha = 0.15f) else Color(0xFF1A1215)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914).copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = currentValue,
                    color = Color(0xFFFFBF00),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.08f))
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
            contentDescription = description,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
