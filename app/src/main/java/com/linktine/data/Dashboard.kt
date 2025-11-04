package com.linktine.data

import com.google.gson.annotations.SerializedName

data class Stats(
    @SerializedName("totalLinks") val totalLinks: Int,
    @SerializedName("readLinks") val readLinks: Int,
    @SerializedName("favoriteLinks") val favoriteLinks: Int,
    @SerializedName("archivedLinks") val archivedLinks: Int,
    @SerializedName("totalCollections") val totalCollections: Int,
    @SerializedName("totalTags") val totalTags: Int,
    @SerializedName("sharedLinks") val sharedLinks: Int,
    @SerializedName("sharedCollections") val sharedCollections: Int
)

// --- RECENT LINK ---
data class RecentLink(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    @SerializedName("createdAt") val createdAt: String
)

// --- RECENT COLLECTION ---
data class RecentCollection(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String
)

// --- TOP TAG ---
data class TopTag(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String,
    @SerializedName("count") val count: Int
)

// --- MAIN RESPONSE ---
data class DashboardResponse(
    @SerializedName("stats") val stats: Stats,
    @SerializedName("recentLinks") val recentLinks: List<RecentLink>,
    @SerializedName("recentCollections") val recentCollections: List<RecentCollection>,
    @SerializedName("topTags") val topTags: List<TopTag>
)