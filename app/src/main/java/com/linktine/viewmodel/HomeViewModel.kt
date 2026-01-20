package com.linktine.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.linktine.data.DashboardRepository
import com.linktine.data.SettingsRepository
import com.linktine.data.types.DashboardResponse
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val dashboardRepository: DashboardRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _dashboardData = MutableLiveData<DashboardResponse?>()
    val dashboardData: LiveData<DashboardResponse?> = _dashboardData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _text = MutableLiveData("Loading dashboard data...")
    val text: LiveData<String> = _text

    init {
        // Observe active profile changes
        viewModelScope.launch {
            settingsRepository.activeProfileFlow.collect { profileId ->
                if (profileId.isNotEmpty()) {
                    loadInitialData()
                } else {
                    // Clear dashboard when no profile
                    _dashboardData.value = null
                    _text.value = "No active profile"
                    _error.value = null
                }
            }
        }
    }

    fun loadInitialData() {
        if (_loading.value == true) return
        _loading.value = true

        viewModelScope.launch {
            try {
                _text.value = "Fetching data..."
                val result = dashboardRepository.fetchDashboard()
                _dashboardData.value = result
                _text.value = "Dashboard loaded! Total Links: ${result.stats.totalLinks}"
                _error.value = null
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                _error.value = errorMessage
                _text.value = "Error loading dashboard: $errorMessage"
                _dashboardData.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    // --- Factory ---
    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                val settingsRepo = SettingsRepository(context)
                val dashboardRepo = DashboardRepository(context, settingsRepo)
                return HomeViewModel(context.applicationContext as Application, dashboardRepo, settingsRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
