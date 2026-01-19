package com.linktine.data.types

import com.google.gson.annotations.SerializedName

/**
 * Data class mapping the JSON response from the /v1/auth/me endpoint.
 */
data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("role")
    val role: String
)

data class UserProfile(
    val id: String,
    val serverUrl: String,
    val token: String,
    val email: String,
    val name: String,
    val role: String
)