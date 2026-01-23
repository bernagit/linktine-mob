package com.linktine.data

import android.content.Context
import com.linktine.data.types.UsernameUpdate
import com.linktine.network.ApiService
import com.linktine.network.RetrofitFactory
import java.io.IOException

class UserRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private suspend fun getApiService(): ApiService {
        val profile = settingsRepository.getActiveProfile()

        if (profile.serverUrl.isEmpty() || profile.token.isEmpty()) {
            throw IllegalStateException("Active profile is not configured.")
        }

        val apiUrl = "${profile.serverUrl}/api"
        return RetrofitFactory.createApiService(settingsRepository, apiUrl)
    }

    suspend fun updateUsername(name: String) {
        try {
            getApiService().updateUsername(UsernameUpdate(name))
        } catch (e: Exception) {
            throw IOException("Could not update username.")
        }
    }
}