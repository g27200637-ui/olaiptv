package com.example.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class XtreamServer(
    val serverUrl: String,
    val username: String,
    val password: String,
    val playlistName: String = "My Playlist"
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "parent_id") val parentId: Int = 0
)

@JsonClass(generateAdapter = true)
data class XtreamStream(
    @Json(name = "num") val num: Int? = null,
    @Json(name = "name") val name: String,
    @Json(name = "stream_id") val streamId: Int = 0,
    @Json(name = "series_id") val seriesId: Int? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "live") val live: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "director") val director: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null
) {
    val id: Int get() = if (streamId != 0) streamId else seriesId ?: 0
    val icon: String? get() = if (!streamIcon.isNullOrBlank()) streamIcon else if (!cover.isNullOrBlank()) cover else null
}

@JsonClass(generateAdapter = true)
data class XtreamEpisode(
    @Json(name = "id") val id: String,
    @Json(name = "episode_num") val episodeNum: Int? = null,
    @Json(name = "title") val title: String,
    @Json(name = "container_extension") val containerExtension: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesInfo(
    @Json(name = "name") val name: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "director") val director: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesInfoResponse(
    @Json(name = "info") val info: XtreamSeriesInfo? = null,
    @Json(name = "episodes") val episodes: Map<String, List<XtreamEpisode>>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamLoginResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "auth") val auth: Int? = null,
    @Json(name = "exp_date") val expDate: String? = null,
    @Json(name = "allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "https_port") val httpsPort: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "time_now") val timeNow: String? = null
)
