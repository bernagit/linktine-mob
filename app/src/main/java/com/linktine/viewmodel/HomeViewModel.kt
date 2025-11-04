package com.linktine.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linktine.data.DashboardRepository
import com.linktine.data.DashboardResponse
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen, responsible for fetching and holding dashboard data.
 */
class HomeViewModel(
    application: Application,
    private val dashboardRepository: DashboardRepository
) : AndroidViewModel(application) {

    // LiveData to hold the fetched dashboard data
    private val _dashboardData = MutableLiveData<DashboardResponse>()
    val dashboardData: LiveData<DashboardResponse> = _dashboardData

    // LiveData for any errors during fetching
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Placeholder LiveData kept from the original fragment for text display
    private val _text = MutableLiveData<String>().apply {
        value = "Loading dashboard data..."
    }
    val text: LiveData<String> = _text

    /**
     * Triggers the data fetching process.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            try {
                _text.value = "Fetching data..."
                val result = dashboardRepository.fetchDashboard()
                _dashboardData.value = result
                _text.value = "Dashboard loaded! Total Links: ${result.stats.totalLinks}"
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                _error.value = errorMessage
                _text.value = "Error loading dashboard: $errorMessage"
            }
        }
    }

    // --- Factory Implementation ---
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

                // Initialize the repositories required
                val settingsRepo = SettingsRepository(context)
                val dashboardRepo = DashboardRepository(context, settingsRepo)

                return HomeViewModel(context.applicationContext as Application, dashboardRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
