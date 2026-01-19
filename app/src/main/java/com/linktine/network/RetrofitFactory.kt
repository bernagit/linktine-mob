package com.linktine.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.linktine.data.SettingsRepository
import com.linktine.utils.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    /**
     * Creates and returns the OkHttpClient with the AuthInterceptor.
     */
    private fun createOkHttpClient(repository: SettingsRepository): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(repository))
            .build()
    }

    fun createApiService(repository: SettingsRepository, baseUrl: String): ApiService {
        val finalBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val gson = GsonBuilder().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(createOkHttpClient(repository))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
