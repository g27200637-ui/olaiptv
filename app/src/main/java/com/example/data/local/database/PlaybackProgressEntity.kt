package com.example.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val id: Int, // streamId (for movie or episode)
    val name: String, // Display name (e.g., "Movie Name" or "Series - S01E02: Episode Title")
    val streamIcon: String?,
    val type: String, // "movies", "series"
    val containerExtension: String?,
    val positionMs: Long,
    val durationMs: Long,
    val lastAccessed: Long = System.currentTimeMillis(),
    val seriesId: Int? = null,
    val season: String? = null,
    val episodeNum: Int? = null,
    val episodeTitle: String? = null
)
