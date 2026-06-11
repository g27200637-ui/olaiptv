package com.example.feature_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.XtreamServer
import com.example.domain.repository.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val repository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _savedServer = MutableStateFlow<XtreamServer?>(null)
    val savedServer: StateFlow<XtreamServer?> = _savedServer.asStateFlow()

    init {
        checkSavedCredentials()
    }

    fun checkSavedCredentials() {
        val server = repository.getSavedServer()
        _savedServer.value = server
        if (server != null) {
            _uiState.value = AuthUiState.Success
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun verifySavedCredentials(onComplete: (Boolean) -> Unit) {
        val server = repository.getSavedServer()
        _savedServer.value = server
        if (server != null) {
            _uiState.value = AuthUiState.Loading
            viewModelScope.launch {
                val result = repository.login(
                    server.serverUrl,
                    server.username,
                    server.password,
                    server.playlistName
                )
                result.onSuccess {
                    _uiState.value = AuthUiState.Success
                    onComplete(true)
                }
                result.onFailure {
                    _uiState.value = AuthUiState.Error(it.localizedMessage ?: "Verification failed or connection timed out")
                    onComplete(false)
                }
            }
        } else {
            _uiState.value = AuthUiState.Idle
            onComplete(false)
        }
    }

    fun login(serverUrl: String, username: String, password: String, playlistName: String) {
        val url = serverUrl.trim()
        val user = username.trim()
        val pass = password.trim()
        val pName = playlistName.trim().ifEmpty { "My Playlist" }

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            _uiState.value = AuthUiState.Error("All fields must be filled out")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.login(url, user, pass, pName)
            result.onSuccess {
                _savedServer.value = XtreamServer(url, user, pass, pName)
                _uiState.value = AuthUiState.Success
            }
            result.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Invalid details or connection timeout")
            }
        }
    }

    fun logout() {
        repository.clearSavedServer()
        _savedServer.value = null
        _uiState.value = AuthUiState.Idle
    }
}
