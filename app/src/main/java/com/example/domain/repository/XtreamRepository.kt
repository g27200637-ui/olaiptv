package com.example.domain.repository

import com.example.domain.model.XtreamCategory
import com.example.domain.model.XtreamLoginResponse
import com.example.domain.model.XtreamServer
import com.example.domain.model.XtreamStream
import com.example.domain.model.XtreamSeriesInfoResponse
import com.example.data.local.database.FavoriteEntity
import com.example.data.local.database.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

interface XtreamRepository {
    suspend fun login(serverUrl: String, username: String, password: String, playlistName: String = "My Playlist"): Result<XtreamLoginResponse>
    suspend fun getCategories(type: String = "live"): Result<List<XtreamCategory>>
    suspend fun getStreams(categoryId: String, type: String = "live"): Result<List<XtreamStream>>
    suspend fun getSeriesInfo(seriesId: Int): Result<XtreamSeriesInfoResponse>
    fun getSavedServer(): XtreamServer?
    fun saveServer(server: XtreamServer)
    fun clearSavedServer()
    fun getExpiryDate(): String?
    fun getStreamUrl(streamId: Int, type: String = "live", containerExtension: String? = null): String

    // Favorites & Playback Progress Database Support
    fun getFavorites(type: String): Flow<List<FavoriteEntity>>
    fun isFavorite(id: Int): Flow<Boolean>
    suspend fun toggleFavorite(
        id: Int,
        name: String,
        streamIcon: String?,
        categoryId: String?,
        type: String,
        containerExtension: String?
    )
    fun getContinueWatching(): Flow<List<PlaybackProgressEntity>>
    suspend fun getPlaybackProgress(id: Int): PlaybackProgressEntity?
    suspend fun savePlaybackProgress(
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
    )
    suspend fun deletePlaybackProgress(id: Int)
}
