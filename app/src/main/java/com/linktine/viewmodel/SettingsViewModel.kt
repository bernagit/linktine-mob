package com.linktine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    private val loginMutex = Mutex()


    // SINGLE SOURCE OF TRUTH
    val areSettingsPresent = repository.activeProfileFlow
        .map { it.isNotEmpty() }
        .distinctUntilChanged()

    var loginJob: Job? = null

    // ---------------- LOGIN ----------------
    fun saveSettingsAndLogin(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            viewModelScope.launch {
                _eventsChannel.send(SettingsEvent.Error("URL and Token cannot be empty."))
            }
            return
        }

        loginJob?.cancel()

        loginJob = viewModelScope.launch {
            loginMutex.withLock {
                try {
                    val userName = repository.saveSettingsAndLogin(url, token)
                    val profile = repository.getActiveProfileOrNull()
                    if (profile != null) {
                        val refreshedName = repository.validateAndRefreshUser(profile.serverUrl, profile.token)
                        _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("Welcome, $refreshedName!"))
                    } else {
                        _eventsChannel.send(SettingsEvent.Error("No active profile found after login"))
                    }
                } catch (e: CancellationException) {
                    // ignore
                } catch (e: Exception) {
                    _eventsChannel.send(SettingsEvent.Error(e.message ?: "Login failed"))
                }
            }
        }
    }


    // ---------------- AUTO LOGIN ----------------
    fun validateCurrentSettingsAndLogin() {
        // loginJob?.cancel()

        loginJob = viewModelScope.launch {
            try {
                val profile = repository.getActiveProfileOrNull()
                if (profile == null) {
                    _eventsChannel.send(
                        SettingsEvent.Error("No active profile found. Please login again.")
                    )
                    return@launch
                }

                val userName =
                    repository.validateAndRefreshUser(profile.serverUrl, profile.token)

                _eventsChannel.send(SettingsEvent.SettingsSavedSuccess("$userName logged in"))

            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                _eventsChannel.send(
                    SettingsEvent.Error("Login failed. Please try with another profile.")
                )
            }
        }
    }

    // ---------------- SWITCH PROFILE ----------------
    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            loginMutex.withLock {
                try {
                    // Ensure the write cannot be canceled
                    withContext(NonCancellable) {
                        repository.setActiveProfile(profileId)
                    }

                    val profile = repository.getActiveProfileOrNull()
                    if (profile != null) {
                        validateCurrentSettingsAndLogin()
                    } else {
                        _eventsChannel.send(SettingsEvent.Error("Selected profile is missing."))
                    }
                } catch (e: CancellationException) {
                    // ignore silently
                } catch (e: Exception) {
                    _eventsChannel.send(SettingsEvent.Error("Failed to switch profile: ${e.message}"))
                }
            }
        }
    }
}
