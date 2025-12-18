package com.linktine.network

import com.linktine.data.types.DashboardResponse
import com.linktine.data.UserResponse
import com.linktine.data.types.Link
import com.linktine.data.types.PaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

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

    /**
     * Executes a GET request to the 'links' endpoint to fetch a list of links.
     */
    @GET("v1/links")
    suspend fun getLinks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("q") query: String? = null,
        @Query("tag") tag: String? = null,
        @Query("collectionId") collectionId: String? = null,
        @Query("read") read: Boolean? = null,
        @Query("archived") archived: Boolean? = null
    ): PaginatedResponse<Link>

}
