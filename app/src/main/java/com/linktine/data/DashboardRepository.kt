package com.linktine.data

import android.content.Context
import com.linktine.data.types.DashboardResponse
import com.linktine.network.ApiService
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Repository to handle business logic related to the Dashboard screen.
 */
class DashboardRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository // Dependency to get the current server URL
) {

    // Lazily gets the API service when needed, ensuring the base URL is current.
    private suspend fun getApiService(): ApiService {
        val serverInfo = settingsRepository.serverInfoFlow.first()
        if (serverInfo.url.isEmpty() || serverInfo.token.isEmpty()) {
            throw IllegalStateException("Server settings (URL and Token) are not configured.")
        }

        // The API path should include /api as per the base URL expectation.
        val apiUrl = "${serverInfo.url}/api"

        return RetrofitFactory.createApiService(context, apiUrl)
    }

    /**
     * Fetches the dashboard data from the API.
     */
    suspend fun fetchDashboard(): DashboardResponse {
        val service = getApiService()
        return try {
            service.getDashboard()
        } catch (e: Exception) {
            // Log the exception and throw a user-friendly error
            println("Dashboard API call failed: ${e.message}")
            throw IOException("Could not load dashboard data. Check connection and authentication.")
        }
    }
}
