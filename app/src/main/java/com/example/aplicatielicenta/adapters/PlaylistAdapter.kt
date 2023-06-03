package com.example.aplicatielicenta.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Playlist

class PlaylistAdapter(private val playlist: List<Playlist>) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_layout, parent, false)
        return PlaylistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlist[position]

        holder.playlistName.text = playlist.name
    }

    override fun getItemCount(): Int {
        return playlist.size
    }

    inner class PlaylistViewHolder(itemview: View): RecyclerView.ViewHolder(itemview){
        val playlistName: TextView

        init {
            playlistName = itemview.findViewById(R.id.etPlaylist)
        }
    }

}