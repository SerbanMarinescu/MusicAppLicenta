package com.example.aplicatielicenta.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AllSongsAdapter @Inject constructor(private val glide: RequestManager) : RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {

    private val songCollection = FirebaseFirestore.getInstance().collection(SONG_COLLECTION)

    var songs: MutableList<Song> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSongsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.song_layout, parent, false)
        return AllSongsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AllSongsViewHolder, position: Int) {
        val song = songs[position]

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
        return songs.size
    }

    fun updateSongs(newSongs: MutableList<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }


    inner class AllSongsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

         var songImage: ImageView
         var songName: TextView
         var songWriter: TextView

        init {
            songImage = itemView.findViewById(R.id.im_song)
            songName = itemView.findViewById(R.id.tw_song_name)
            songWriter = itemView.findViewById(R.id.tw_song_writer)
        }
    }


     suspend fun getSongsFromFirestore(): List<Song> {

        return songCollection.get().await().toObjects(Song::class.java)
    }

    suspend fun fetchData() {
        songs = getSongsFromFirestore() as MutableList<Song>
        notifyDataSetChanged()
    }


    var onItemClickListener: ((Song) -> Unit)? = null

    fun setItemClickListener(listener: (Song) -> Unit){
        onItemClickListener = listener
    }

}