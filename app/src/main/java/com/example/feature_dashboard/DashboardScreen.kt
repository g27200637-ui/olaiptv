package com.example.feature_dashboard

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.core.ui.tvFocusableBorder
import com.example.domain.model.XtreamServer
import com.example.domain.model.XtreamCategory
import com.example.data.local.database.PlaybackProgressEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    playlistName: String,
    serverInfo: XtreamServer?,
    onLiveTvClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onContinueWatchingClick: (PlaybackProgressEntity) -> Unit,
    onChangeServerClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val continueWatchingList by viewModel.continueWatching.collectAsState()
    val liveCategories by viewModel.liveCategories.collectAsState()
    val movieCategories by viewModel.movieCategories.collectAsState()
    val seriesCategories by viewModel.seriesCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()

    // Dialog heights
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSearchChoiceDialog by remember { mutableStateOf(false) }
    var isReloading by remember { mutableStateOf(false) }
    var itemToDeleteByLongClick by remember { mutableStateOf<PlaybackProgressEntity?>(null) }

    // Settings config representation
    var decoderState by remember { mutableStateOf("Hardware (Recommended)") }
    var aspectRatioState by remember { mutableStateOf("Auto") }
    var bufferSizeState by remember { mutableStateOf("2.0 seconds") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1F040A), // Beautiful Crimson-toned Dark Core
                        Color(0xFF0A0204), // Deep Wine Dark transition
                        Color(0xFF020202)  // Full OLED Black edges
                    )
                )
            )
            .padding(top = 16.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================== TOP BAR SYSTEM ==================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand Logo
                BrandedIbLogo()

                // Row of Interactive HUD Controls (Only non-duplicate maintenance controls)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subscription Expiry Card/Chip
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E151A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape) // Green active dot
                        )
                        Text(
                            text = "EXPIRY:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = expiryDate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Refresh Sync Portal
                    DashboardIconButton(
                        icon = Icons.Default.Refresh,
                        label = "Sync Portal",
                        onClick = {
                            coroutineScope.launch {
                                isReloading = true
                                viewModel.loadDashboardData()
                                delay(1800)
                                isReloading = false
                            }
                        }
                    )

                    // Exit Stream application
                    DashboardIconButton(
                        icon = Icons.Default.ExitToApp,
                        label = "Exit",
                        onClick = onExitClick
                    )
                }
            }

            // ================== MAIN VIEW CONTENT STREAM ==================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: (Continue Watching Carousel has been moved to the bottom with the categories)

                // Row 2: Adaptive Launcher Grid supporting both portrait and landscape screen dimensions
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

                if (isPortrait) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // LEFT PANEL: "LIVE TV" (Large tall block)
                        LauncherLiveTvCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp),
                            onClick = onLiveTvClick
                        )

                        // MOVIES and SERIES cards side-by-side
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(108.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LauncherMoviesCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = onMoviesClick
                            )
                            LauncherSeriesCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = onSeriesClick
                            )
                        }

                        // BOTTOM ROW: "LIST USER", "ACCOUNT", "SETTINGS"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LauncherBottomActionCard(
                                label = "LIST USER",
                                icon = Icons.Default.People,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = onChangeServerClick
                            )
                            LauncherBottomActionCard(
                                label = "ACCOUNT",
                                icon = Icons.Default.AccountCircle,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { showAccountDialog = true }
                            )
                            LauncherBottomActionCard(
                                label = "SETTINGS",
                                icon = Icons.Default.Settings,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { showSettingsDialog = true }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT COLUMN PANEL: "LIVE TV" (Large square block)
                        LauncherLiveTvCard(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight(),
                            onClick = onLiveTvClick
                        )

                        // RIGHT COLUMN PANEL: MOVIES, SERIES, and Actions below
                        Column(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // ROW 1: "MOVIES" and "SERIES" side-by-side
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                LauncherMoviesCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = onMoviesClick
                                )
                                LauncherSeriesCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = onSeriesClick
                                )
                            }

                            // ROW 2: "LIST USER", "ACCOUNT", "SETTINGS" smaller action buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.9f),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                LauncherBottomActionCard(
                                    label = "LIST USER",
                                    icon = Icons.Default.People,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = onChangeServerClick
                                )
                                LauncherBottomActionCard(
                                    label = "ACCOUNT",
                                    icon = Icons.Default.AccountCircle,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { showAccountDialog = true }
                                )
                                LauncherBottomActionCard(
                                    label = "SETTINGS",
                                    icon = Icons.Default.Settings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { showSettingsDialog = true }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: Live TV Category Showcase Preview with Shimmer Loading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LIVE TV CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                    if (isLoading) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(8) {
                                Box(
                                    modifier = Modifier
                                        .width(155.dp)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else {
                        if (liveCategories.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(liveCategories) { category ->
                                    DashboardCategoryPreviewCard(
                                        category = category,
                                        typeFlag = "live",
                                        onClick = { onCategoryClick("live", category.categoryId) }
                                    )
                                }
                            }
                        } else {
                            EmptyCategoryRowPlaceholder("No live categories synced")
                        }
                    }
                }

                // Row 4: Movies Category Showcase Preview with Shimmer Loading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MOVIE CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                    if (isLoading) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(8) {
                                Box(
                                    modifier = Modifier
                                        .width(155.dp)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else {
                        if (movieCategories.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(movieCategories) { category ->
                                    DashboardCategoryPreviewCard(
                                        category = category,
                                        typeFlag = "movies",
                                        onClick = { onCategoryClick("movies", category.categoryId) }
                                    )
                                }
                            }
                        } else {
                            EmptyCategoryRowPlaceholder("No movie categories synced")
                        }
                    }
                }

                // Row 5: Series Category Showcase Preview with Shimmer Loading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TV SERIES CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                    if (isLoading) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(8) {
                                Box(
                                    modifier = Modifier
                                        .width(155.dp)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else {
                        if (seriesCategories.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(seriesCategories) { category ->
                                    DashboardCategoryPreviewCard(
                                        category = category,
                                        typeFlag = "series",
                                        onClick = { onCategoryClick("series", category.categoryId) }
                                    )
                                }
                            }
                        } else {
                            EmptyCategoryRowPlaceholder("No series categories synced")
                        }
                    }
                }

                // Row 6: Continue Watching Carousel (Only if progress history exists) - Moved to bottom alongside Categories
                if (continueWatchingList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CONTINUE WATCHING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            letterSpacing = 1.sp
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(continueWatchingList) { item ->
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = { onContinueWatchingClick(item) },
                                    onLongClick = { itemToDeleteByLongClick = item },
                                    onDeleteHistory = { itemToDeleteByLongClick = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Portal reloading state indication Overlay ---
        if (isReloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFEF4444),
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "Synchronizing multimedia playlists with server cache...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================== SEARCH MODE POPUP CHOOSER ==================
        if (showSearchChoiceDialog) {
            Dialog(onDismissRequest = { showSearchChoiceDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F0103),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.width(360.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                        Text(text = "SEARCH DATABASE PORTAL", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "Choose a stream directory to explore and search on screen:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Button(
                            onClick = { showSearchChoiceDialog = false; onLiveTvClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F040A)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text("💻 SEARCH LIVE TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showSearchChoiceDialog = false; onMoviesClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F040A)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text("🎬 SEARCH MOVIES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showSearchChoiceDialog = false; onSeriesClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F040A)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text("📺 SEARCH TV SERIES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ================== ACCOUNT PROFILE INFO POPUP ==================
        if (showAccountDialog) {
            Dialog(onDismissRequest = { showAccountDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF120002),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.width(440.dp).padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AccountBox, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Text(text = "ACTIVE ACCOUNT PROFILE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountDetailRow("Playlist Name", serverInfo?.playlistName ?: "Active IPTV")
                            AccountDetailRow("Server API URL", serverInfo?.serverUrl ?: "http://sample.server")
                            AccountDetailRow("Username Client", serverInfo?.username ?: "DemoUser01")
                            AccountDetailRow("Connection subscription", "ACTIVE (Full Access Plan)")
                        }

                        Button(
                            onClick = { showAccountDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Text("CLOSE DIALOG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================== SYSTEM CONFIGURATION SETTINGS ==================
        if (showSettingsDialog) {
            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF120002),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.width(440.dp).padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Text(text = "SYSTEM ENGINE CONFIG", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ExoDecoder Engine", color = Color.White, fontSize = 12.sp)
                            OutlinedButton(
                                onClick = { decoderState = if (decoderState.contains("Hardware")) "Software" else "Hardware (Recommended)" },
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(decoderState, fontSize = 10.sp, color = Color.White)
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aspect Ratio Frame", color = Color.White, fontSize = 12.sp)
                            OutlinedButton(
                                onClick = { aspectRatioState = if (aspectRatioState == "Auto") "16:9 Stretch" else "Auto" },
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(aspectRatioState, fontSize = 10.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showSettingsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE CONFIG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================== DELETE HISTORY CONFIRM POPUP (Arabic / TV-Optimized) ==================
        itemToDeleteByLongClick?.let { item ->
            Dialog(onDismissRequest = { itemToDeleteByLongClick = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F0103),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.width(380.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "حذف من قائمة المتابعة",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "هل أنت متأكد من رغبتك في حذف \"${item.name}\" من قائمة المتابعة؟",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // CANCEL button (Focus default style)
                            val cancelInteractionSource = remember { MutableInteractionSource() }
                            val cancelFocused by cancelInteractionSource.collectIsFocusedAsState()
                            Button(
                                onClick = { itemToDeleteByLongClick = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cancelFocused) Color.White.copy(alpha = 0.2f) else Color(0xFF1F040A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .tvFocusableBorder(
                                        interactionSource = cancelInteractionSource,
                                        shape = RoundedCornerShape(8.dp),
                                        focusedBorderColor = Color.White,
                                        focusedScale = 1.05f,
                                        focusable = false
                                    ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "إلغاء",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // DELETE button (Focus accent style)
                            val deleteInteractionSource = remember { MutableInteractionSource() }
                            val deleteFocused by deleteInteractionSource.collectIsFocusedAsState()
                            Button(
                                onClick = {
                                    viewModel.deleteContinueWatching(item.id)
                                    itemToDeleteByLongClick = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (deleteFocused) Color(0xFFFF3333) else Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .tvFocusableBorder(
                                        interactionSource = deleteInteractionSource,
                                        shape = RoundedCornerShape(8.dp),
                                        focusedBorderColor = Color.White,
                                        focusedScale = 1.05f,
                                        focusable = false
                                    )
                            ) {
                                Text(
                                    text = "احذف",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFFEF4444) else Color(0xFF1A1214))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.05f,
                focusable = false
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color.White else Color(0xFFEF4444),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    item: PlaybackProgressEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteHistory: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .width(260.dp) // Slightly wider to accommodate image + text nicely
            .height(95.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14080B))
            .border(
                border = BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f,
                focusable = false
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stream Icon / Thumbnail Container (Left Side)
            Box(
                modifier = Modifier
                    .width(65.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221115)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = item.streamIcon,
                        contentDescription = "Poster",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // Fallback visual icon
                    Icon(
                        imageVector = if (item.type == "series") Icons.Default.Tv else Icons.Default.PlayArrow,
                        contentDescription = "Media Icon",
                        tint = Color(0xFFEF4444).copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Details and progress (Right Side)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.type.uppercase(),
                            color = Color(0xFFEF4444),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Delete history icon
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onDeleteHistory() }
                    )
                }

                // Progress Bar and Percent Label
                val progressVal = if (item.durationMs > 0) item.positionMs.toFloat() / item.durationMs else 0f
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESUME",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progressVal * 100).toInt().coerceIn(0, 100)}%",
                            fontSize = 8.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(1.5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressVal.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color(0xFFEF4444), RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCategoryPreviewCard(
    category: XtreamCategory,
    typeFlag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cardBg = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFF4C0811), Color(0xFF200306)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF140C0E), Color(0xFF0F080A)))
    }

    Box(
        modifier = Modifier
            .width(155.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(
                border = BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.05f,
                focusable = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.categoryName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyCategoryRowPlaceholder(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(horizontal = 12.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Text(text = msg, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BrandedIbLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.15f)
                lineTo(size.width * 0.5f, size.height * 0.85f)
                close()
            }
            drawPath(path = path, color = Color(0xFFEF4444))
        }
        Text(
            text = "OLA IPTV",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AccountDetailRow(label: String, valText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = valText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ================== CUSTOM HIGH-END TV LAUNCHER UI COMPONENTS ==================

@Composable
fun LauncherLiveTvCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val liveGradient = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFFFF2E54), Color(0xFFC40024)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE50914), Color(0xFF5A000F)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(liveGradient)
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(16.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f,
                focusable = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiveTvLogoIcon(color = Color.White)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "LIVE TV",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun LauncherMoviesCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val moviesGradient = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFFFF6B4A), Color(0xFFC8102E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEA4335), Color(0xFF9B001C)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(moviesGradient)
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(16.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f,
                focusable = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MoviesLogoIcon(color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "MOVIES",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun LauncherSeriesCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val seriesGradient = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFFFF007F), Color(0xFF5A0079)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFC71585), Color(0xFF4A0E4E)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(seriesGradient)
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(16.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f,
                focusable = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SeriesLogoIcon(color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SERIES",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun LauncherBottomActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgBrush = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFFB91C1C), Color(0xFF6F0E17)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1E1214).copy(alpha = 0.9f), Color(0xFF0F0708).copy(alpha = 0.9f)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f,
                focusable = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.White else Color(0xFFEF4444),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun LiveTvLogoIcon(color: Color = Color.White) {
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val width = size.width
            val height = size.height

            // Antennas
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.35f),
                end = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.15f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.35f),
                end = androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.15f),
                strokeWidth = strokeWidth
            )
            drawCircle(color = color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.15f))
            drawCircle(color = color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.15f))

            // TV Case Box
            val rectTop = height * 0.33f
            val rectHeight = height * 0.55f
            val rectWidth = width * 0.8f
            val rectLeft = width * 0.1f

            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .border(BorderStroke(1.5.dp, Color.White), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun MoviesLogoIcon(color: Color = Color.White) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .border(2.dp, Color.White, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp).padding(start = 2.dp)
        )
    }
}

@Composable
fun SeriesLogoIcon(color: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.MovieFilter,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(48.dp)
    )
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_rotation"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.06f),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, translateAnim - 200f),
            end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
        )
    )
}
