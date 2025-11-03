package com.linktine.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linktine.utils.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Define keys for DataStore preferences
    private object PreferencesKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val SERVER_TOKEN = stringPreferencesKey("server_token")
    }

    /**
     * Saves the server URL and Token to DataStore.
     */
    suspend fun saveSettings(url: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = url
            preferences[PreferencesKeys.SERVER_TOKEN] = token
        }
    }

    /**
     * Reads the current ServerInfo as a Flow.
     * Use this to check if settings are already present.
     */
    val serverInfoFlow: Flow<ServerInfo> = context.dataStore.data
        .map { preferences ->
            val url = preferences[PreferencesKeys.SERVER_URL] ?: ""
            val token = preferences[PreferencesKeys.SERVER_TOKEN] ?: ""
            ServerInfo(url, token)
        }

    /**
     * Helper function to check if both URL and Token are non-empty.
     */
    val areSettingsPresent: Flow<Boolean> = serverInfoFlow.map { info ->
        info.url.isNotEmpty() && info.token.isNotEmpty()
    }

    /**
     * Executes the login and saves settings and user info upon success.
     * @return The user's name on success, or throws an exception on failure.
     */
    suspend fun saveSettingsAndLogin(url: String, token: String): String {
        // 1. Save URL and Token (DataStore is suspended, so this works)
        saveSettings(url, token)

        // 2. Perform the network call and process response (similar to your old activity code)
        return try {
            val request = HttpClient.request(
                context = context, // Pass context if HttpClient needs it
                path = "/v1/auth/me",
                method = "GET",
                headers = mapOf("Authorization" to "Apikey $token")
            )

            // Execute synchronously or use a wrapper to handle async inside coroutines
            // IMPORTANT: For a real app, use Retrofit/Ktor. We adapt your OKHttp style:
            val response = suspendCancellableCoroutine<Response> { continuation ->
                HttpClient.execute(request, object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                })
            }

            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty response body")
                val json = JSONObject(body)
                val userName = json.optString("name")

                // 3. Save User Details (similar to your saveUser function)
                saveUser(json, token)

                userName
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("Login failed (Code ${response.code}): $errorBody")
            }
        } catch (e: Exception) {
            // Rethrow the exception to be caught in the ViewModel
            throw e
        }
    }

    // Adapted from your saveUser function
    private fun saveUser(json: JSONObject, token: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        // ... [Your existing logic to save user details to SharedPreferences] ...
        // NOTE: For a clean MVVM structure, User details should ideally be saved via DataStore or Room
        // For now, we keep your SharedPreferences logic here, but DataStore is preferred.
        val users = JSONArray(prefs.getString("users", "[]"))
        // ... [logic to create user JSONObject and save to prefs] ...
        val user = JSONObject().apply {
            put("id", json.optString("id"))
            put("email", json.optString("email"))
            put("name", json.optString("name"))
            put("role", json.optString("role"))
            put("token", token)
        }

        users.put(user)

        prefs.edit {
            putString("users", users.toString())
            putString("activeUser", json.optString("id"))
        }
    }
}