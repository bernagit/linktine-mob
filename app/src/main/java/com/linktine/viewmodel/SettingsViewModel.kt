package com.linktine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import android.content.Context
import kotlinx.coroutines.flow.first

/**
 * Events that the ViewModel sends to the Fragment to handle one-time UI actions (like navigation).
 */
sealed class SettingsEvent {
    data class SettingsSavedSuccess(val message: String) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    // Channel for one-time events (like navigation or showing a Toast)
    private val _eventsChannel = Channel<SettingsEvent>()
    val events = _eventsChannel.receiveAsFlow() // Exposed as Flow for observation

    // Flow to check if settings are already saved
    val areSettingsPresent = repository.areSettingsPresent

    /**
     * Initiates the saving process (user input).
     */
    fun saveSettingsAndLogin(url: String, token: String) {
        // Basic validation
        if (url.isBlank() || token.isBlank()) {
            viewModelScope.launch {
                _eventsChannel.send(SettingsEvent.Error("URL and Token cannot be empty."))
            }
            return
        }

        viewModelScope.launch {
            try {
                // Repository handles saving settings and the network login
                val userName = repository.saveSettingsAndLogin(url, token)

                // Send success event with the personalized message
                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("Welcome, $userName!"))
            } catch (e: Exception) {
                // Network or parsing errors are caught here
                val message = e.message ?: "An unknown login error occurred."
                _eventsChannel.send(SettingsEvent.Error(message))
            }
        }
    }

    /**
     * Attempts login validation using currently saved settings (used on app startup).
     * If successful, it triggers navigation to Home. If it fails, the user remains on Auth screen.
     */
    fun validateCurrentSettingsAndLogin() {
        viewModelScope.launch {
            try {
                // 1. Check if settings are even present before continuing
                if (repository.areSettingsPresent.first().not()) {
                    return@launch
                }

                // 2. Fetch saved credentials from the DataStore
                val info = repository.getCurrentServerInfo()

                // 3. Perform network validation and refresh user data
                val userName = repository.validateAndRefreshUser(info.url, info.token)

                // Send success event (triggers navigation in the Fragment)
                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("$userName logged in"))

            } catch (e: Exception) {
                // Network error, invalid token, or other issue. Stay on Auth screen.
                // Note: The UI should probably show a small message, but not block login attempts.
                _eventsChannel.send(SettingsEvent.Error("Login failed. Please try with another token"))
            }
        }
    }
}