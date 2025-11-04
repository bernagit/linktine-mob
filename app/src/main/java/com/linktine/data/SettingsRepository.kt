package com.linktine.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// Assuming DataStore setup here
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val SERVER_TOKEN = stringPreferencesKey("server_token")
    }

    suspend fun saveSettings(url: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = url
            preferences[PreferencesKeys.SERVER_TOKEN] = token
        }
    }

    val serverInfoFlow: Flow<ServerInfo> = context.dataStore.data
        .map { preferences ->
            val url = preferences[PreferencesKeys.SERVER_URL] ?: ""
            val token = preferences[PreferencesKeys.SERVER_TOKEN] ?: ""
            ServerInfo(url, token)
        }

    /**
     * Reads the current ServerInfo as a single value (non-Flow) synchronously.
     * This is the function that resolves the 'Unresolved reference' error in the ViewModel.
     */
    suspend fun getCurrentServerInfo(): ServerInfo {
        return serverInfoFlow.first()
    }

    val areSettingsPresent: Flow<Boolean> = serverInfoFlow.map { info ->
        info.url.isNotEmpty() && info.token.isNotEmpty()
    }

    suspend fun saveSettingsAndLogin(url: String, token: String): String {
        saveSettings(url, token)
        return performNetworkValidationAndUserSave(url, token)
    }

    suspend fun validateAndRefreshUser(url: String, token: String): String {
        return performNetworkValidationAndUserSave(url, token)
    }

    private suspend fun performNetworkValidationAndUserSave(url: String, token: String): String {
        return try {
            val apiUrl = "$url/api"
            val authService = RetrofitFactory.createApiService(context, apiUrl)

            val userResponse = authService.getMeWithAuthToken("Apikey $token")

            val userJson = JSONObject().apply {
                put("id", userResponse.id)
                put("email", userResponse.email)
                put("name", userResponse.name)
                put("role", userResponse.role)
            }

            saveUser(userJson, token)
            userResponse.name

        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "Unknown error"
            throw IOException("Login failed (Code ${e.code()}): $errorBody")
        } catch (e: Exception) {
            throw Exception("Login failed: ${e.message}", e)
        }
    }


    private fun saveUser(json: JSONObject, token: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val usersArray = try {
            JSONArray(prefs.getString("users", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }

        val userId = json.optString("id")

        val newUserObject = JSONObject().apply {
            put("id", userId)
            put("email", json.optString("email"))
            put("name", json.optString("name"))
            put("role", json.optString("role"))
            put("token", token)
        }

        var userFound = false
        for (i in 0 until usersArray.length()) {
            val existingUser = usersArray.optJSONObject(i)
            if (existingUser != null && existingUser.optString("id") == userId) {
                usersArray.put(i, newUserObject)
                userFound = true
                break
            }
        }

        if (!userFound) {
            usersArray.put(newUserObject)
        }

        prefs.edit {
            putString("users", usersArray.toString())
            putString("activeUser", userId)
        }
    }
}
