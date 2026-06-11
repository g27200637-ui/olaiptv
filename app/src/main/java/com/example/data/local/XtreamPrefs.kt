package com.example.data.local

import android.content.Context
import android.util.Base64
import com.example.domain.model.XtreamServer
import java.nio.charset.StandardCharsets

class XtreamPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("xtream_iptv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_ENC = "password_enc" // Simple obfuscated/encoded password
        private const val KEY_PLAYLIST_NAME = "playlist_name"
        private const val KEY_EXP_DATE = "exp_date"
    }

    fun getExpiryDate(): String? {
        return prefs.getString(KEY_EXP_DATE, null)
    }

    fun saveExpiryDate(expDate: String?) {
        prefs.edit().putString(KEY_EXP_DATE, expDate).apply()
    }

    fun getSavedServer(): XtreamServer? {
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val encPassword = prefs.getString(KEY_PASSWORD_ENC, null) ?: return null
        val playlistName = prefs.getString(KEY_PLAYLIST_NAME, "My Playlist") ?: "My Playlist"
        
        val password = try {
            val decodedBytes = Base64.decode(encPassword, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
        
        return XtreamServer(url, username, password, playlistName)
    }

    fun saveServer(server: XtreamServer) {
        val encPassword = try {
            val bytes = server.password.toByteArray(StandardCharsets.UTF_8)
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
        
        prefs.edit()
            .putString(KEY_SERVER_URL, server.serverUrl)
            .putString(KEY_USERNAME, server.username)
            .putString(KEY_PASSWORD_ENC, encPassword)
            .putString(KEY_PLAYLIST_NAME, server.playlistName)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
