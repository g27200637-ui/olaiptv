package com.example.feature_player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: XtreamRepository
) : ViewModel() {

    private val _streamUrl = MutableStateFlow<String>("")
    val streamUrl = _streamUrl.asStateFlow()

    private val _reconnectTrigger = MutableStateFlow(0)
    val reconnectTrigger = _reconnectTrigger.asStateFlow()

    fun loadStream(streamId: Int, type: String = "live", containerExtension: String? = null) {
        val url = repository.getStreamUrl(streamId, type, containerExtension)
        _streamUrl.value = url
    }

    fun triggerReconnect() {
        _reconnectTrigger.value += 1
    }

    suspend fun getPlaybackProgress(id: Int) = repository.getPlaybackProgress(id)

    fun savePlaybackProgress(
        id: Int,
        name: String,
        streamIcon: String?,
        type: String,
        containerExtension: String?,
        positionMs: Long,
        durationMs: Long,
        seriesId: Int? = null,
        season: String? = null,
        episodeNum: Int? = null,
        episodeTitle: String? = null
    ) {
        viewModelScope.launch {
            repository.savePlaybackProgress(
                id = id,
                name = name,
                streamIcon = streamIcon,
                type = type,
                containerExtension = containerExtension,
                positionMs = positionMs,
                durationMs = durationMs,
                seriesId = seriesId,
                season = season,
                episodeNum = episodeNum,
                episodeTitle = episodeTitle
            )
        }
    }

    fun deletePlaybackProgress(id: Int) {
        viewModelScope.launch {
            repository.deletePlaybackProgress(id)
        }
    }

    fun getPlaylistName(): String {
        return repository.getSavedServer()?.playlistName ?: "My Playlist"
    }
}
