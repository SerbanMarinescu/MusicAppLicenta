package com.example.aplicatielicenta.other

import com.example.aplicatielicenta.data.Song

interface PlaylistClickListener {

    fun onAddToExistingPlaylistClicked(song: Song)
    fun onCreateNewPlaylistClicked(song: Song)
}