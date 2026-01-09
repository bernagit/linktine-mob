package com.linktine.viewmodel;

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linktine.data.SettingsRepository
import com.linktine.data.TagRepository
import com.linktine.data.types.Tag
import kotlinx.coroutines.launch

class TagViewModel(
    application: Application,
    private val tagRepository: TagRepository
) : AndroidViewModel(application) {

    private val _tagData = MutableLiveData<List<Tag>>()
    val tagData: LiveData<List<Tag>> = _tagData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _status = MutableLiveData("Loading tags…")
    val status: LiveData<String> = _status

    fun loadTags() {
        viewModelScope.launch {
            try {
                _status.value = "Fetching tags…"
                _tagData.value = tagRepository.fetchTags()
                _status.value = "Tags loaded"
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            try {
                tagRepository.createTag(name, color)
                loadTags()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateTag(id: String, name: String, color: String) {
        viewModelScope.launch {
            try {
                tagRepository.updateTag(id, name, color)
                loadTags()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTag(id)
                loadTags()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateTagLinks(tagId: String, linkIds: List<String>) {
        viewModelScope.launch {
            try {
                tagRepository.updateTagLinks(tagId, linkIds)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    class Factory(private val context: Context): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TagViewModel::class.java)) {
                val settingsRepo = SettingsRepository(context)
                val tagRepo = TagRepository(context, settingsRepo)
                return TagViewModel(context.applicationContext as Application, tagRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
