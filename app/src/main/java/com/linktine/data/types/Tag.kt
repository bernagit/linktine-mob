package com.linktine.data.types

import com.google.gson.annotations.SerializedName

data class LinkTag(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("tagId") val tagId: String,
    @SerializedName("tag") val tag: Tag,
)

data class TagLink(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("tagId") val tagId: String,
    @SerializedName("link") val link: Link
)

data class Tag(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("links") val links: List<TagLink>
)

data class TagCreate(
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String,
)

data class TagUpdateLinks(
    @SerializedName("linkIds") val linkIds: List<String>
)