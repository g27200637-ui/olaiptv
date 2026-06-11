package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.ui.ViewModelFactory
import com.example.feature_auth.AuthScreen
import com.example.feature_auth.AuthUiState
import com.example.feature_auth.AuthViewModel
import com.example.feature_channels.ChannelsScreen
import com.example.feature_channels.ChannelsViewModel
import com.example.feature_player.PlayerScreen
import com.example.feature_player.PlayerViewModel
import com.example.feature_dashboard.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

sealed interface Screen {
    object Auth : Screen
    object Dashboard : Screen
    data class Channels(val type: String, val categoryId: String? = null) : Screen
    data class Player(val stream: com.example.domain.model.XtreamStream, val type: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Resolve early native splash screen handling
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Resolve application repository container
        val appRepository = (application as XtreamApplication).container.xtreamRepository

        setContent {
            MyApplicationTheme {
                // Initialize ViewModels with the custom ViewModelFactory
                val authViewModel: AuthViewModel = viewModel(
                    factory = ViewModelFactory(appRepository)
                )
                val dashboardViewModel: com.example.feature_dashboard.DashboardViewModel = viewModel(
                    factory = ViewModelFactory(appRepository)
                )
                val channelsViewModel: ChannelsViewModel = viewModel(
                    factory = ViewModelFactory(appRepository)
                )
                val playerViewModel: PlayerViewModel = viewModel(
                    factory = ViewModelFactory(appRepository)
                )

                val savedServer by authViewModel.savedServer.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }

                // Splash control states
                var isSplashShowing by remember { mutableStateOf(true) }
                var isAuthVerified by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    // 1. Kick off background verification
                    authViewModel.verifySavedCredentials { success ->
                        isAuthVerified = success
                        if (success) {
                            dashboardViewModel.loadDashboardData()
                        }
                    }

                    // 2. Minimum display time of logo splash screen (1-2 seconds)
                    delay(1500)

                    // 3. Coordinate both operations to wait for slowest
                    while (isAuthVerified == null) {
                        delay(100)
                    }

                    // 4. Update the current active view screen and dismiss splash
                    if (isAuthVerified == true) {
                        currentScreen = Screen.Dashboard
                    } else {
                        currentScreen = Screen.Auth
                    }
                    isSplashShowing = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF060103) // High-end dark cinematic canvas representation
                ) {
                    if (isSplashShowing) {
                        AnimatedSplashScreen()
                    } else {
                        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                            when (screen) {
                                is Screen.Auth -> {
                                    AuthScreen(
                                        viewModel = authViewModel,
                                        onLoginSuccess = {
                                            currentScreen = Screen.Dashboard
                                            dashboardViewModel.loadDashboardData()
                                        }
                                    )
                                }
                                is Screen.Dashboard -> {
                                    DashboardScreen(
                                        viewModel = dashboardViewModel,
                                        playlistName = savedServer?.playlistName ?: "Active IPTV",
                                        serverInfo = savedServer,
                                        onLiveTvClick = {
                                            currentScreen = Screen.Channels("live")
                                        },
                                        onMoviesClick = {
                                            currentScreen = Screen.Channels("movies")
                                        },
                                        onSeriesClick = {
                                            currentScreen = Screen.Channels("series")
                                        },
                                        onCategoryClick = { type, categoryId ->
                                            currentScreen = Screen.Channels(type, categoryId)
                                        },
                                        onContinueWatchingClick = { resumeItem ->
                                            val stream = com.example.domain.model.XtreamStream(
                                                name = resumeItem.name,
                                                streamId = if (resumeItem.type == "series") 0 else resumeItem.id,
                                                seriesId = if (resumeItem.type == "series") resumeItem.id else null,
                                                streamIcon = resumeItem.streamIcon,
                                                containerExtension = resumeItem.containerExtension
                                            )
                                            currentScreen = Screen.Player(stream, resumeItem.type)
                                        },
                                        onChangeServerClick = {
                                            authViewModel.logout()
                                            currentScreen = Screen.Auth
                                        },
                                        onExitClick = {
                                            finish()
                                        }
                                    )
                                }
                                is Screen.Channels -> {
                                    ChannelsScreen(
                                        viewModel = channelsViewModel,
                                        type = screen.type,
                                        initialCategoryId = screen.categoryId,
                                        onChannelSelected = { selectedStream ->
                                            currentScreen = Screen.Player(selectedStream, screen.type)
                                        },
                                        onBack = {
                                            currentScreen = Screen.Dashboard
                                        }
                                    )
                                }
                                is Screen.Player -> {
                                    PlayerScreen(
                                        viewModel = playerViewModel,
                                        stream = screen.stream,
                                        type = screen.type,
                                        onBack = {
                                            currentScreen = Screen.Channels(screen.type)
                                        }
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

@Composable
fun AnimatedSplashScreen() {
    var animateStart by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (animateStart) 1.05f else 0.85f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "logo_scale"
    )
    
    LaunchedEffect(Unit) {
        animateStart = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060103)),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Orb
        Box(
            modifier = Modifier
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE50914).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.scale(scale)
        ) {
            // Elegant Vector Icon Play Crest (matching Dashboard design)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF2E54), Color(0xFFC40024))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp).padding(start = 4.dp)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "OLA IPTV",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "PREMIUM ENTERTAINMENT PORTAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
