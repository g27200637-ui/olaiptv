package com.example.feature_channels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import coil.compose.AsyncImage
import com.example.core.ui.tvFocusableBorder
import com.example.domain.model.XtreamCategory
import com.example.domain.model.XtreamStream
import com.example.domain.model.XtreamSeriesInfoResponse

@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel,
    type: String = "live",
    initialCategoryId: String? = null,
    onChannelSelected: (XtreamStream) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val streamsState by viewModel.streamsState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playlistName = remember { viewModel.getPlaylistName() }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    // Movies and Series overlays
    var selectedMovieForDetails by remember { mutableStateOf<XtreamStream?>(null) }
    var selectedSeriesForEpisodes by remember { mutableStateOf<XtreamStream?>(null) }
    var seriesEpisodesResponse by remember { mutableStateOf<XtreamSeriesInfoResponse?>(null) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    // Local back hook
    BackHandler {
        if (selectedMovieForDetails != null) {
            selectedMovieForDetails = null
        } else if (selectedSeriesForEpisodes != null) {
            selectedSeriesForEpisodes = null
        } else {
            onBack()
        }
    }

    // Load categories only on initial enter or type change
    LaunchedEffect(type) {
        viewModel.loadCategories(type)
    }

    // Set initial category if specified
    LaunchedEffect(categoriesState, initialCategoryId) {
        if (initialCategoryId != null && categoriesState is CategoriesUiState.Success) {
            val list = (categoriesState as CategoriesUiState.Success).categories
            val matching = list.firstOrNull { it.categoryId == initialCategoryId }
            if (matching != null) {
                viewModel.selectCategory(matching)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030104)) // High-end dark cinematic purple-toned canvas
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================== CINEMATIC HEADER ==================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (type) {
                                "movies" -> Icons.Default.Movie
                                "series" -> Icons.Default.Tv
                                else -> Icons.Default.Tv
                            },
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (type) {
                                "movies" -> "Cinematic Movies"
                                "series" -> "TV Seasons & Series"
                                else -> "High-Fidelity Live TV"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "SYNCED PLYLIST: ${playlistName.uppercase()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444).copy(alpha = 0.65f),
                        letterSpacing = 1.sp
                    )
                }

                // Modern Header Navigation Bar & Integrated D-pad Search Box
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Integrated TV Search Bar
                    TVSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) }
                    )

                    // Back to main Dashboard Button
                    IconButtonWithLabel(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Main Dashboard",
                        onClick = onBack
                    )
                }
            }

            // ================== SIDEBAR CATEGORIES + MAIN GRID COLUMN ==================
            if (isPortrait) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Left pane categories list as Horizontal row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        when (val catState = categoriesState) {
                            is CategoriesUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                }
                            }
                            is CategoriesUiState.Success -> {
                                CategoryHorizontalRow(
                                    categories = catState.categories,
                                    selectedCategory = selectedCategory,
                                    onCategoryClick = { viewModel.selectCategory(it) }
                                )
                            }
                            is CategoriesUiState.Error -> {
                                Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                    Text(catState.message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Right pane grid list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF070204).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(16.dp))
                            .padding(6.dp)
                    ) {
                        when (val strState = streamsState) {
                            is StreamsUiState.Idle -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select any category to stream contents", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            is StreamsUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 3.dp)
                                }
                            }
                            is StreamsUiState.Success -> {
                                val filteredStreams = remember(strState.streams, searchQuery) {
                                    if (searchQuery.isBlank()) {
                                        strState.streams
                                    } else {
                                        strState.streams.filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                    }
                                }

                                if (filteredStreams.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "No matching results for '${searchQuery}'" else "No streams found inside this category",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    ChannelGrid(
                                        streams = filteredStreams,
                                        type = type,
                                        isFavoriteFlow = { id -> viewModel.isStreamFavorite(id) },
                                        onFavoriteToggle = { stream -> viewModel.toggleFavorite(stream) },
                                        onStreamClick = { selectedStream ->
                                            if (type == "movies") {
                                                selectedMovieForDetails = selectedStream
                                            } else if (type == "series") {
                                                selectedSeriesForEpisodes = selectedStream
                                                isLoadingEpisodes = true
                                                seriesEpisodesResponse = null
                                                viewModel.getSeriesInfo(selectedStream.id) { response ->
                                                    seriesEpisodesResponse = response
                                                    isLoadingEpisodes = false
                                                }
                                            } else {
                                                onChannelSelected(selectedStream)
                                            }
                                        }
                                    )
                                }
                            }
                            is StreamsUiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(strState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Left pane categories list
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.32f)
                            .background(Color(0xFF0A0507), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        when (val catState = categoriesState) {
                            is CategoriesUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 3.dp)
                                }
                            }
                            is CategoriesUiState.Success -> {
                                CategorySidebarList(
                                    categories = catState.categories,
                                    selectedCategory = selectedCategory,
                                    onCategoryClick = { viewModel.selectCategory(it) }
                                )
                            }
                            is CategoriesUiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(catState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right pane grid list
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(Color(0xFF070204).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        when (val strState = streamsState) {
                            is StreamsUiState.Idle -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select any category to stream contents", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                            is StreamsUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFEF4444), strokeWidth = 3.dp)
                                }
                            }
                            is StreamsUiState.Success -> {
                                // Filter list based on search query
                                val filteredStreams = remember(strState.streams, searchQuery) {
                                    if (searchQuery.isBlank()) {
                                        strState.streams
                                    } else {
                                        strState.streams.filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                    }
                                }

                                if (filteredStreams.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "No matching results for '${searchQuery}'" else "No streams found inside this category",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    ChannelGrid(
                                        streams = filteredStreams,
                                        type = type,
                                        isFavoriteFlow = { id -> viewModel.isStreamFavorite(id) },
                                        onFavoriteToggle = { stream -> viewModel.toggleFavorite(stream) },
                                        onStreamClick = { selectedStream ->
                                            if (type == "movies") {
                                                selectedMovieForDetails = selectedStream
                                            } else if (type == "series") {
                                                selectedSeriesForEpisodes = selectedStream
                                                isLoadingEpisodes = true
                                                seriesEpisodesResponse = null
                                                viewModel.getSeriesInfo(selectedStream.id) { response ->
                                                    seriesEpisodesResponse = response
                                                    isLoadingEpisodes = false
                                                }
                                            } else {
                                                onChannelSelected(selectedStream)
                                            }
                                        }
                                    )
                                }
                            }
                            is StreamsUiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(strState.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Footer bar Info HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), RoundedCornerShape(3.dp)))
                    val labelCount = when (val state = streamsState) {
                        is StreamsUiState.Success -> {
                            val sizeStr = state.streams.size
                            when (type) {
                                "movies" -> "$sizeStr Movies Indexed"
                                "series" -> "$sizeStr TV Shows Indexed"
                                else -> "$sizeStr Active Channels Live"
                            }
                        }
                        else -> "Sync Ready"
                    }
                    Text(text = labelCount.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndicatorBadge(label = "OK", actionDesc = "Connect")
                    IndicatorBadge(label = "⭐/LONG OK", actionDesc = "Toggle Favorites")
                }
            }
        }

        // ================== MOVIES DETAIL OVERLAY DIALOG ==================
        if (selectedMovieForDetails != null) {
            val movie = selectedMovieForDetails!!
            val isFavoriteState by viewModel.isStreamFavorite(movie.id).collectAsState(initial = false)
            val allProgressList by viewModel.allPlaybackProgressList.collectAsState()
            val movieProgress = remember(allProgressList, movie.id) {
                allProgressList.find { it.id == movie.id }
            }

            Dialog(
                onDismissRequest = { selectedMovieForDetails = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                MovieDetailsScreen(
                    movie = movie,
                    isFavorite = isFavoriteState,
                    playbackProgress = movieProgress,
                    onPlayClick = {
                        onChannelSelected(movie)
                        selectedMovieForDetails = null
                    },
                    onFavoriteToggle = {
                        viewModel.toggleFavorite(movie)
                    },
                    onBackClick = {
                        selectedMovieForDetails = null
                    },
                    onRecommendMovieClick = { selectedStream ->
                        selectedMovieForDetails = selectedStream
                    }
                )
            }
        }        // ================== TV SERIES COVERS + EPISODES SELECTOR ==================
        if (selectedSeriesForEpisodes != null) {
            val series = selectedSeriesForEpisodes!!
            Dialog(
                onDismissRequest = { selectedSeriesForEpisodes = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF040103))
                ) {
                    // Cinematic Blur Backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.68f)
                    ) {
                        AsyncImage(
                            model = series.icon,
                            contentDescription = "Backdrop background blurred",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp)
                        )
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
                                            Color(0xFF040103).copy(alpha = 0.5f),
                                            Color(0xFF040103)
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with Back Button (Highly accessible for TV)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val backInteraction = remember { MutableInteractionSource() }
                            val isBackFocused by backInteraction.collectIsFocusedAsState()

                            Button(
                                onClick = { selectedSeriesForEpisodes = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBackFocused) Color(0xFFEF4444) else Color(0xFF1E1E1E)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                interactionSource = backInteraction,
                                modifier = Modifier
                                    .height(44.dp)
                                    .border(
                                        width = if (isBackFocused) 2.dp else 1.dp,
                                        color = if (isBackFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BACK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "SERIES DETAILS",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isLoadingEpisodes) {
                                CircularProgressIndicator(
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.align(Alignment.Center),
                                    strokeWidth = 3.5.dp
                                )
                            } else {
                                val response = seriesEpisodesResponse
                                val mapEpisodes = response?.episodes ?: emptyMap()
                                if (mapEpisodes.isEmpty()) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(46.dp))
                                        Text("No episodes cataloged for this TV Show", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = { selectedSeriesForEpisodes = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("RETURN BACK", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    val listSeasons = remember(mapEpisodes) {
                                        mapEpisodes.keys.toList().sortedBy { it.toIntOrNull() ?: 0 }
                                    }
                                    // Properly reset selection when listSeasons changes due to new TV series.
                                    var currentSeasonSelected by remember(listSeasons) { mutableStateOf(listSeasons.firstOrNull() ?: "") }
                                    val isFavoriteSeries by viewModel.isStreamFavorite(series.id).collectAsState(initial = false)

                                    if (isPortrait) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Poster & Series Metadata stacked
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                AsyncImage(
                                                    model = series.icon,
                                                    contentDescription = series.name,
                                                    modifier = Modifier
                                                        .width(110.dp)
                                                        .height(160.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.DarkGray)
                                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = series.name,
                                                        color = Color.White,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        lineHeight = 22.sp
                                                    )

                                                    // Ratings
                                                    val seriesRating = remember(series.id) {
                                                        if (!response?.info?.rating.isNullOrBlank()) {
                                                            response.info.rating
                                                        } else {
                                                            val randomVal = (series.id % 15 + 80) / 10.0
                                                            "%.1f".format(randomVal)
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                                        Text(text = "$seriesRating / 10", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Button(
                                                        onClick = { viewModel.toggleFavorite(series) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isFavoriteSeries) Color(0xFF1A1214) else Color(0xFF2E2E2E)
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = if (isFavoriteSeries) BorderStroke(1.dp, Color(0xFFEF4444)) else null,
                                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFavoriteSeries) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = null,
                                                            tint = if (isFavoriteSeries) Color(0xFFEF4444) else Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isFavoriteSeries) "FAVORITED" else "FAVORITE",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }

                                            // Story
                                            val seriesPlot = remember(series.id, response) {
                                                if (!response?.info?.plot.isNullOrBlank()) {
                                                    response.info.plot
                                                } else {
                                                    "An award-winning story cataloged on Ola IPTV. Full of mystery, action, and brilliant performance. Play the episodes seamlessly on your home screen."
                                                }
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(text = "STORY / القصة", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = seriesPlot,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }

                                            Divider(color = Color.White.copy(alpha = 0.12f))

                                            // Seasons tabs
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "SELECT SEASON",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                )

                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(listSeasons) { season ->
                                                        val isSeasonActive = season == currentSeasonSelected
                                                        val seasonInteractionSource = remember { MutableInteractionSource() }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSeasonActive) Color(0xFFEF4444) else Color(0xFF14080B))
                                                                .clickable { currentSeasonSelected = season }
                                                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp))
                                                                .padding(vertical = 8.dp, horizontal = 14.dp)
                                                        ) {
                                                            Text(
                                                                text = "SEASON $season",
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Divider(color = Color.White.copy(alpha = 0.06f))

                                            // Episodes lists inside portrait
                                            Text(
                                                text = "SEASON $currentSeasonSelected EPISODES (${(mapEpisodes[currentSeasonSelected] ?: emptyList()).size})",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            val listEpisodes = mapEpisodes[currentSeasonSelected] ?: emptyList()
                                            val allProgressList by viewModel.allPlaybackProgressList.collectAsState()
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listEpisodes.forEach { episode ->
                                                    val epInteractionSource = remember { MutableInteractionSource() }
                                                    val isFocusedEp by epInteractionSource.collectIsFocusedAsState()

                                                    val episodeIdInt = remember(episode.id) { episode.id.toIntOrNull() ?: 0 }
                                                    val epProgress = remember(allProgressList, episodeIdInt) {
                                                        allProgressList.find { it.id == episodeIdInt }
                                                    }
                                                    val epPercentage = remember(epProgress) {
                                                        if (epProgress != null && epProgress.durationMs > 0) {
                                                            ((epProgress.positionMs * 100) / epProgress.durationMs).toInt().coerceIn(0, 100)
                                                        } else {
                                                            0
                                                        }
                                                    }

                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isFocusedEp) Color(0xFF1D0A0F) else Color(0xFF0F0608)
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(64.dp)
                                                            .clickable(
                                                                interactionSource = epInteractionSource,
                                                                indication = null,
                                                                onClick = {
                                                                    val epStream = XtreamStream(
                                                                        streamId = episode.id.toIntOrNull() ?: 0,
                                                                        name = "${series.name} - S${currentSeasonSelected}E${episode.episodeNum ?: 1}: ${episode.title}",
                                                                        streamIcon = series.icon,
                                                                        categoryId = series.categoryId,
                                                                        containerExtension = episode.containerExtension
                                                                    )
                                                                    onChannelSelected(epStream)
                                                                    selectedSeriesForEpisodes = null
                                                                }
                                                            )
                                                            .focusable(interactionSource = epInteractionSource)
                                                            .tvFocusableBorder(
                                                                interactionSource = epInteractionSource,
                                                                shape = RoundedCornerShape(8.dp),
                                                                focusedBorderColor = Color(0xFFEF4444),
                                                                focusedScale = 1.02f,
                                                                focusable = false
                                                            )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 16.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Circular play/index
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                                modifier = Modifier.weight(0.85f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(Color(0xFF241418), CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = (episode.episodeNum ?: 1).toString(),
                                                                        color = Color(0xFFEF4444),
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Black
                                                                    )
                                                                }

                                                                Column {
                                                                    Text(
                                                                        text = episode.title ?: "Episode ${episode.episodeNum}",
                                                                        color = Color.White,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    if (!episode.containerExtension.isNullOrBlank()) {
                                                                        Text(
                                                                            text = "Format: ${episode.containerExtension.uppercase()}",
                                                                            color = Color.White.copy(alpha = 0.4f),
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "Play Episode",
                                                                tint = Color.White.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            // Left Column: Poster display, series info and button details (Spacious 0.35f & Scrollable)
                                            Column(
                                                modifier = Modifier
                                                    .weight(0.35f)
                                                    .fillMaxHeight()
                                                    .padding(end = 20.dp)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                AsyncImage(
                                                    model = series.icon,
                                                    contentDescription = series.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(210.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.DarkGray)
                                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(12.dp)),
                                                    contentScale = ContentScale.Crop
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = series.name,
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        lineHeight = 26.sp,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    
                                                    // Ratings badging for Series info
                                                    val seriesRating = remember(series.id) {
                                                        if (!response?.info?.rating.isNullOrBlank()) {
                                                            response.info.rating
                                                        } else {
                                                            val randomVal = (series.id % 15 + 80) / 10.0
                                                            "%.1f".format(randomVal)
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(text = "$seriesRating / 10", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                        ) {
                                                            Text(text = "SERIES / مسلسل", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                                        }
                                                    }
                                                }

                                                // Toggle Favorites button inside tv shows details (Highly visible/accessible)
                                                Button(
                                                    onClick = { viewModel.toggleFavorite(series) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isFavoriteSeries) Color(0xFF1A1214) else Color(0xFF2E2E2E)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = if (isFavoriteSeries) BorderStroke(1.dp, Color(0xFFEF4444)) else null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFavoriteSeries) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = if (isFavoriteSeries) Color(0xFFEF4444) else Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isFavoriteSeries) "FAVORITED / المفضلة" else "FAVORITE / مفضلة",
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = Color.White
                                                    )
                                                }

                                                Divider(color = Color.White.copy(alpha = 0.12f))

                                                // Series plot story
                                                val seriesPlot = remember(series.id, response) {
                                                    if (!response?.info?.plot.isNullOrBlank()) {
                                                        response.info.plot
                                                    } else {
                                                        "An award-winning story cataloged on Ola IPTV. Full of mystery, action, and brilliant performance. Play the episodes seamlessly on your home screen."
                                                    }
                                                }
                                                
                                                Text(
                                                    text = "STORY / القصة",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = seriesPlot,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 14.sp,
                                                    maxLines = 12,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 20.sp
                                                )

                                                Divider(color = Color.White.copy(alpha = 0.08f))

                                                val seriesCast = response?.info?.cast ?: "Premium TV Show Leads, Drama Stars"
                                                Column {
                                                    Text(
                                                        text = "Cast:",
                                                        color = Color.White.copy(alpha = 0.5f),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = seriesCast,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis,
                                                        lineHeight = 18.sp
                                                    )
                                                }
                                            }

                                            // Right Column: Horizontal Seasons tab-strip + Vertical Episodes Cards (Aesthetic 0.65f)
                                            Column(
                                                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "EPISODES DECK",
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }

                                                // Season choosing subtitle
                                                Text(
                                                    text = "SELECT SEASON",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                )

                                                // Seasons list as a horizontal scroll row
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(listSeasons) { season ->
                                                        val isSeasonActive = season == currentSeasonSelected
                                                        val seasonInteractionSource = remember { MutableInteractionSource() }
                                                        val isFocusedSeason by seasonInteractionSource.collectIsFocusedAsState()

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isSeasonActive) Color(0xFFEF4444)
                                                                    else if (isFocusedSeason) Color.White.copy(alpha = 0.15f)
                                                                    else Color(0xFF14080B)
                                                                )
                                                                .clickable(
                                                                    interactionSource = seasonInteractionSource,
                                                                    indication = null,
                                                                    onClick = { currentSeasonSelected = season }
                                                                )
                                                                .focusable(interactionSource = seasonInteractionSource)
                                                                .border(
                                                                    BorderStroke(
                                                                        width = if (isFocusedSeason) 1.5.dp else 1.dp,
                                                                        color = if (isFocusedSeason) Color.White else Color.White.copy(alpha = 0.05f)
                                                                    ),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(vertical = 10.dp, horizontal = 18.dp)
                                                        ) {
                                                            Text(
                                                                text = "SEASON $season",
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }

                                                Divider(color = Color.White.copy(alpha = 0.06f))

                                                Text(
                                                    text = "SEASON $currentSeasonSelected EPISODES (${(mapEpisodes[currentSeasonSelected] ?: emptyList()).size})",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                val listEpisodes = mapEpisodes[currentSeasonSelected] ?: emptyList()
                                                val allProgressList by viewModel.allPlaybackProgressList.collectAsState()
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                                ) {
                                                    items(listEpisodes) { episode ->
                                                         val allProgressList by viewModel.allPlaybackProgressList.collectAsState()
                                                         val episodeIdInt = remember(episode.id) { episode.id.toIntOrNull() ?: 0 }
                                                         val epProgress = remember(allProgressList, episodeIdInt) {
                                                             allProgressList.find { it.id == episodeIdInt }
                                                         }
                                                         val epPercentage = remember(epProgress) {
                                                             if (epProgress != null && epProgress.durationMs > 0) {
                                                                 ((epProgress.positionMs * 100) / epProgress.durationMs).toInt().coerceIn(0, 100)
                                                             } else {
                                                                 0
                                                             }
                                                         }
                                                        val epInteractionSource = remember { MutableInteractionSource() }
                                                        val isFocusedEp by epInteractionSource.collectIsFocusedAsState()

                                                        Card(
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isFocusedEp) Color(0xFF1D0A0F) else Color(0xFF0F0608)
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(64.dp)
                                                                .clickable(
                                                                    interactionSource = epInteractionSource,
                                                                    indication = null,
                                                                    onClick = {
                                                                        val epStream = XtreamStream(
                                                                            streamId = episode.id.toIntOrNull() ?: 0,
                                                                            name = "${series.name} - S${currentSeasonSelected}E${episode.episodeNum ?: 1}: ${episode.title}",
                                                                            streamIcon = series.icon,
                                                                            categoryId = series.categoryId,
                                                                            containerExtension = episode.containerExtension
                                                                        )
                                                                        onChannelSelected(epStream)
                                                                        selectedSeriesForEpisodes = null
                                                                    }
                                                                )
                                                                .focusable(interactionSource = epInteractionSource)
                                                                .tvFocusableBorder(
                                                                    interactionSource = epInteractionSource,
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    focusedBorderColor = Color(0xFFEF4444),
                                                                    focusedScale = 1.02f,
                                                                    focusable = false
                                                                )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(horizontal = 16.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(0.85f)) {
                                                                    Text(
                                                                        text = "Episode ${episode.episodeNum ?: 1}: ${episode.title}",
                                                                        color = Color.White,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    Spacer(modifier = Modifier.height(2.dp))
                                                                    if (!episode.containerExtension.isNullOrBlank()) {
                                                                        Text(
                                                                            text = if (epPercentage > 0) "FORMAT: ${episode.containerExtension.uppercase()}  •  $epPercentage% WATCHED (${formatMillis(epProgress!!.positionMs)} / ${formatMillis(epProgress.durationMs)})" else "FORMAT: ${episode.containerExtension.uppercase()}",
                                                                            color = Color.White.copy(alpha = 0.4f),
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Medium
                                                                        )
                                                                    }
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(28.dp)
                                                                        .background(Color(0xFFEF4444), RoundedCornerShape(14.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.PlayArrow,
                                                                        contentDescription = "Play Episode",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
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
fun TVSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .width(220.dp)
            .height(44.dp)
            .focusable(interactionSource = interactionSource)
            .tvFocusableBorder(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.025f,
                focusable = false
            )
            .border(
                width = 1.dp,
                color = if (isFocused) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isFocused) Color(0xFF14080B) else Color(0xFF0C0305),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Search by stream name...",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CategorySidebarList(
    categories: List<XtreamCategory>,
    selectedCategory: XtreamCategory?,
    onCategoryClick: (XtreamCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.categoryId }) { category ->
            val isSelected = category.categoryId == selectedCategory?.categoryId

            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            val backgroundColor = when {
                isSelected -> Color(0xFFEF4444)
                isFocused -> Color(0xFFEF4444).copy(alpha = 0.18f)
                else -> Color.Transparent
            }
            val textColor = when {
                isSelected -> Color.White
                isFocused -> Color.White
                else -> Color.White.copy(alpha = 0.55f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCategoryClick(category) }
                    )
                    .focusable(interactionSource = interactionSource)
                    .border(
                        border = when {
                            isFocused -> BorderStroke(2.dp, Color.White)
                            isSelected -> BorderStroke(1.25.dp, Color.White.copy(alpha = 0.5f))
                            else -> BorderStroke(0.dp, Color.Transparent)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (category.categoryId) {
                        "-1" -> Icons.Default.Star
                        "-ALL" -> Icons.Default.List
                        "-RECENT" -> Icons.Default.Refresh
                        else -> Icons.Default.Folder
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.categoryName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHorizontalRow(
    categories: List<XtreamCategory>,
    selectedCategory: XtreamCategory?,
    onCategoryClick: (XtreamCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        items(categories, key = { it.categoryId }) { category ->
            val isSelected = category.categoryId == selectedCategory?.categoryId

            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            val backgroundColor = when {
                isSelected -> Color(0xFFEF4444)
                isFocused -> Color(0xFFEF4444).copy(alpha = 0.18f)
                else -> Color(0xFF100B0D)
            }
            val textColor = when {
                isSelected -> Color.White
                isFocused -> Color.White
                else -> Color.White.copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCategoryClick(category) }
                    )
                    .focusable(interactionSource = interactionSource)
                    .border(
                        border = when {
                            isFocused -> BorderStroke(2.dp, Color.White)
                            isSelected -> BorderStroke(1.25.dp, Color.White.copy(alpha = 0.5f))
                            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (category.categoryId) {
                        "-1" -> Icons.Default.Star
                        "-ALL" -> Icons.Default.List
                        "-RECENT" -> Icons.Default.Refresh
                        else -> Icons.Default.Folder
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.categoryName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelGrid(
    streams: List<XtreamStream>,
    type: String,
    isFavoriteFlow: (Int) -> kotlinx.coroutines.flow.Flow<Boolean>,
    onFavoriteToggle: (XtreamStream) -> Unit,
    onStreamClick: (XtreamStream) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val columnsCount = if (isPortrait) {
        if (type == "live") 1 else 2
    } else {
        if (type == "live") 3 else 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(streams, key = { if (it.streamId > 0) "s_${it.streamId}" else "se_${it.id}" }) { stream ->
            val isFav by isFavoriteFlow(stream.id).collectAsState(initial = false)

            if (type == "live") {
                LiveTvCardItem(
                    stream = stream,
                    isFav = isFav,
                    onFavoriteToggle = { onFavoriteToggle(stream) },
                    onClick = { onStreamClick(stream) }
                )
            } else {
                PosterGridCardItem(
                    stream = stream,
                    isFav = isFav,
                    onFavoriteToggle = { onFavoriteToggle(stream) },
                    onClick = { onStreamClick(stream) }
                )
            }
        }
    }
}

@Composable
fun LiveTvCardItem(
    stream: XtreamStream,
    isFav: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth scaling transition for the card container (1.10x zoom on focus)
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F080A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(
                border = if (isFocused) {
                    BorderStroke(3.dp, Color.White)
                } else {
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Brush.verticalGradient(listOf(Color(0xFF281014), Color(0xFF130608)))),
                contentAlignment = Alignment.Center
            ) {
                // Heart favorite badge
                if (isFav) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!stream.icon.isNullOrBlank()) {
                        AsyncImage(
                            model = stream.icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        val initials = if (stream.name.length >= 2) stream.name.take(2).uppercase() else "TV"
                        Text(text = initials, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Text Label & Live Progress Simulation Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stream.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Percentage tracker simulated progress
                val progressOffset = remember(stream.id) { (stream.id * 17) % 55 + 20 }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressOffset / 100f)
                                .fillMaxHeight()
                                .background(Color(0xFFEF4444), RoundedCornerShape(1.dp))
                        )
                    }
                    Text(text = "LIVE", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun PosterGridCardItem(
    stream: XtreamStream,
    isFav: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth scaling transition for the card container (1.10x zoom on focus)
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    // Smooth scaling transition for the image itself inside (1.15x zoom inside on focus)
    val imageScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "image_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F080A))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(
                border = if (isFocused) {
                    BorderStroke(3.dp, Color.White)
                } else {
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = stream.icon,
            contentDescription = stream.name,
            modifier = Modifier
                .fillMaxSize()
                .scale(imageScale),
            contentScale = ContentScale.Crop
        )

        // Backdrop visual cinematic gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Badges overlays
        if (isFav) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }

        // Title and year (Simulated) labels at the bottom of the poster
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = stream.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ULTRA HD (PRO)",
                color = Color(0xFFEF4444),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun IndicatorBadge(label: String, actionDesc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(text = actionDesc, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IconButtonWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundCol = if (isFocused) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color(0xFF1E293B)
    }

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundCol)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .border(
                border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

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
