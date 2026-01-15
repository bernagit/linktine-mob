package com.linktine.data.types

import com.google.gson.annotations.SerializedName
import com.linktine.data.Link
import java.util.Date

data class Collection (
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("color") val color: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("createdAt") val createdAt: Date,
    @SerializedName("updatedAt") val updatedAt: Date,
    @SerializedName("links") val links: List<Link>?,
)

data class CollectionsResponse(
    @SerializedName("collections") val collections: List<Collection>,
    @SerializedName("links") val links: List<Link>,
)

data class CollectionCreate(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("color") val color: String,
    @SerializedName("parentId") val parentId: String?,
)

data class CollectionUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("parentId") val parentId: String? = "null",
)