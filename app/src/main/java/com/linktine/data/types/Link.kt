package com.linktine.data.types

import com.google.gson.annotations.SerializedName

data class Link(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("name") val name: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("domain") val domain: String?,
    @SerializedName("read") val read: Boolean,
    @SerializedName("archived") val archived: Boolean,
    @SerializedName("favorite") val favorite: Boolean,
    @SerializedName("note") val note: String?,
    @SerializedName("userId") val userId: String,
    @SerializedName("collectionId") val collectionId: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("tags") val tags: List<LinkTag>,
    @SerializedName("collection") val collection: Collection?
)

data class LinkCreate(
    @SerializedName("url") val url: String,
    @SerializedName("name") val name: String? = null,
)

data class LinkUpdate(
    @SerializedName("name") val title: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("read") val read: Boolean? = null,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("favorite") val favorite: Boolean? = null,
    @SerializedName("collectionId") val collectionId: String? = "null",
)
