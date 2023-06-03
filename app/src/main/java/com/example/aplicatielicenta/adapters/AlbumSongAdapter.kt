package com.example.aplicatielicenta.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import javax.inject.Inject

class AlbumSongAdapter @Inject constructor(private val glide: RequestManager, private val songList: List<Song>):
    RecyclerView.Adapter<AlbumSongAdapter.AlbumSongViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumSongViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.song_layout, parent, false)
        return AlbumSongViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AlbumSongViewHolder, position: Int) {

        val song = songList[position]

        holder.songName.text = song.title
        holder.songWriter.text = song.subtitle

        glide.load(song.imageUrl).into(holder.songImage)

        holder.itemView.setOnClickListener{
            onItemClickListener?.let {
                it(song)
            }
        }
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    var onItemClickListener: ((Song) -> Unit)? = null

    fun setItemClickListener(listener: (Song) -> Unit){
        onItemClickListener = listener
    }

    inner class AlbumSongViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview){
        val songName: TextView
        val songWriter: TextView
        val songImage: ImageView

        init {
            songName = itemView.findViewById(R.id.tw_song_name)
            songWriter = itemView.findViewById(R.id.tw_song_writer)
            songImage = itemView.findViewById(R.id.im_song)
        }
    }

}