package com.linktine.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.*
import com.linktine.data.SettingsRepository
import com.linktine.data.UserRepository
import com.linktine.data.types.UserProfile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    val activeProfile: LiveData<UserProfile?> =
        repository.usersFlow.map {
            repository.getActiveProfileOrNull()
        }.asLiveData()

    // Logout navigation event
    private val _logoutEvent = MutableLiveData<Unit>()
    val logoutEvent: LiveData<Unit> = _logoutEvent

    val currentTheme: LiveData<String> =
        repository.currentThemeFlow.asLiveData()

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun setDarkMode(enabled: Boolean) {
        var currentTheme = ""

        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            currentTheme = "dark"
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            currentTheme = "light"
        }

        viewModelScope.launch {
            repository.setCurrentTheme(currentTheme)
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUsername(newUsername)
                repository.updateActiveProfileName(newUsername)
                _message.postValue("Username updated!")
            } catch (_: Exception) {
                _message.postValue("Failed to update username")
            }
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
            val userRepository = UserRepository(context, repo)
            return ProfileViewModel(context.applicationContext as Application, repo, userRepository) as T
        }
    }
}
