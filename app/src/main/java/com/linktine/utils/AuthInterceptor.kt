package com.linktine.utils

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor responsible for adding the Authorization header
 * using the currently active user's token.
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Reusing the token fetching logic from your existing HttpClient
        val token = HttpClient.getToken(context)

        val requestBuilder = originalRequest.newBuilder()

        // Add the Authorization header if a token is present
        if (token != null) {
            requestBuilder.header("Authorization", token)
        }

        val response = chain.proceed(requestBuilder.build())
        if(response.code == 401) {
            HttpClient.clearToken(context)
        }

        return response
    }
}
