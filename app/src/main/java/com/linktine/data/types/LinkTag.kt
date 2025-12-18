package com.linktine.data.types

import com.google.gson.annotations.SerializedName

data class LinkTag(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("tagId") val tagId: String,
    @SerializedName("tag") val tag: Tag
)

data class Tag(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String,
    @SerializedName("createdAt") val createdAt: String
)
