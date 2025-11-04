package com.linktine.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.linktine.data.SettingsRepository

/**
 * Factory class to instantiate SettingsViewModel with a SettingsRepository dependency.
 */
class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            // Create the Repository, passing the application Context
            val repository = SettingsRepository(context)

            // Return the ViewModel instance
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}