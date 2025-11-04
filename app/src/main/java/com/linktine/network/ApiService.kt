package com.linktine.network

import com.linktine.data.DashboardResponse
import com.linktine.data.UserResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Interface defining all API endpoints for the application.
 */
interface ApiService {

    /**
     * Executes a GET request to the 'me' endpoint to verify the token and fetch user details.
     */
    @GET("v1/auth/me")
    suspend fun getMe(): UserResponse

    /**
     * The first 'me' request
     */
    @GET("v1/auth/me")
    suspend fun getMeWithAuthToken(@Header("Authorization") token: String): UserResponse

    /**
     * Executes a GET request to the dashboard endpoint to fetch home screen data.
     */
    @GET("v1/base/dashboard")
    suspend fun getDashboard(): DashboardResponse
}
