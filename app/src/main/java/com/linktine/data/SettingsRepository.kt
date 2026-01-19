package com.linktine.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linktine.data.types.UserProfile
import com.linktine.network.RetrofitFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(val context: Context) {

    private object PreferencesKeys {
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    // -------------------- ACTIVE PROFILE --------------------

    suspend fun setActiveProfile(profileId: String) {
        context.dataStore.edit {
            it[PreferencesKeys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun getActiveProfileId(): String {
        return context.dataStore.data.first()[PreferencesKeys.ACTIVE_PROFILE_ID] ?: ""
    }

    val areSettingsPresent: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.ACTIVE_PROFILE_ID]?.isNotEmpty() == true
    }

    val activeProfileFlow: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.ACTIVE_PROFILE_ID] ?: "" }

    // -------------------- LOGIN --------------------

    suspend fun saveSettingsAndLogin(url: String, token: String): String {
        return performNetworkValidationAndUserSave(url, token)
    }

    suspend fun validateAndRefreshUser(url: String, token: String): String {
        return performNetworkValidationAndUserSave(url, token)
    }

    private suspend fun performNetworkValidationAndUserSave(url: String, token: String): String {
        return try {
            val apiUrl = "$url/api"
            val authService = RetrofitFactory.createApiService(this, apiUrl)

            val userResponse = authService.getMeWithAuthToken("Apikey $token")

            val userJson = JSONObject().apply {
                put("id", userResponse.id)
                put("email", userResponse.email)
                put("name", userResponse.name)
                put("role", userResponse.role)
            }

            saveUser(userJson, token, url)
            setActiveProfile(userResponse.id)

            userResponse.name

        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "Unknown error"
            throw IOException("Login failed (Code ${e.code()}): $errorBody")
        } catch (e: Exception) {
            throw Exception("Login failed: ${e.message}", e)
        }
    }

    // -------------------- USER STORAGE --------------------

    private fun saveUser(json: JSONObject, token: String, serverUrl: String) {
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
            put("serverUrl", serverUrl)
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
        }
    }

    // -------------------- READ USERS --------------------

    fun getAllProfiles(): List<UserProfile> {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val usersArray = JSONArray(prefs.getString("users", "[]"))

        val list = mutableListOf<UserProfile>()

        for (i in 0 until usersArray.length()) {
            val u = usersArray.getJSONObject(i)
            list.add(
                UserProfile(
                    id = u.optString("id"),
                    serverUrl = u.optString("serverUrl"),
                    token = u.optString("token"),
                    email = u.optString("email"),
                    name = u.optString("name"),
                    role = u.optString("role")
                )
            )
        }
        return list
    }

    suspend fun getActiveProfile(): UserProfile {
        val id = getActiveProfileId()
        return getAllProfiles().first { it.id == id }
    }

    // -------------------- DELETE PROFILE --------------------

    fun deleteProfile(profileId: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val usersArray = JSONArray(prefs.getString("users", "[]"))

        val newArray = JSONArray()

        for (i in 0 until usersArray.length()) {
            val obj = usersArray.getJSONObject(i)
            if (obj.optString("id") != profileId) {
                newArray.put(obj)
            }
        }

        prefs.edit {
            putString("users", newArray.toString())
        }
    }
}
