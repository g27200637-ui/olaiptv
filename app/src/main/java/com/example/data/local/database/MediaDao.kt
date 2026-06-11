package com.example.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // Favorites
    @Query("SELECT * FROM favorites WHERE type = :type")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavoriteSync(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: Int)

    // Playback Progress / Continue Watching
    @Query("SELECT * FROM playback_progress ORDER BY lastAccessed DESC")
    fun getAllPlaybackProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE id = :id")
    suspend fun getPlaybackProgress(id: Int): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE id = :id")
    suspend fun deletePlaybackProgress(id: Int)
}
