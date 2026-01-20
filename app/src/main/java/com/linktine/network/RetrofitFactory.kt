package com.linktine.network

import com.linktine.data.SettingsRepository
import com.linktine.utils.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
object RetrofitFactory {

    fun createApiService(repository: SettingsRepository, baseUrl: String): ApiService {
        val finalBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(repository))
            .build()

        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

