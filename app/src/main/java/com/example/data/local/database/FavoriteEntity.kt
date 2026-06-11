package com.example.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int, // streamId or seriesId
    val name: String,
    val streamIcon: String?,
    val categoryId: String?,
    val type: String, // "live", "movies", "series"
    val containerExtension: String? = null
)
