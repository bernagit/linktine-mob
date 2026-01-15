package com.linktine.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.linktine.data.LinkRepository
import com.linktine.data.SettingsRepository
import com.linktine.data.types.Link
import com.linktine.data.types.PaginatedResponse
import kotlinx.coroutines.launch

class LinkViewModel(
    application: Application,
    private val linkRepository: LinkRepository
) : AndroidViewModel(application) {

    private val _linkData = MutableLiveData<PaginatedResponse<Link>>()
    val linkData: LiveData<PaginatedResponse<Link>> = _linkData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _status = MutableLiveData("Loading links…")
    val status: LiveData<String> = _status

    fun loadInitialLinks(
        page: Int = 1,
        limit: Int = 20,
        query: String? = null,
        tag: String? = null,
        collectionId: String? = null,
        read: Boolean? = null,
        archived: Boolean? = null,
        favorite: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                _status.value = "Fetching links…"
                val result = linkRepository.fetchLinks(
                    page = page,
                    limit = limit,
                    query = query,
                    tag = tag,
                    collectionId = collectionId,
                    read = read,
                    archived = archived,
                    favorite = favorite
                )
                _linkData.value = result
                _status.value = "Loaded ${result.data.size} links (Total: ${result.total})"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _status.value = "Failed loading links"
            }
        }
    }

    fun addLink(title: String?, url: String, tags: List<String>) {
        viewModelScope.launch {
            linkRepository.createLink(url = url, name = title, tags = tags)
        }
        loadInitialLinks()
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            linkRepository.deleteLink(id = id)
        }
        loadInitialLinks()
    }

    fun updateLink(
        id: String,
        name: String,
        url: String,
        read: Boolean,
        archived: Boolean,
        favorite: Boolean
    ) {
        viewModelScope.launch {
            try {
                linkRepository.updateLink(
                    id = id,
                    name = name,
                    url = url,
                    read = read,
                    archived = archived,
                    favorite = favorite
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed updating link"
            }
        }
        loadInitialLinks()
    }


    // Factory
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LinkViewModel::class.java)) {
                val settingsRepo = SettingsRepository(context)
                val linkRepo = LinkRepository(context, settingsRepo)
                return LinkViewModel(context.applicationContext as Application, linkRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
