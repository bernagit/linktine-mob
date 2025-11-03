package com.linktine.utils

import android.content.Context
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

object HttpClient {
    private val client = OkHttpClient()

    /***
     * Returns the base API URL (stored as "server_url" in SharedPreferences)
     */
    fun getBaseUrl(context: Context): String? {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val base = prefs.getString("server_url", null)
        return if (base != null) "$base/api" else null
    }

    /***
     * Gets the token of the *current* user (last logged-in)
     * If multiple users are stored, returns the last in the array.
     */
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val usersJson = prefs.getString("users", "[]") ?: "[]"
        val activeUser = prefs.getString("activeUser", null) ?: return null

        return try {
            val usersArray = JSONArray(usersJson)
            if (usersArray.length() == 0) return null

            for (i in 0 until usersArray.length()) {
                val userObj = usersArray.getJSONObject(i)
                val id = userObj.optString("id", "")
                if (id.equals(activeUser, ignoreCase = true)) {
                    val token = userObj.optString("token")
                    return token.let { "Apikey $it" }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /***
     * Builds an HTTP request with support for:
     *  - query parameters
     *  - custom headers
     *  - automatic auth header (from last user)
     */
    fun request(
        context: Context,
        path: String,
        method: String = "GET",
        body: RequestBody? = null,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null
    ): Request {

        val baseUrl = getBaseUrl(context) ?: throw Exception("Server URL not set")
        val urlBuilder = "$baseUrl$path".toHttpUrlOrNull()!!.newBuilder()

        // Add query parameters
        params?.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val requestBuilder = Request.Builder().url(urlBuilder.build())

        // Add global and custom headers
        headers?.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        // Add token automatically if available
        val token = getToken(context)
        if (token != null) {
            requestBuilder.addHeader("Authorization", token)
        }

        // Set HTTP method
        when (method.uppercase()) {
            "POST" -> requestBuilder.post(body ?: FormBody.Builder().build())
            "PUT" -> requestBuilder.put(body ?: FormBody.Builder().build())
            "DELETE" -> requestBuilder.delete(body)
            else -> requestBuilder.get()
        }

        return requestBuilder.build()
    }

    /***
     * Executes a request asynchronously using OkHttp
     */
    fun execute(request: Request, callback: Callback) {
        client.newCall(request).enqueue(callback)
    }
}
