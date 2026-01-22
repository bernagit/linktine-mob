package com.linktine.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linktine.data.CollectionRepository
import com.linktine.data.LinkRepository
import com.linktine.data.SettingsRepository
import com.linktine.data.types.CollectionsResponse
import com.linktine.data.types.Collection
import kotlinx.coroutines.launch

class CollectionsViewModel(
    application: Application,
    private val collectionRepository: CollectionRepository,
    private val linkRepository: LinkRepository,
): AndroidViewModel(application) {

    private val _collectionsData = MutableLiveData<CollectionsResponse>()
    val collectionsData: LiveData<CollectionsResponse> = _collectionsData

    private val _currentCollection = MutableLiveData<Collection?>(null)
    val currentCollection: LiveData<Collection?> = _currentCollection

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val collectionHistory = mutableListOf<Collection?>()

    private val _text = MutableLiveData<String>().apply {
        value = "Loading dashboard data..."
    }
    val text: LiveData<String> = _text

    fun selectCollection(collection: Collection) {
        collectionHistory.add(_currentCollection.value)

        _currentCollection.value = collection
        reloadData()
    }

    fun reloadData() {
        viewModelScope.launch {
            try {
                val result = collectionRepository.fetchCollections(currentCollection.value?.id)
                _collectionsData.value = result
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                _error.value = errorMessage
                _text.value = "Error loading collection: $errorMessage"
            }
        }
    }

    fun goBack(): Boolean {
        return if (collectionHistory.isNotEmpty()) {
            val previous = collectionHistory.removeAt(collectionHistory.lastIndex)
            _currentCollection.value = previous
            viewModelScope.launch {
                val result = if (previous != null) {
                    collectionRepository.fetchCollections(previous.id)
                } else {
                    collectionRepository.fetchCollections(null)
                }
                _collectionsData.value = result
            }
            true
        } else {
            false
        }
    }

    fun addLink(title: String?, url: String) {
        viewModelScope.launch {
            linkRepository.createLink(url = url, name = title)
            reloadData()
        }
    }

    fun addCollection(name: String, description: String?, color: String, parentId: String?) {
        viewModelScope.launch {
            collectionRepository.createCollection(name, description, color, parentId)
            reloadData()
        }
    }

    fun updateCollection(collectionId: String, name: String, description: String?, color: String, parentId: String?) {
        viewModelScope.launch {
            collectionRepository.updateCollection(collectionId, name, description, color, parentId)
            reloadData()
        }
    }

    fun moveLink(linkId: String, collectionId: String?) {
        viewModelScope.launch {
            linkRepository.moveLinkIntoCollection(linkId, collectionId)
            reloadData()
        }
    }

    fun moveCollection(collectionId: String, parentId: String?) {
        viewModelScope.launch {
            collectionRepository.moveCollection(collectionId, parentId)
            reloadData()
        }
    }

    fun deleteLink(linkId: String) {
        viewModelScope.launch {
            linkRepository.deleteLink(linkId)
            reloadData()
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(collectionId)
            reloadData()
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CollectionsViewModel::class.java)) {

                val settingsRepo = SettingsRepository(context)
                val collectionRepo = CollectionRepository(context, settingsRepo)
                val linkRepo = LinkRepository(context, settingsRepo)

                return CollectionsViewModel(
                    context.applicationContext as Application,
                    collectionRepo,
                    linkRepo,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}