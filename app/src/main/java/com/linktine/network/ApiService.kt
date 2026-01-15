package com.linktine.network

import com.linktine.data.types.DashboardResponse
import com.linktine.data.UserResponse
import com.linktine.data.types.Collection
import com.linktine.data.types.CollectionCreate
import com.linktine.data.types.CollectionUpdate
import com.linktine.data.types.CollectionsResponse
import com.linktine.data.types.Link
import com.linktine.data.types.LinkCreate
import com.linktine.data.types.LinkUpdate
import com.linktine.data.types.PaginatedResponse
import com.linktine.data.types.Tag
import com.linktine.data.types.TagCreate
import com.linktine.data.types.TagUpdateLinks
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface defining all API endpoints for the application.
 */
interface ApiService {
    // HEALTH-CHECK
    @GET("v1/auth/me")
    suspend fun getMeWithAuthToken(@Header("Authorization") token: String): UserResponse

    // DASHBOARD FRAGMENT
    @GET("v1/base/dashboard")
    suspend fun getDashboard(): DashboardResponse

    // LINK FRAGMENT
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

    @POST("v1/links")
    suspend fun createLink(@Body link: LinkCreate): Link

    @DELETE("v1/links/{id}")
    suspend fun deleteLink(@Path("id") id: String)

    @PUT("v1/links/{id}")
    suspend fun updateLink(@Path("id") id: String, @Body link: LinkUpdate): Link

    // TAGS FRAGMENT
    @GET("v1/tags")
    suspend fun getTags(): List<Tag>

    @POST("v1/tags")
    suspend fun createTag(@Body tag: TagCreate): Tag

    @PUT("v1/tags/{id}")
    suspend fun updateTag(@Path("id") id: String, @Body tag: TagCreate)

    @DELETE("v1/tags/{id}")
    suspend fun deleteTag(@Path("id") id: String)

    @PATCH("v1/tags/{id}/links")
    suspend fun updateLinks(@Path("id") id: String, @Body updateTag: TagUpdateLinks)

    @GET("v1/collections")
    suspend fun getCollections(): CollectionsResponse

    @GET("v1/collections/{id}")
    suspend fun getCollectionByParentId(@Path("id") id: String): CollectionsResponse

    @GET("v1/collections/parent")
    suspend fun getCollectionsByParent(
        @Query("parentId") parentId: String?
    ): CollectionsResponse

    @POST("v1/collections")
    suspend fun createCollection(@Body collection: CollectionCreate): Collection

    @DELETE("v1/collections/{id}")
    suspend fun deleteCollection(@Path("id") id: String)

    @PUT("v1/collections/{id}")
    suspend fun updateCollection(@Path("id") id: String, @Body collection: CollectionUpdate): Collection
}
