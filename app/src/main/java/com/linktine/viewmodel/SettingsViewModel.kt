package com.linktine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
     * Initiates the saving process.
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
                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("Welcome $userName"))
            } catch (e: Exception) {
                // Network or parsing errors are caught here
                val message = e.message ?: "An unknown login error occurred."
                _eventsChannel.send(SettingsEvent.Error(message))
            }
        }
    }
}