package com.example.feature_channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.XtreamCategory
import com.example.domain.model.XtreamStream
import com.example.domain.model.XtreamSeriesInfoResponse
import com.example.domain.repository.XtreamRepository
import com.example.data.local.database.FavoriteEntity
import com.example.data.local.database.PlaybackProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(val categories: List<XtreamCategory>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

sealed interface StreamsUiState {
    object Idle : StreamsUiState
    object Loading : StreamsUiState
    data class Success(val streams: List<XtreamStream>) : StreamsUiState
    data class Error(val message: String) : StreamsUiState
}

class ChannelsViewModel(
    private val repository: XtreamRepository
) : ViewModel() {

    private val _categoriesState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val categoriesState: StateFlow<CategoriesUiState> = _categoriesState.asStateFlow()

    private val _streamsState = MutableStateFlow<StreamsUiState>(StreamsUiState.Idle)
    val streamsState: StateFlow<StreamsUiState> = _streamsState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<XtreamCategory?>(null)
    val selectedCategory: StateFlow<XtreamCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _favoritesList = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    val favoritesList: StateFlow<List<FavoriteEntity>> = _favoritesList.asStateFlow()

    private val _recentList = MutableStateFlow<List<PlaybackProgressEntity>>(emptyList())
    val recentList: StateFlow<List<PlaybackProgressEntity>> = _recentList.asStateFlow()

    val allPlaybackProgressList: StateFlow<List<PlaybackProgressEntity>> = repository.getContinueWatching()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    suspend fun getPlaybackProgress(id: Int): PlaybackProgressEntity? {
        return repository.getPlaybackProgress(id)
    }

    private var currentType: String = "live"

    fun loadCategories(type: String = "live") {
        currentType = type
        _categoriesState.value = CategoriesUiState.Loading
        _selectedCategory.value = null
        _searchQuery.value = ""
        
        viewModelScope.launch {
            // Collect favorites dynamically for local UI updates
            launch {
                repository.getFavorites(type).collect { list ->
                    _favoritesList.value = list
                    if (_selectedCategory.value?.categoryId == "-1") {
                        loadFavoritesIntoStreams(list)
                    }
                }
            }

            // Collect recently viewed (continue watching) dynamically for local UI updates
            launch {
                repository.getContinueWatching().collect { list ->
                    val filtered = list.filter { it.type == type }
                        .sortedByDescending { it.lastAccessed }
                    _recentList.value = filtered
                    if (_selectedCategory.value?.categoryId == "-RECENT") {
                        loadRecentIntoStreams(filtered)
                    }
                }
            }

            val result = repository.getCategories(type)
            result.onSuccess { list ->
                val allCategory = XtreamCategory(categoryId = "-ALL", categoryName = "All")
                val recentCategory = XtreamCategory(categoryId = "-RECENT", categoryName = "Recently Viewed")
                val favCategory = XtreamCategory(categoryId = "-1", categoryName = "Favorites")
                val combined = listOf(allCategory, recentCategory, favCategory) + list
                _categoriesState.value = CategoriesUiState.Success(combined)
                if (combined.isNotEmpty() && _selectedCategory.value == null) {
                    selectCategory(combined.first())
                }
            }
            result.onFailure { error ->
                _categoriesState.value = CategoriesUiState.Error(error.localizedMessage ?: "Failed to load categories")
            }
        }
    }

    fun selectCategory(category: XtreamCategory) {
        _selectedCategory.value = category
        when (category.categoryId) {
            "-1" -> loadFavoritesIntoStreams(_favoritesList.value)
            "-RECENT" -> loadRecentIntoStreams(_recentList.value)
            else -> loadStreams(category.categoryId)
        }
    }

    private fun loadFavoritesIntoStreams(favorites: List<FavoriteEntity>) {
        val streams = favorites.map { fav ->
            XtreamStream(
                name = fav.name,
                streamId = if (currentType == "series") 0 else fav.id,
                seriesId = if (currentType == "series") fav.id else null,
                streamIcon = fav.streamIcon,
                categoryId = fav.categoryId,
                containerExtension = fav.containerExtension
            )
        }
        _streamsState.value = StreamsUiState.Success(streams)
    }

    private fun loadRecentIntoStreams(recent: List<PlaybackProgressEntity>) {
        val streams = recent.map { rec ->
            XtreamStream(
                name = rec.name,
                streamId = if (currentType == "series") 0 else rec.id,
                seriesId = if (currentType == "series") rec.id else null,
                streamIcon = rec.streamIcon,
                categoryId = "-RECENT",
                containerExtension = rec.containerExtension
            )
        }
        _streamsState.value = StreamsUiState.Success(streams)
    }

    fun loadStreams(categoryId: String) {
        _streamsState.value = StreamsUiState.Loading
        viewModelScope.launch {
            val result = repository.getStreams(categoryId, currentType)
            result.onSuccess { list ->
                _streamsState.value = StreamsUiState.Success(list)
            }
            result.onFailure { error ->
                _streamsState.value = StreamsUiState.Error(error.localizedMessage ?: "Failed to load channels")
            }
        }
    }

    fun getSeriesInfo(seriesId: Int, onResult: (XtreamSeriesInfoResponse?) -> Unit) {
        viewModelScope.launch {
            val result = repository.getSeriesInfo(seriesId)
            result.onSuccess { info ->
                onResult(info)
            }.onFailure {
                onResult(null)
            }
        }
    }

    fun isStreamFavorite(id: Int): StateFlow<Boolean> {
        val state = MutableStateFlow(false)
        viewModelScope.launch {
            repository.isFavorite(id).collect {
                state.value = it
            }
        }
        return state
    }

    fun toggleFavorite(stream: XtreamStream) {
        viewModelScope.launch {
            repository.toggleFavorite(
                id = stream.id,
                name = stream.name,
                streamIcon = stream.icon,
                categoryId = stream.categoryId,
                type = currentType,
                containerExtension = stream.containerExtension
            )
        }
    }

    fun getCurrentType(): String = currentType

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getPlaylistName(): String {
        return repository.getSavedServer()?.playlistName ?: "My Playlist"
    }
}
