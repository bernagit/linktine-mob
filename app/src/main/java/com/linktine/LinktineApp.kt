package com.linktine

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.*

class LinktineApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val repo = SettingsRepository(this)

        CoroutineScope(Dispatchers.IO).launch {
            val theme = repo.getCurrentTheme()

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(
                    when (theme) {
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_NO
                    }
                )
            }
        }
    }
}
