package com.linktine.data.types

import com.google.gson.annotations.SerializedName
data class Collection(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("color") val color: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)