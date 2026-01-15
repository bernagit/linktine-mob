package com.linktine.data

import android.content.Context
import com.linktine.data.types.Collection
import com.linktine.data.types.CollectionCreate
import com.linktine.data.types.CollectionUpdate
import com.linktine.data.types.CollectionsResponse
import com.linktine.network.ApiService
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.first
import java.io.IOException

class CollectionRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private suspend fun getApiService(): ApiService {
        val serverInfo = settingsRepository.serverInfoFlow.first()
        if (serverInfo.url.isEmpty() || serverInfo.token.isEmpty()) {
            throw IllegalStateException("Server settings (URL and Token) are not configured.")
        }

        val apiUrl = "${serverInfo.url}/api"
        return RetrofitFactory.createApiService(context, apiUrl)
    }

    suspend fun fetchCollections(parentId: String?): CollectionsResponse {
        val service = getApiService()
        return try {
            service.getCollectionsByParent(parentId)
        } catch (e: Exception) {
            throw IOException(
                "Could not load collections (parentId=$parentId). Check connection and authentication.",
                e
            )
        }
    }

    suspend fun createCollection(name: String, description: String?, color: String, parentId: String?): Collection {
        val service = getApiService()
        return try {
            service.createCollection(CollectionCreate(name, description, color, parentId))
        } catch (e: Exception) {
            throw IOException(
                "Could not create new collection. Check connection and authentication.",
                e
            )
        }
    }

    suspend fun deleteCollection(collectionId: String) {
        val service = getApiService()
        try {
            service.deleteCollection(collectionId)
        } catch (e: Exception) {
            println("Collection API call failed: ${e.message}")
            throw IOException("Could not delete collection.")
        }
    }

    suspend fun moveCollection(collectionId: String, parentId: String?) {
        val service = getApiService()
        val parentId = when(parentId) {
            null -> "null"
            else -> parentId
        }
        val collectionUpdate = CollectionUpdate(parentId = parentId)
        service.updateCollection(collectionId, collectionUpdate)
    }
}
