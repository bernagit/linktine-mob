package com.linktine.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray

/**
 * This object is now primarily a utility for reading legacy settings
 * and token from SharedPreferences for the AuthInterceptor.
 */
object HttpClient {
    private const val TAG = "HttpClient"

    /***
     * Gets the token of the *current* user (last logged-in)
     * Returns the token prepended with "Apikey ".
     */
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val usersJson = prefs.getString("users", "[]") ?: "[]"
        val activeUser = prefs.getString("activeUser", null) ?: run {
            Log.w(TAG, "No active user ID found in SharedPreferences.")
            return null
        }

        Log.d(TAG, "Active user ID: $activeUser")

        return try {
            val usersArray = JSONArray(usersJson)
            if (usersArray.length() == 0) {
                Log.w(TAG, "Users array is empty.")
                return null
            }

            for (i in 0 until usersArray.length()) {
                val userObj = usersArray.getJSONObject(i)
                val id = userObj.optString("id", "")

                if (id.equals(activeUser, ignoreCase = true)) {
                    val token = userObj.optString("token")

                    if (token.isNotEmpty()) {
                        val fullToken = "Apikey $token"
                        Log.i(TAG, "Token found and formatted successfully.")
                        // Log the first few chars of the token for verification, not the full token for security
                        Log.d(TAG, "Authorization Header Value (Prefix): ${fullToken.take(30)}...")
                        return fullToken
                    } else {
                        Log.e(TAG, "Found active user but token field was empty.")
                        return null
                    }
                }
            }
            Log.w(TAG, "Active user ID '$activeUser' not found in the users list.")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user data or retrieving token.", e)
            null
        }
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val usersJson = prefs.getString("users", "[]") ?: "[]"
        val activeUserId = prefs.getString("activeUser", null)

        if (activeUserId == null) {
            Log.w(TAG, "clearToken: No active user found.")
            return
        }

        try {
            val usersArray = JSONArray(usersJson)
            for (i in 0 until usersArray.length()) {
                val userObj = usersArray.getJSONObject(i)
                val id = userObj.optString("id", "")
                if (id.equals(activeUserId, ignoreCase = true)) {
                    userObj.remove("token") // ✅ remove the token key
                    Log.i(TAG, "Token cleared for active user: $activeUserId")
                    break
                }
            }

            prefs.edit().putString("users", usersArray.toString()).apply()
            prefs.edit().remove("activeUser").apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error while clearing token for active user.", e)
        }
    }
}
