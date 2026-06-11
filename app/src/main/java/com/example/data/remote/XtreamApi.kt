package com.example.data.remote

import com.example.domain.model.XtreamCategory
import com.example.domain.model.XtreamLoginResponse
import com.example.domain.model.XtreamStream
import com.example.domain.model.XtreamSeriesInfoResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamApi {
    @GET
    suspend fun login(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamLoginResponse

    @GET
    suspend fun getCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String?
    ): List<XtreamStream>

    @GET
    suspend fun getSeriesInfo(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Int
    ): XtreamSeriesInfoResponse
}
