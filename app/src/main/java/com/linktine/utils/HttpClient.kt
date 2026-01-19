package com.linktine.utils

import android.content.Context
import android.util.Log
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import androidx.core.content.edit

object HttpClient {
    private const val TAG = "HttpClient"

    /**
     * Returns the token of the *active* user, prepended with "Apikey ".
     * Returns null if no active user or token is missing.
     */
    fun getToken(context: Context): String? = runBlocking {
        try {
            val repository = SettingsRepository(context.applicationContext)
            val activeProfileId = repository.getActiveProfileId()

            if (activeProfileId.isEmpty()) {
                Log.w(TAG, "No active profile set.")
                return@runBlocking null
            }

            val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val usersJson = prefs.getString("users", "[]") ?: "[]"
            val usersArray = JSONArray(usersJson)

            for (i in 0 until usersArray.length()) {
                val userObj = usersArray.getJSONObject(i)
                if (userObj.optString("id") == activeProfileId) {
                    val token = userObj.optString("token")
                    if (token.isNotEmpty()) {
                        val fullToken = "Apikey $token"
                        Log.d(TAG, "Token found for active profile: ${fullToken.take(30)}...")
                        return@runBlocking fullToken
                    } else {
                        Log.w(TAG, "Token is empty for active profile.")
                        return@runBlocking null
                    }
                }
            }

            Log.w(TAG, "Active profile ID not found in users list: $activeProfileId")
            null

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token", e)
            null
        }
    }

    /**
     * Clears the token for the active profile.
     */
    fun clearToken(context: Context) = runBlocking {
        try {
            val repository = SettingsRepository(context.applicationContext)
            val activeProfileId = repository.getActiveProfileId()
            if (activeProfileId.isEmpty()) {
                Log.w(TAG, "clearToken: No active profile set.")
                return@runBlocking
            }

            val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val usersJson = prefs.getString("users", "[]") ?: "[]"
            val usersArray = JSONArray(usersJson)
            var updated = false

            for (i in 0 until usersArray.length()) {
                val userObj = usersArray.getJSONObject(i)
                if (userObj.optString("id") == activeProfileId) {
                    userObj.remove("token")
                    updated = true
                    Log.i(TAG, "Token cleared for profile: $activeProfileId")
                    break
                }
            }

            if (updated) {
                prefs.edit { putString("users", usersArray.toString()) }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing token for active profile", e)
        }
    }
}
