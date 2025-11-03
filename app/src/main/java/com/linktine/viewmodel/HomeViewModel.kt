package com.linktine.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    // Example of a data field exposed to the Fragment
    private val _text = MutableLiveData<String>().apply {
        value = "Welcome Home! No data loaded yet."
    }
    val text: LiveData<String> = _text

    // Function to start loading initial data (will use a Repository later)
    fun loadInitialData() {
        // This is where you'd call repository.getDashboardData()
        _text.value = "Loading dashboard data..."
    }
}