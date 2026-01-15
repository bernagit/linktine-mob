package com.linktine.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.linktine.utils.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    /**
     * Creates and returns the OkHttpClient with the AuthInterceptor.
     */
    private fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            // AuthInterceptor reads the token and injects the Authorization header
            .addInterceptor(AuthInterceptor(context))
            .build()
    }

    /**
     * Creates the Retrofit service instance with the dynamic base URL.
     * @param baseUrl The base API URL (e.g., "https://server.com/api")
     */
    fun createApiService(context: Context, baseUrl: String): ApiService {
        // Ensure the base URL ends with a '/' for Retrofit
        val finalBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val gson = GsonBuilder()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
