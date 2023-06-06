package com.example.aplicatielicenta.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicatielicenta.PlaylistActivity
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Playlist
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PlaylistAdapter(private val playlist: MutableList<String>) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_layout, parent, false)
        return PlaylistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlistName = playlist[position]

        holder.playlistName.text = playlistName

        holder.deletePlaylist.setOnClickListener{
            deletePlaylist(playlistName)
            playlist.removeAt(position)
            notifyDataSetChanged()
        }

        holder.itemView.setOnClickListener{
            Intent(holder.itemView.context, PlaylistActivity::class.java).apply {
                putExtra("PlaylistName", playlistName)
                holder.itemView.context.startActivity(this)
            }
        }
    }

    private fun deletePlaylist(playlist: String) {
        FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(playlist).removeValue()
    }

    override fun getItemCount(): Int {
        return playlist.size
    }

    inner class PlaylistViewHolder(itemview: View): RecyclerView.ViewHolder(itemview){
        val playlistName: TextView
        val deletePlaylist: ImageView

        init {
            playlistName = itemview.findViewById(R.id.etPlaylist)
            deletePlaylist = itemview.findViewById(R.id.delete_playlist)
        }
    }

}