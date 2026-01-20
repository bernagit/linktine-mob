package com.linktine.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.*
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val repository: SettingsRepository
) : AndroidViewModel(application) {

    val activeProfile = liveData {
        emit(repository.getActiveProfileOrNull())
    }

    // Logout navigation event
    private val _logoutEvent = MutableLiveData<Unit>()
    val logoutEvent: LiveData<Unit> = _logoutEvent

    fun setDarkMode(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOrNull()
            if (profile != null) {
                repository.deleteProfile(profile.id)
            }

            repository.setActiveProfile("")

            // Notify UI to navigate
            _logoutEvent.postValue(Unit)
        }
    }

    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = SettingsRepository(context)
            return ProfileViewModel(context.applicationContext as Application, repo) as T
        }
    }
}
