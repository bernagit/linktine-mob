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
        archived: Boolean? = null
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
                )
                _linkData.value = result
                _status.value = "Loaded ${result.data.size} links (Total: ${result.total})"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _status.value = "Failed loading links"
            }
        }
    }

    fun loadNextPage() {
        val current = _linkData.value ?: return
        val nextPage = current.page + 1
        loadInitialLinks(page = nextPage, limit = current.pageSize)
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
