package com.example.core.di

import android.content.Context
import com.example.data.local.XtreamPrefs
import com.example.data.remote.XtreamApi
import com.example.data.repository.XtreamRepositoryImpl
import com.example.domain.repository.XtreamRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val xtreamRepository: XtreamRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(requestWithUserAgent)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/") // Dynamic URLs are passed in using Retrofit's @Url
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val xtreamApi: XtreamApi by lazy {
        retrofit.create(XtreamApi::class.java)
    }

    private val xtreamPrefs: XtreamPrefs by lazy {
        XtreamPrefs(context)
    }

    private val appDatabase: com.example.data.local.database.AppDatabase by lazy {
        com.example.data.local.database.AppDatabase.getDatabase(context)
    }

    override val xtreamRepository: XtreamRepository by lazy {
        XtreamRepositoryImpl(
            api = xtreamApi,
            prefs = xtreamPrefs,
            mediaDao = appDatabase.mediaDao()
        )
    }
}
