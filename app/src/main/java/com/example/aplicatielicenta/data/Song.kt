package com.example.aplicatielicenta.data

data class Song(

    val mediaId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val songUrl: String = "",
    val imageUrl: String = "",
    val genre: String = "",
    val year: Long = 0,
    val purpose: String = "",
    val type: String = "",
    var isLiked: Boolean = false
)