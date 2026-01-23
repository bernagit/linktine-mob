package com.linktine.utils

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

    @Volatile private var cachedToken: String? = null

    private val publicAuthPaths = setOf(
        "/auth/login",
        "/auth/register",
        "/auth/logout"
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            repository.activeProfileFlow.collect { id ->
                val profile = runCatching { repository.getActiveProfile() }.getOrNull()
                cachedToken = profile?.token
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (publicAuthPaths.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
        cachedToken?.let {
            builder.header("Authorization", "Apikey $it")
        }

        return chain.proceed(builder.build())
    }
}
