package com.example.aplicatielicenta.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

class LikedSongsAdapter @Inject constructor(private val glide: RequestManager, private val songList: List<Song>)
    : RecyclerView.Adapter<LikedSongsAdapter.LikedSongsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikedSongsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.album_layout, parent, false)
        return LikedSongsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LikedSongsViewHolder, position: Int) {

        val song = songList[position]

        glide.load(song.imageUrl).into(holder.songImage)
        holder.songName.text = song.title

        holder.itemView.setOnClickListener{
            onItemClickListener?.let {
                it(song)
            }
        }
    }

    override fun getItemCount(): Int {
        return songList.size
    }


    inner class LikedSongsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val songName: TextView
        val songImage: CircleImageView

        init {
            songName = itemView.findViewById(R.id.etAlbumName)
            songImage = itemView.findViewById(R.id.cim_album)
        }
    }

    var onItemClickListener: ((Song) -> Unit)? = null

    fun setItemClickListener(listener: (Song) -> Unit){
        onItemClickListener = listener
    }

}