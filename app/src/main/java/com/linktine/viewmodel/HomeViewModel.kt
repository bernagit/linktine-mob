package com.linktine.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.linktine.data.DashboardRepository
import com.linktine.data.SettingsRepository
import com.linktine.data.types.DashboardResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomeViewModel(
    application: Application,
    private val dashboardRepository: DashboardRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _dashboardData = MutableLiveData<DashboardResponse?>()
    val dashboardData: LiveData<DashboardResponse?> = _dashboardData

    private val _activeUser = MutableLiveData<String?>()
    val activeUser: LiveData<String?> = _activeUser

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _text = MutableLiveData("Loading dashboard data...")
    val text: LiveData<String> = _text

    private val loadMutex = Mutex()

    init {
        // Observe profile changes
        viewModelScope.launch {
            settingsRepository.activeProfileFlow.collect { profileId ->
                if (profileId.isNotEmpty()) {
                    val profile = settingsRepository.getActiveProfileOrNull()
                    _activeUser.value = profile?.name
                    loadDashboardForActiveProfile()
                } else {
                    _activeUser.value = null
                    clearDashboard()
                }
            }
        }
    }

    private suspend fun loadDashboardForActiveProfile() {
        loadMutex.withLock {
            _loading.postValue(true)
            _error.postValue(null)
            _dashboardData.postValue(null) // Clear old dashboard immediately

            try {
                val result = dashboardRepository.fetchDashboard()
                _dashboardData.postValue(result)
                _text.postValue("Dashboard loaded! Total Links: ${result.stats.totalLinks}")
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to load dashboard")
                _text.postValue("Error loading dashboard: ${_error.value}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun clearDashboard() {
        _dashboardData.postValue(null)
        _text.postValue("No active profile")
        _error.postValue(null)
        _loading.postValue(false)
    }

    fun loadInitialData() {
        viewModelScope.launch {
            loadDashboardForActiveProfile()
        }
    }

    // --- Factory ---
    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                val settingsRepo = SettingsRepository(context)
                val dashboardRepo = DashboardRepository(context, settingsRepo)
                return HomeViewModel(context.applicationContext as android.app.Application, dashboardRepo, settingsRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

