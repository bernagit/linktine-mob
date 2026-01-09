package com.linktine.data

import com.google.gson.annotations.SerializedName
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
    @SerializedName("children") val children: List<Collection>?,
    @SerializedName("links") val links: List<Link>?,
)

data class CollectionsResponse(
    @SerializedName("data") val data: List<Collection>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int,
)