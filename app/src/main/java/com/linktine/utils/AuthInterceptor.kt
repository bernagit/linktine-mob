package com.linktine.utils

import android.content.Context
import com.linktine.data.SettingsRepository
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONArray

/**
 * OkHttp Interceptor responsible for adding the Authorization header
 * using the currently active user's token.
 *
 * Excludes the /auth/me endpoint.
 */
class AuthInterceptor(private val settingsRepository: SettingsRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip adding the token for the /auth/me endpoint
        if (path.endsWith("/auth/me")) {
            return chain.proceed(originalRequest)
        }

        // Get the active profile *synchronously* from SharedPreferences
        val prefs = settingsRepository.context.getSharedPreferences(
            "AppSettings", Context.MODE_PRIVATE
        )
        val activeProfileId = prefs.getString("active_profile_id", null) ?: ""

        val usersArray = JSONArray(prefs.getString("users", "[]") ?: "[]")
        var token: String? = null
        for (i in 0 until usersArray.length()) {
            val userObj = usersArray.getJSONObject(i)
            if (userObj.optString("id") == activeProfileId) {
                token = userObj.optString("token")
                break
            }
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Apikey $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            HttpClient.clearToken(settingsRepository.context)
        }

        return response
    }
}
