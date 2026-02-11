package com.linktine.utils

import android.util.Log
import com.linktine.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor responsible for adding the Authorization header
 * using the currently active user's token.
 *
 * Excludes the /auth/me endpoint.
 */
class AuthInterceptor(private val repository: SettingsRepository) : Interceptor {

    private val publicAuthPaths = setOf(
        "/auth/login",
        "/auth/register",
        "/auth/logout"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (publicAuthPaths.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }

        val token = kotlinx.coroutines.runBlocking {
//            Log.d("TEST", repository.getActiveProfile().id)
            repository.getActiveProfileOrNull()?.token
        }

        val builder = request.newBuilder()
        token?.let {
            builder.header("Authorization", "Apikey $it")
        }

        if (token != null) {
            Log.d("AuthInterceptor", token)
        }
        return chain.proceed(builder.build())
    }
}
