package com.linktine.data.types

import com.google.gson.annotations.SerializedName

data class PaginatedResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int
)