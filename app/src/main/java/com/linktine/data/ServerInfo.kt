package com.linktine.data

data class ServerInfo(
    val url: String,
    val token: String
) {
    companion object {
        // A placeholder for when no data has been saved yet
        val EMPTY = ServerInfo(url = "", token = "")
    }
}