package com.linktine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

    private val _eventsChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _eventsChannel.receiveAsFlow()

    val areSettingsPresent = repository.areSettingsPresent

    /**
     * Login / create profile from user input.
     */
    fun saveSettingsAndLogin(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            viewModelScope.launch {
                _eventsChannel.send(SettingsEvent.Error("URL and Token cannot be empty."))
            }
            return
        }

        viewModelScope.launch {
            try {
                val userName = repository.saveSettingsAndLogin(url, token)
                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("Welcome, $userName!"))
            } catch (e: Exception) {
                _eventsChannel.send(
                    SettingsEvent.Error(e.message ?: "An unknown login error occurred.")
                )
            }
        }
    }

    /**
     * Login using active profile.
     */
    fun validateCurrentSettingsAndLogin() {
        viewModelScope.launch {
            try {
                if (!repository.areSettingsPresent.first()) return@launch

                val profile = repository.getActiveProfile()
                val userName =
                    repository.validateAndRefreshUser(profile.serverUrl, profile.token)

                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("$userName logged in"))

            } catch (e: Exception) {
                _eventsChannel.send(
                    SettingsEvent.Error("Login failed. Please try with another profile.")
                )
            }
        }
    }

    /**
     * Switch active profile.
     */
    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
            validateCurrentSettingsAndLogin()
        }
    }
}
