package com.example.data.repository

import com.example.data.local.XtreamPrefs
import com.example.data.remote.XtreamApi
import com.example.domain.model.XtreamCategory
import com.example.domain.model.XtreamLoginResponse
import com.example.domain.model.XtreamServer
import com.example.domain.model.XtreamStream
import com.example.domain.model.XtreamSeriesInfoResponse
import com.example.domain.repository.XtreamRepository
import com.example.data.local.database.FavoriteEntity
import com.example.data.local.database.PlaybackProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class XtreamRepositoryImpl(
    private val api: XtreamApi,
    private val prefs: XtreamPrefs,
    private val mediaDao: com.example.data.local.database.MediaDao
) : XtreamRepository {

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        playlistName: String
    ): Result<XtreamLoginResponse> = withContext(Dispatchers.IO) {
        try {
            var formattedUrl = serverUrl.trim()
            if (formattedUrl.equals("demo_mode", ignoreCase = true) || username.trim().equals("demo", ignoreCase = true)) {
                val mockResponse = XtreamLoginResponse(
                    userInfo = com.example.domain.model.XtreamUserInfo(
                        username = "DemoUser",
                        status = "Active",
                        auth = 1,
                        expDate = "Unlimited",
                        allowedOutputFormats = listOf("m3u8", "ts", "mp4")
                    ),
                    serverInfo = com.example.domain.model.XtreamServerInfo(
                        url = "demo_mode",
                        port = "80",
                        timezone = "UTC"
                    )
                )
                prefs.saveServer(XtreamServer("demo_mode", "demo", "demo", playlistName.ifBlank { "Demo Portal" }))
                prefs.saveExpiryDate("Unlimited")
                return@withContext Result.success(mockResponse)
            }

            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "http://$formattedUrl"
            }
            val cleanUrl = formatApiUrl(formattedUrl)
            val response = api.login(cleanUrl, username, password)
            
            if (response.userInfo != null) {
                // Save the server details dynamically upon successful login verification
                prefs.saveServer(XtreamServer(formattedUrl, username, password, playlistName))
                prefs.saveExpiryDate(response.userInfo.expDate)
                Result.success(response)
            } else {
                Result.failure(Exception("Authentication failed: Server returned empty user info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(type: String): Result<List<XtreamCategory>> = withContext(Dispatchers.IO) {
        try {
            val server = getSavedServer() ?: return@withContext Result.failure(Exception("No active Xtream server configured"))
            if (server.serverUrl.equals("demo_mode", ignoreCase = true) || server.username.equals("demo", ignoreCase = true)) {
                val list = when (type) {
                    "movies" -> listOf(
                        XtreamCategory("10", "🔥 Action & Adventure"),
                        XtreamCategory("11", "🎭 Comedy Specials"),
                        XtreamCategory("12", "🚀 Science Fiction")
                    )
                    "series" -> listOf(
                        XtreamCategory("20", "🎬 Arabic Dramas"),
                        XtreamCategory("21", "🕵️ Mystery & Suspense")
                    )
                    else -> listOf(
                        XtreamCategory("1", "⚽ Bein Sports (Premium)"),
                        XtreamCategory("2", "📺 Arabic Entertainment"),
                        XtreamCategory("3", "📰 News Channels"),
                        XtreamCategory("4", "🎨 Kids & Animation")
                    )
                }
                return@withContext Result.success(list)
            }

            val cleanUrl = formatApiUrl(server.serverUrl)
            val action = when (type) {
                "movies" -> "get_vod_categories"
                "series" -> "get_series_categories"
                else -> "get_live_categories"
            }
            val categories = api.getCategories(cleanUrl, server.username, server.password, action = action)
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStreams(categoryId: String, type: String): Result<List<XtreamStream>> = withContext(Dispatchers.IO) {
        try {
            val server = getSavedServer() ?: return@withContext Result.failure(Exception("No active Xtream server configured"))
            if (server.serverUrl.equals("demo_mode", ignoreCase = true) || server.username.equals("demo", ignoreCase = true)) {
                val list = when (type) {
                    "movies" -> when (categoryId) {
                        "-ALL" -> listOf(
                            XtreamStream(num = 1, name = "Tears of Steel (Sci-Fi Action)", streamId = 100, cover = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300&auto=format&fit=crop", plot = "In a dystopian future, a group of scientists try to save the world from giant rogue robots.", rating = "8.2", releaseDate = "2012-09-27", containerExtension = "mp4"),
                            XtreamStream(num = 2, name = "Sintel (Epic Fantasy)", streamId = 101, cover = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300&auto=format&fit=crop", plot = "A lonely young woman named Sintel befriends a baby dragon.", rating = "7.9", releaseDate = "2010-09-27", containerExtension = "mp4"),
                            XtreamStream(num = 3, name = "Big Buck Bunny (Animation Comedy)", streamId = 102, cover = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?q=80&w=300&auto=format&fit=crop", plot = "A giant, friendly rabbit decides to take sweet, comedic revenge on three mischievous pests.", rating = "8.5", releaseDate = "2008-05-10", containerExtension = "mp4"),
                            XtreamStream(num = 5, name = "Cosmic Journey", streamId = 103, cover = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=300&auto=format&fit=crop", plot = "Deep space travel exploration beyond the solar system highlights the beauties of the galaxy.", rating = "9.0", releaseDate = "2024", containerExtension = "mp4")
                        )
                        "10" -> listOf(
                            XtreamStream(num = 1, name = "Tears of Steel (Sci-Fi Action)", streamId = 100, cover = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300&auto=format&fit=crop", plot = "In a dystopian future, a group of scientists try to save the world from giant rogue robots.", rating = "8.2", releaseDate = "2012-09-27", containerExtension = "mp4"),
                            XtreamStream(num = 2, name = "Sintel (Epic Fantasy)", streamId = 101, cover = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300&auto=format&fit=crop", plot = "A lonely young woman named Sintel befriends a baby dragon.", rating = "7.9", releaseDate = "2010-09-27", containerExtension = "mp4")
                        )
                        "11" -> listOf(
                            XtreamStream(num = 3, name = "Big Buck Bunny (Animation Comedy)", streamId = 102, cover = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?q=80&w=300&auto=format&fit=crop", plot = "A giant, friendly rabbit decides to take sweet, comedic revenge on three mischievous pests.", rating = "8.5", releaseDate = "2008-05-10", containerExtension = "mp4")
                        )
                        "12" -> listOf(
                            XtreamStream(num = 2, name = "Sintel (Epic Fantasy)", streamId = 101, cover = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300&auto=format&fit=crop", plot = "A lonely young woman named Sintel befriends a baby dragon.", rating = "7.9", releaseDate = "2010-09-27", containerExtension = "mp4"),
                            XtreamStream(num = 5, name = "Cosmic Journey", streamId = 103, cover = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=300&auto=format&fit=crop", plot = "Deep space travel exploration beyond the solar system highlights the beauties of the galaxy.", rating = "9.0", releaseDate = "2024", containerExtension = "mp4")
                        )
                        else -> listOf(
                            XtreamStream(num = 1, name = "Tears of Steel (Sci-Fi Action)", streamId = 100, cover = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300&auto=format&fit=crop", containerExtension = "mp4"),
                            XtreamStream(num = 2, name = "Sintel (Epic Fantasy)", streamId = 101, cover = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300&auto=format&fit=crop", containerExtension = "mp4"),
                            XtreamStream(num = 3, name = "Big Buck Bunny (Animation Comedy)", streamId = 102, cover = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?q=80&w=300&auto=format&fit=crop", containerExtension = "mp4")
                        )
                    }
                    "series" -> when (categoryId) {
                        "-ALL" -> listOf(
                            XtreamStream(num = 1, name = "The Golden Era (العصر الذهبي)", seriesId = 201, cover = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?q=80&w=300&auto=format&fit=crop", plot = "An ancient history tale of honor, family bonds, and the struggle for leadership in the golden lands.", rating = "8.9", releaseDate = "2023", containerExtension = "mp4"),
                            XtreamStream(num = 2, name = "Shadow Tracker (تعقب الظلال)", seriesId = 202, cover = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?q=80&w=300&auto=format&fit=crop", plot = "A high-stakes thriller where detective Omar tries to solve mysterious digital clues.", rating = "9.1", releaseDate = "2024", containerExtension = "mp4")
                        )
                        "20" -> listOf(
                            XtreamStream(num = 1, name = "The Golden Era (العصر الذهبي)", seriesId = 201, cover = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?q=80&w=300&auto=format&fit=crop", plot = "An ancient history tale of honor, family bonds, and the struggle for leadership in the golden lands.", rating = "8.9", releaseDate = "2023", containerExtension = "mp4")
                        )
                        "21" -> listOf(
                            XtreamStream(num = 2, name = "Shadow Tracker (تعقب الظلال)", seriesId = 202, cover = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?q=80&w=300&auto=format&fit=crop", plot = "A high-stakes thriller where detective Omar tries to solve mysterious digital clues.", rating = "9.1", releaseDate = "2024", containerExtension = "mp4")
                        )
                        else -> listOf(
                            XtreamStream(num = 1, name = "The Golden Era (العصر الذهبي)", seriesId = 201, cover = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?q=80&w=300&auto=format&fit=crop", containerExtension = "mp4"),
                            XtreamStream(num = 2, name = "Shadow Tracker (تعقب الظلال)", seriesId = 202, cover = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?q=80&w=300&auto=format&fit=crop", containerExtension = "mp4")
                        )
                    }
                    else -> when (categoryId) {
                        "-ALL" -> listOf(
                            XtreamStream(num = 1, name = "Bein Sports 1 HD", streamId = 1, streamIcon = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 2, name = "Bein Sports 2 HD", streamId = 2, streamIcon = "https://images.unsplash.com/photo-1540747737956-378724044432?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 3, name = "MBC 1 HD", streamId = 3, streamIcon = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 4, name = "Rotana Cinema", streamId = 4, streamIcon = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 5, name = "Al Jazeera Arabic", streamId = 5, streamIcon = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 6, name = "Al Arabiya News", streamId = 6, streamIcon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 7, name = "Space Toon Channel", streamId = 7, streamIcon = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 8, name = "Disney Arabic", streamId = 8, streamIcon = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=200&auto=format&fit=crop")
                        )
                        "1" -> listOf(
                            XtreamStream(num = 1, name = "Bein Sports 1 HD", streamId = 1, streamIcon = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 2, name = "Bein Sports 2 HD", streamId = 2, streamIcon = "https://images.unsplash.com/photo-1540747737956-378724044432?q=80&w=200&auto=format&fit=crop")
                        )
                        "2" -> listOf(
                            XtreamStream(num = 3, name = "MBC 1 HD", streamId = 3, streamIcon = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 4, name = "Rotana Cinema", streamId = 4, streamIcon = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=200&auto=format&fit=crop")
                        )
                        "3" -> listOf(
                            XtreamStream(num = 5, name = "Al Jazeera Arabic", streamId = 5, streamIcon = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 6, name = "Al Arabiya News", streamId = 6, streamIcon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?q=80&w=200&auto=format&fit=crop")
                        )
                        "4" -> listOf(
                            XtreamStream(num = 7, name = "Space Toon Channel", streamId = 7, streamIcon = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 8, name = "Disney Arabic", streamId = 8, streamIcon = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=200&auto=format&fit=crop")
                        )
                        else -> listOf(
                            XtreamStream(num = 1, name = "Bein Sports 1 HD", streamId = 1, streamIcon = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 2, name = "Bein Sports 2 HD", streamId = 2, streamIcon = "https://images.unsplash.com/photo-1540747737956-378724044432?q=80&w=200&auto=format&fit=crop"),
                            XtreamStream(num = 3, name = "MBC 1 HD", streamId = 3, streamIcon = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?q=80&w=200&auto=format&fit=crop")
                        )
                    }
                }
                return@withContext Result.success(list)
            }

            val cleanUrl = formatApiUrl(server.serverUrl)
            val action = when (type) {
                "movies" -> "get_vod_streams"
                "series" -> "get_series"
                else -> "get_live_streams"
            }
            val apiCategoryId = if (categoryId == "-ALL") null else categoryId
            val streams = api.getStreams(cleanUrl, server.username, server.password, action = action, categoryId = apiCategoryId)
            Result.success(streams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSeriesInfo(seriesId: Int): Result<XtreamSeriesInfoResponse> = withContext(Dispatchers.IO) {
        try {
            val server = getSavedServer() ?: return@withContext Result.failure(Exception("No active Xtream server configured"))
            if (server.serverUrl.equals("demo_mode", ignoreCase = true) || server.username.equals("demo", ignoreCase = true)) {
                val info = if (seriesId == 201) {
                    com.example.domain.model.XtreamSeriesInfo(
                        name = "The Golden Era (العصر الذهبي)",
                        cover = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?q=80&w=300&auto=format&fit=crop",
                        plot = "An ancient history tale of honor, family bonds, and the struggle for leadership in the golden lands.",
                        rating = "8.9",
                        releaseDate = "2023",
                        genre = "Drama, History",
                        director = "Ahmed Aly",
                        cast = "Mona Zaki, Karim Abdel Aziz"
                    )
                } else {
                    com.example.domain.model.XtreamSeriesInfo(
                        name = "Shadow Tracker (تعقب الظلال)",
                        cover = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?q=80&w=300&auto=format&fit=crop",
                        plot = "A high-stakes thriller where detective Omar tries to solve mysterious digital clues.",
                        rating = "9.1",
                        releaseDate = "2024",
                        genre = "Mystery, Suspense",
                        director = "Youssef Sherif",
                        cast = "Amir Karara, Nelly Karim"
                    )
                }

                val episodes = if (seriesId == 201) {
                    mapOf(
                        "1" to listOf(
                            com.example.domain.model.XtreamEpisode(id = "20101", episodeNum = 1, title = "Episode 1 - The Awakening (استيقاظ)", containerExtension = "mp4"),
                            com.example.domain.model.XtreamEpisode(id = "20102", episodeNum = 2, title = "Episode 2 - The Alliance (التحالف)", containerExtension = "mp4"),
                            com.example.domain.model.XtreamEpisode(id = "20103", episodeNum = 3, title = "Episode 3 - The Red Sea (البحر الأحمر)", containerExtension = "mp4")
                        )
                    )
                } else {
                    mapOf(
                        "1" to listOf(
                            com.example.domain.model.XtreamEpisode(id = "21001", episodeNum = 1, title = "Episode 1 - Cryptic Signals (إشارات غامضة)", containerExtension = "mp4"),
                            com.example.domain.model.XtreamEpisode(id = "21002", episodeNum = 2, title = "Episode 2 - Double Agent (العميل المزدوج)", containerExtension = "mp4")
                        )
                    )
                }

                return@withContext Result.success(XtreamSeriesInfoResponse(info = info, episodes = episodes))
            }

            val cleanUrl = formatApiUrl(server.serverUrl)
            val seriesInfo = api.getSeriesInfo(cleanUrl, server.username, server.password, seriesId = seriesId)
            Result.success(seriesInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedServer(): XtreamServer? {
        return prefs.getSavedServer()
    }

    override fun saveServer(server: XtreamServer) {
        prefs.saveServer(server)
    }

    override fun clearSavedServer() {
        prefs.clear()
    }

    override fun getExpiryDate(): String? {
        return prefs.getExpiryDate()
    }

    override fun getStreamUrl(streamId: Int, type: String, containerExtension: String?): String {
        val server = getSavedServer() ?: return ""
        if (server.serverUrl.equals("demo_mode", ignoreCase = true) || server.username.equals("demo", ignoreCase = true)) {
            return when (streamId) {
                // True HLS live streams for Live TV Channels
                1 -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                2 -> "https://test-streams.mux.dev/pts_spg/pts_spg.m3u8"
                3 -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                4 -> "https://test-streams.mux.dev/pts_spg/pts_spg.m3u8"
                5 -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                6 -> "https://test-streams.mux.dev/pts_spg/pts_spg.m3u8"
                7 -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                8 -> "https://test-streams.mux.dev/pts_spg/pts_spg.m3u8"
                
                // Movies with modernized storage.googleapis.com endpoints preventing SNI/SSL validation bugs
                100 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                101 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                102 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                103 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
                
                // Series episodes with modernized TLS endpoints
                20101 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                20102 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                20103 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                21001 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
                21002 -> "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                
                else -> "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            }
        }

        val baseUrl = if (server.serverUrl.endsWith("/")) server.serverUrl else "${server.serverUrl}/"
        return when (type) {
            "movies" -> {
                val ext = containerExtension ?: "mp4"
                "${baseUrl}movie/${server.username}/${server.password}/$streamId.$ext"
            }
            "series" -> {
                val ext = containerExtension ?: "mp4"
                "${baseUrl}series/${server.username}/${server.password}/$streamId.$ext"
            }
            else -> {
                "${baseUrl}live/${server.username}/${server.password}/$streamId.ts"
            }
        }
    }

    // Favorites & Playback Progress Implementation
    override fun getFavorites(type: String): Flow<List<FavoriteEntity>> {
        return mediaDao.getFavoritesByType(type)
    }

    override fun isFavorite(id: Int): Flow<Boolean> {
        return mediaDao.isFavorite(id)
    }

    override suspend fun toggleFavorite(
        id: Int,
        name: String,
        streamIcon: String?,
        categoryId: String?,
        type: String,
        containerExtension: String?
    ) = withContext(Dispatchers.IO) {
        if (mediaDao.isFavoriteSync(id)) {
            mediaDao.deleteFavorite(id)
        } else {
            mediaDao.insertFavorite(
                FavoriteEntity(
                    id = id,
                    name = name,
                    streamIcon = streamIcon,
                    categoryId = categoryId,
                    type = type,
                    containerExtension = containerExtension
                )
            )
        }
    }

    override fun getContinueWatching(): Flow<List<PlaybackProgressEntity>> {
        return mediaDao.getAllPlaybackProgress()
    }

    override suspend fun getPlaybackProgress(id: Int): PlaybackProgressEntity? = withContext(Dispatchers.IO) {
        return@withContext mediaDao.getPlaybackProgress(id)
    }

    override suspend fun savePlaybackProgress(
        id: Int,
        name: String,
        streamIcon: String?,
        type: String,
        containerExtension: String?,
        positionMs: Long,
        durationMs: Long,
        seriesId: Int?,
        season: String?,
        episodeNum: Int?,
        episodeTitle: String?
    ) = withContext(Dispatchers.IO) {
        mediaDao.insertPlaybackProgress(
            PlaybackProgressEntity(
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
                episodeTitle = episodeTitle,
                lastAccessed = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePlaybackProgress(id: Int) = withContext(Dispatchers.IO) {
        mediaDao.deletePlaybackProgress(id)
    }

    private fun formatApiUrl(serverUrl: String): String {
        var url = serverUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        val baseUrl = if (url.endsWith("/")) url else "$url/"
        return "${baseUrl}player_api.php"
    }
}
