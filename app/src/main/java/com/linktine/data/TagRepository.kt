package com.linktine.data

import android.content.Context
import com.linktine.data.types.Tag
import com.linktine.data.types.TagCreate
import com.linktine.data.types.TagUpdateLinks
import com.linktine.network.ApiService
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.first
import java.io.IOException

class TagRepository(
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


    suspend fun fetchTags(): List<Tag> =
        try {
            getApiService().getTags()
        } catch (e: Exception) {
            throw IOException("Could not load tags.")
        }

    suspend fun createTag(name: String, color: String): Tag =
        try {
            getApiService().createTag(TagCreate(name, color))
        } catch (e: Exception) {
            throw IOException("Could not create tag.")
        }

    suspend fun updateTag(id: String, name: String, color: String) =
        try {
            getApiService().updateTag(id, TagCreate(name, color))
        } catch (e: Exception) {
            throw IOException("Could not update tag.")
        }

    suspend fun deleteTag(id: String) =
        try {
            getApiService().deleteTag(id)
        } catch (e: Exception) {
            throw IOException("Could not delete tag.")
        }

    suspend fun updateTagLinks(tagId: String, linkIds: List<String>) =
        try {
            getApiService().updateLinks(
                tagId,
                TagUpdateLinks(linkIds)
            )
        } catch (e: Exception) {
            throw IOException("Could not update tag links.")
        }
}
