package com.linktine.data;

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linktine.data.types.UserProfile
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_ID = stringPreferencesKey("active_profile_id")
        val USERS_JSON = stringPreferencesKey("users_json")
        val CURRENT_THEME = stringPreferencesKey("current_theme")
    }

    private val dataStore = context.dataStore

    // ---------------- FLOW CACHE ----------------

    val activeProfileFlow = dataStore.data.map {
        it[Keys.ACTIVE_ID] ?: ""
    }

    val usersFlow = dataStore.data.map {
        it[Keys.USERS_JSON] ?: "[]"
    }

    val currentThemeFlow = dataStore.data.map {
        it[Keys.CURRENT_THEME] ?: "light"
    }

    // ---------------- WRITE ----------------

    suspend fun setActiveProfile(profileId: String) {
        dataStore.edit { it[Keys.ACTIVE_ID] = profileId }
    }

    private suspend fun saveUsers(json: String) {
        dataStore.edit { it[Keys.USERS_JSON] = json }
    }

    suspend fun setCurrentTheme(currentTheme: String) {
        val themeNames = listOf("light", "dark")

        if(!themeNames.contains(currentTheme)) {
            return;
        }

        dataStore.edit { it[Keys.CURRENT_THEME] = currentTheme }
    }

    // ---------------- READ ----------------

    suspend fun getActiveProfile(): UserProfile {
        val id = activeProfileFlow.first()
        val users = getAllProfiles()
        return users.first { it.id == id }
    }

    suspend fun getActiveProfileOrNull(): UserProfile? {
        val allProfiles = getAllProfiles()
        val activeId = activeProfileFlow.first()
        return allProfiles.firstOrNull { it.id == activeId }
    }


    suspend fun getAllProfiles(): List<UserProfile> {
        val json = usersFlow.first()
        val array = JSONArray(json)

        return List(array.length()) {
            val u = array.getJSONObject(it)
            UserProfile(
                id = u.getString("id"),
                serverUrl = u.getString("serverUrl"),
                token = u.getString("token"),
                email = u.getString("email"),
                name = u.getString("name"),
                role = u.getString("role")
            )
        }
    }

    suspend fun getCurrentTheme(): String {
        return currentThemeFlow.first()
    }

    // ---------------- LOGIN ----------------

    suspend fun saveSettingsAndLogin(url: String, token: String): String {
        return performLogin(url, token)
    }

    suspend fun validateAndRefreshUser(url: String, token: String): String {
        return performLogin(url, token)
    }

    private suspend fun performLogin(url: String, token: String): String {
        val apiUrl = "$url/api"
        val service = RetrofitFactory.createApiService(this, apiUrl)

        val user = service.getMeWithAuthToken("Apikey $token")

        val users = getAllProfiles().toMutableList()
        users.removeAll { it.id == user.id }

        users.add(
            UserProfile(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role,
                serverUrl = url,
                token = token
            )
        )

        saveUsers(JSONArray(users.map {
            JSONObject().apply {
                put("id", it.id)
                put("email", it.email)
                put("name", it.name)
                put("role", it.role)
                put("serverUrl", it.serverUrl)
                put("token", it.token)
            }
        }).toString())

        setActiveProfile(user.id)

        return user.name
    }

    // ---------------- DELETE ----------------

    suspend fun deleteProfile(id: String) {
        val users = getAllProfiles().filter { it.id != id }
        saveUsers(JSONArray(users.map {
            JSONObject().apply {
                put("id", it.id)
                put("email", it.email)
                put("name", it.name)
                put("role", it.role)
                put("serverUrl", it.serverUrl)
                put("token", it.token)
            }
        }).toString())
        if (users.isNotEmpty()) {
            setActiveProfile(users.first().id)
        }
    }
}