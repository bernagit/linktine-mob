package com.linktine.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linktine.data.CollectionsResponse
import com.linktine.data.DashboardRepository
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.launch

class CollectionsViewModel(
    application: Application,
    private val dashboardRepository: DashboardRepository
): AndroidViewModel(application) {

    private val _collectionsData = MutableLiveData<CollectionsResponse>()
    val collectionsData: LiveData<CollectionsResponse> = _collectionsData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _text = MutableLiveData<String>().apply {
        value = "Loading dashboard data..."
    }
    val text: LiveData<String> = _text

    fun loadInitialData() {
        viewModelScope.launch {
            try {
                _text.value = "Fetching data..."
                val result = dashboardRepository.fetchCollections()
                _collectionsData.value = result
                _text.value = "Data loaded! Total collections: ${result.data.size}"
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                _error.value = errorMessage
                _text.value = "Error loading dashboard: $errorMessage"
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CollectionsViewModel::class.java)) {

                val settingsRepo = SettingsRepository(context)
                val dashboardRepo = DashboardRepository(context, settingsRepo)

                return CollectionsViewModel(
                    context.applicationContext as Application,
                    dashboardRepo
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}