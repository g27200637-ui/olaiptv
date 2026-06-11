package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.model.XtreamLoginResponse
import com.example.domain.model.XtreamServer
import com.example.domain.repository.XtreamRepository
import com.example.feature_auth.AuthScreen
import com.example.feature_auth.AuthViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun auth_screen_screenshot() {
    val mockRepository = object : XtreamRepository {
        override suspend fun login(serverUrl: String, username: String, password: String, playlistName: String) = Result.success(XtreamLoginResponse())
        override suspend fun getCategories(type: String): Result<List<com.example.domain.model.XtreamCategory>> = Result.success(emptyList())
        override suspend fun getStreams(categoryId: String, type: String): Result<List<com.example.domain.model.XtreamStream>> = Result.success(emptyList())
        override suspend fun getSeriesInfo(seriesId: Int) = Result.success(com.example.domain.model.XtreamSeriesInfoResponse(null))
        override fun getSavedServer(): XtreamServer? = null
        override fun saveServer(server: XtreamServer) {}
        override fun clearSavedServer() {}
        override fun getStreamUrl(streamId: Int, type: String, containerExtension: String?) = ""
        
        // Stub Room functions for mockRepository
        override fun getFavorites(type: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.database.FavoriteEntity>())
        override fun isFavorite(id: Int) = kotlinx.coroutines.flow.flowOf(false)
        override suspend fun toggleFavorite(id: Int, name: String, streamIcon: String?, categoryId: String?, type: String, containerExtension: String?) {}
        override fun getContinueWatching() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.database.PlaybackProgressEntity>())
        override suspend fun getPlaybackProgress(id: Int) = null
        override suspend fun savePlaybackProgress(id: Int, name: String, streamIcon: String?, type: String, containerExtension: String?, positionMs: Long, durationMs: Long, seriesId: Int?, season: String?, episodeNum: Int?, episodeTitle: String?) {}
        override suspend fun deletePlaybackProgress(id: Int) {}
    }
    
    composeTestRule.setContent { 
        MyApplicationTheme { 
            AuthScreen(
                viewModel = AuthViewModel(mockRepository),
                onLoginSuccess = {}
            )
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
