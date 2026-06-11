package com.example.feature_channels

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.ui.tvFocusableBorder
import com.example.domain.model.XtreamStream
import com.example.data.local.database.PlaybackProgressEntity

@Composable
fun MovieDetailsScreen(
    movie: XtreamStream,
    isFavorite: Boolean,
    playbackProgress: PlaybackProgressEntity?,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onBackClick: () -> Unit,
    onRecommendMovieClick: (XtreamStream) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTrailerDialog by remember { mutableStateOf(false) }
    var focusedScreenshotIndex by remember { mutableStateOf<Int?>(null) }

    // Curated dynamic constants based on Movie ID
    val score = remember(movie.id) {
        if (!movie.rating.isNullOrBlank()) {
            movie.rating.toDoubleOrNull()
        } else {
            null
        }
    }
    
    val runtime = "-"

    val releaseYear = remember(movie.id) {
        if (!movie.releaseDate.isNullOrBlank() && movie.releaseDate.length >= 4) {
            movie.releaseDate.take(4)
        } else {
            "-"
        }
    }

    val synopsis = remember(movie.id) {
        if (!movie.plot.isNullOrBlank()) {
            movie.plot
        } else {
            "-"
        }
    }

    val tagline = ""

    val castList = remember(movie.id) {
        val parsedCast = mutableListOf<CastMember>()
        if (!movie.cast.isNullOrBlank()) {
            val names = movie.cast.split(",").map { it.trim() }.filter { it.isNotBlank() }
            names.forEach { name ->
                parsedCast.add(CastMember(name, "-", ""))
            }
        }
        parsedCast
    }

    val screenshotUrls = remember(movie.id) {
        listOf(
            "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=600&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=600&auto=format&fit=crop"
        )
    }

    val similarAndRecommendedList = remember(movie.id) {
        val titles = listOf("Star Seekers", "The Red Horizon", "Starlight Sync", "Quantum Protocol", "Cyber Drift 2", "Neon Catalyst", "Solar Strike")
        val icons = listOf(
            "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=300&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1542204172-e70528091869?q=80&w=300&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=300&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1574267431629-2e570b28a125?q=80&w=300&auto=format&fit=crop"
        )
        titles.mapIndexed { idx, t ->
            XtreamStream(
                streamId = 5000 + idx + (movie.id * 10),
                name = t,
                streamIcon = icons[idx % icons.size],
                rating = "%.1f".format(6.8 + (idx % 3) * 0.7),
                releaseDate = (2020 + (idx % 6)).toString()
            )
        }
    }

    val progressValue = remember(playbackProgress) {
        if (playbackProgress != null && playbackProgress.durationMs > 0) {
            ((playbackProgress.positionMs * 100) / playbackProgress.durationMs).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    val progressTimeText = remember(playbackProgress) {
        if (playbackProgress != null) {
            val from = formatMillis(playbackProgress.positionMs)
            val to = formatMillis(playbackProgress.durationMs)
            "$from / $to"
        } else {
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040103))
    ) {
        // ================== CINEMATIC BACKGROUND BLURRED BACKDROP ==================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
        ) {
            AsyncImage(
                model = movie.icon ?: screenshotUrls[0],
                contentDescription = "Backdrop background blurred",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            )
            // Beautiful triple gradient overlay for premium visual atmosphere
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF040103),
                                Color(0xFF040103).copy(alpha = 0.95f),
                                Color(0xFF040103).copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            startX = 0.0f,
                            endX = 1400.0f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF040103).copy(alpha = 0.4f),
                                Color(0xFF040103)
                            ),
                            startY = 0.0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // ================== INNER SCROLLABLE CONTENT CANVAS ==================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // BACK ACTION & HUD STATUS HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val isBackFocused by backInteraction.collectIsFocusedAsState()

                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackFocused) Color(0xFFEF4444) else Color(0xFF1C1318)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        interactionSource = backInteraction,
                        modifier = Modifier
                            .height(44.dp)
                            .border(
                                width = if (isBackFocused) 2.dp else 1.dp,
                                color = if (isBackFocused) Color.White else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BACK TO LIBRARY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEF4444), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PREMIUM STREAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = "XTREAM CINEMA HUB",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Streaming / Audio Formats Glassmorphic Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityLabel(text = "4K ULTRA HD")
                    QualityLabel(text = "HDR 10+")
                    QualityLabel(text = "DOLBY VISION")
                    QualityLabel(text = "DOLBY ATMOS")
                    QualityLabel(text = "DTS:X 7.1")
                }
            }

            // HERO HERO BLOCK (Poster Left, Meta & Description Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Large Poster Left with Progress meter if partially watched
                Column(
                    modifier = Modifier.width(220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1A1D))
                            .border(
                                BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        AsyncImage(
                            model = movie.icon,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 4K Cinema watermark tag overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "4K BD",
                                color = Color(0xFFEF4444),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Partial progress indicator if watched
                        if (progressValue > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "RESUME WATCHING",
                                            color = Color(0xFFEF4444),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            "$progressValue%",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (progressTimeText.isNotEmpty()) {
                                        Text(
                                            text = progressTimeText,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = progressValue / 100f,
                                        color = Color(0xFFEF4444),
                                        trackColor = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Metadata Details Block Right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = movie.name,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = releaseYear,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Tagline
                        if (tagline.isNotEmpty()) {
                            Text(
                                text = "\"$tagline\"",
                                fontSize = 15.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444).copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Metadata details badges: runtime, parental rating, and circular ratings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Score percentage circular indicator (Double arc ring style)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserRatingCircularProgress(score = score)
                            Column {
                                Text(
                                    text = "USER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "SCORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        VerticalDivider()

                        // Runtime duration
                        InfoPillBadge(icon = Icons.Default.Schedule, label = runtime)

                        VerticalDivider()

                        // Age parental rating badge
                        val ageBadge = "-"
                        InfoPillBadge(icon = Icons.Default.Info, label = ageBadge)

                        VerticalDivider()

                        // Genre tags row
                        val genres = "-"
                        InfoPillBadge(icon = Icons.Default.Category, label = genres)
                    }

                    // CRITIC RATINGS ROW (IMDb, TMDB, Rotten Tomatoes)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val imdbVal = remember(score) { score?.let { "%.1f".format(it) } ?: "-" }
                        val tomatoesVal = "-"
                        val tmdbVal = "-"

                        // IMDb Badge
                        CriticRatingBadge(
                            source = "IMDb",
                            score = imdbVal,
                            tint = Color(0xFFF5C518),
                            textColor = Color.Black
                        )
                        // TMDB Badge
                        CriticRatingBadge(
                            source = "TMDB",
                            score = tmdbVal,
                            tint = Color(0xFF01B4E4),
                            textColor = Color.White
                        )
                        // Rotten Tomatoes Badge
                        CriticRatingBadge(
                            source = "Rotten Tomatoes",
                            score = tomatoesVal,
                            tint = Color(0xFFFA320A),
                            textColor = Color.White
                        )
                    }

                    // Synopsis Overview block
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SYNOPSIS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = synopsis,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action buttons (Play, Add to Favorites, Watchlist, Trailer, Share)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Play Button (Primary)
                        TvActionButton(
                            onClick = onPlayClick,
                            containerColor = Color(0xFFEF4444),
                            focusedColor = Color.White,
                            focusedTextColor = Color.Black,
                            content = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (progressValue > 0) "RESUME PLAYBACK" else "PLAY MOVIE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (progressValue > 0 && progressTimeText.isNotEmpty()) {
                                        Text(
                                            text = progressTimeText,
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        )

                        // Add to Favorites (Toggle)
                        TvActionButton(
                            onClick = onFavoriteToggle,
                            containerColor = if (isFavorite) Color(0xFF1E0D12) else Color(0xFF1B1417),
                            focusedColor = Color(0xFFEF4444),
                            focusedTextColor = Color.White,
                            borderColor = if (isFavorite) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f),
                            content = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFavorite) "FAVORITED" else "FAVORITE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        )

                        // Trailer Button
                        TvActionButton(
                            onClick = { showTrailerDialog = true },
                            containerColor = Color(0xFF1B1417),
                            focusedColor = Color.White,
                            focusedTextColor = Color.Black,
                            content = {
                                Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TRAILER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        )
                    }
                }
            }

            // CREW & GENERAL INFORMATION HUB
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0609).copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    val directorVal = remember(movie.id) {
                        if (!movie.director.isNullOrBlank()) movie.director else "-"
                    }
                    val writerVal = "-"
                    val studioVal = "-"

                    // General Staff
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("PRODUCTION STAFF", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444), letterSpacing = 1.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StaffItem(title = "Director", value = directorVal)
                            StaffItem(title = "Screenplay Writers", value = writerVal)
                            StaffItem(title = "Production Studio", value = studioVal)
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Original stats
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("TECHNICAL METADATA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444), letterSpacing = 1.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StaffItem(title = "Original Title", value = movie.name.ifBlank { "-" })
                                StaffItem(title = "Language", value = "-")
                                StaffItem(title = "Stream Status", value = "Active")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StaffItem(title = "Country", value = "-")
                                StaffItem(title = "Subtitles", value = "-")
                                StaffItem(title = "Codec Rate", value = "-")
                            }
                        }
                    }
                }
            }

            // ================== CAST & CREW SECTION ==================
            if (castList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "FEATURED CAST & CHARACTERS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 48.dp)
                    ) {
                        items(castList) { cast ->
                            var isCardFocused by remember { mutableStateOf(false) }
                            val interactionSource = remember { MutableInteractionSource() }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCardFocused) Color(0xFF1E0E14) else Color(0xFF0F070A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(135.dp)
                                    .tvFocusableBorder(
                                        interactionSource = interactionSource,
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBorderColor = Color(0xFFEF4444)
                                    )
                                    .onFocusChanged { isCardFocused = it.isFocused }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!cast.picUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = cast.picUrl,
                                            contentDescription = cast.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(75.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(75.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1F1216))
                                                .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "User profile photo placeholder",
                                                tint = Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = cast.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = cast.charName,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ================== SHIFT ZOOM FULLSCREEN SCREENSHOT DIALOG ==================
        if (focusedScreenshotIndex != null) {
            val url = screenshotUrls[focusedScreenshotIndex!!]
            Dialog(
                onDismissRequest = { focusedScreenshotIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Zoomed scene frame",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        )

                        // Dialog controls help badge in bottom of fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("TAP OUTSIDE OR PRESS BACK TO EXTINGUISH VIEWPORT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Box(modifier = Modifier.size(4.dp).background(Color(0xFFEF4444), CircleShape))
                            Button(
                                onClick = { focusedScreenshotIndex = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("CLOSE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ================== PREMIUM GLOWING TRAILER INTERACTIVE DIALOG ==================
        if (showTrailerDialog) {
            Dialog(
                onDismissRequest = { showTrailerDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    color = Color(0xFF070204).copy(alpha = 0.98f),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                Text("TRAILER STREAM: ${movie.name.uppercase()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            IconButton(onClick = { showTrailerDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        // Simulated Cinema visual playback deck representation with soundwaves and gradient
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video poster artwork embedded behind a translucent tint
                            AsyncImage(
                                model = movie.icon ?: screenshotUrls[1],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.35f)
                            )

                            // Superimposed cinematic HUD elements (loading spinner & spectrum bars)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 3.dp)
                                Text(
                                    "ESTABLISHING SECURE DECODER CACHE PIPELINE...",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                // Interactive sound wave simulation
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "audio_faders")
                                    val sizes = listOf(45.dp, 25.dp, 55.dp, 35.dp, 48.dp, 15.dp)
                                    sizes.forEachIndexed { i, originalHeight ->
                                        val heightMultiplier by infiniteTransition.animateFloat(
                                            initialValue = 0.2f,
                                            targetValue = 1.0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(durationMillis = 400 + (i * 120), easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "fader_$i"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(originalHeight * heightMultiplier)
                                                .background(Color(0xFFEF4444), RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }
                        }

                        // Playback controller bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showTrailerDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("PAUSE DECK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showTrailerDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("TOGGLE COMPANION SOUND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            Text("SIMULATED SECURE TUNNEL PLAYBACK  |  4K STEREO DTS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun QualityLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CriticRatingBadge(
    source: String,
    score: String,
    tint: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(tint, RoundedCornerShape(4.dp))
                .padding(vertical = 2.dp, horizontal = 6.dp)
        ) {
            Text(
                text = source,
                color = textColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = score,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun UserRatingCircularProgress(
    score: Double?,
    modifier: Modifier = Modifier
) {
    val percentValue = score?.let { (it * 10).toInt() }
    Box(
        modifier = modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background gray path
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.width / 2f,
                style = Stroke(width = 3.dp.toPx())
            )
            if (percentValue != null) {
                // Draw sweeps progress arc with premium green tint color
                val sweepAngle = (percentValue / 100f) * 360f
                drawArc(
                    color = Color(0xFF22C55E),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }
        }
        Text(
            text = percentValue?.let { "$it%" } ?: "-",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun InfoPillBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TvActionButton(
    onClick: () -> Unit,
    containerColor: Color,
    focusedColor: Color,
    focusedTextColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) focusedColor else containerColor)
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.05f,
                focusable = true
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColorAdjusted = if (isFocused) focusedTextColor else Color.White
        CompositionLocalProvider(
            LocalContentColor provides textColorAdjusted
        ) {
            content()
        }
    }
}

@Composable
fun StaffItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title: ",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(135.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1f.dp)
            .height(20.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
}

data class CastMember(
    val name: String,
    val charName: String,
    val picUrl: String
)

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
