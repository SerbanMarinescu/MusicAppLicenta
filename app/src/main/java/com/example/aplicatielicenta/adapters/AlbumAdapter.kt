package com.example.aplicatielicenta.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Album
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

class AlbumAdapter @Inject constructor(private val glide: RequestManager, private val albumList: List<Album>) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.album_layout, parent, false)
        return AlbumViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {

        val album = albumList[position]

        holder.albumName.text = album.name
        glide.load(getDrawableResourceId(holder.itemView.context, album.name.lowercase())).into(holder.albumImage)
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    private fun getDrawableResourceId(context: Context, drawableName: String): Int {
        return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    }

    inner class AlbumViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val albumName: TextView
        val albumImage: CircleImageView

        init {
            albumName = itemView.findViewById(R.id.etAlbumName)
            albumImage = itemView.findViewById(R.id.cim_album)
        }
    }
}