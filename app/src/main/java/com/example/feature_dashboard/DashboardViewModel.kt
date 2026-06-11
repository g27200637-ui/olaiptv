package com.example.feature_dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.XtreamCategory
import com.example.domain.repository.XtreamRepository
import com.example.data.local.database.PlaybackProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: XtreamRepository
) : ViewModel() {

    val continueWatching = repository.getContinueWatching()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _liveCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val liveCategories: StateFlow<List<XtreamCategory>> = _liveCategories

    private val _movieCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val movieCategories: StateFlow<List<XtreamCategory>> = _movieCategories

    private val _seriesCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val seriesCategories: StateFlow<List<XtreamCategory>> = _seriesCategories

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _expiryDate = MutableStateFlow<String>("Unlimited")
    val expiryDate: StateFlow<String> = _expiryDate

    init {
        loadDashboardData()
    }

    private fun formatExpiryDate(rawExpDate: String?): String {
        if (rawExpDate.isNullOrBlank() || rawExpDate == "null") return "Unlimited"
        if (rawExpDate.equals("Unlimited", ignoreCase = true)) return "Unlimited"
        val timestamp = rawExpDate.toLongOrNull() ?: return rawExpDate
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = java.util.Date(timestamp * 1000L)
            sdf.format(date)
        } catch (e: Exception) {
            rawExpDate
        }
    }

    fun loadDashboardData() {
        _isLoading.value = true
        val rawExp = repository.getExpiryDate()
        _expiryDate.value = formatExpiryDate(rawExp)
        viewModelScope.launch {
            try {
                repository.getCategories("live").onSuccess {
                    _liveCategories.value = it.take(8)
                }
                repository.getCategories("movies").onSuccess {
                    _movieCategories.value = it.take(8)
                }
                repository.getCategories("series").onSuccess {
                    _seriesCategories.value = it.take(8)
                }
            } catch (e: Exception) {
                // Fail-safe default fallback logic
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteContinueWatching(id: Int) {
        viewModelScope.launch {
            repository.deletePlaybackProgress(id)
        }
    }
}
