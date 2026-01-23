package com.linktine.data.types

import com.google.gson.annotations.SerializedName

data class UsernameUpdate(
    @SerializedName("name") val name: String,
)