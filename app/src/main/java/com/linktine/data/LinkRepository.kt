package com.linktine.data

import android.content.Context
import com.linktine.data.types.Link
import com.linktine.data.types.PaginatedResponse
import com.linktine.network.ApiService
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Repository to handle business logic related to Links.
 */
class LinkRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    // Ensures Retrofit always uses the current server URL/token
    private suspend fun getApiService(): ApiService {
        val serverInfo = settingsRepository.serverInfoFlow.first()
        if (serverInfo.url.isEmpty() || serverInfo.token.isEmpty()) {
            throw IllegalStateException("Server settings (URL and Token) are not configured.")
        }

        val apiUrl = "${serverInfo.url}/api"
        return RetrofitFactory.createApiService(context, apiUrl)
    }

    /**
     * Fetches paginated links from the server.
     */
    suspend fun fetchLinks(
        page: Int = 1,
        limit: Int = 20,
        query: String? = null,
        tag: String? = null,
        collectionId: String? = null,
        read: Boolean? = null,
        archived: Boolean? = null,
    ): PaginatedResponse<Link> {
        val service = getApiService()
        return try {
            service.getLinks(page, limit, query, tag, collectionId, read, archived)
        } catch (e: Exception) {
            println("Link API call failed: ${e.message}")
            throw IOException("Could not load links.")
        }
    }

}
