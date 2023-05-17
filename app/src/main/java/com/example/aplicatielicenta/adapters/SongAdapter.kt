package com.example.aplicatielicenta.adapters

import android.media.Image
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import javax.inject.Inject

class SongAdapter @Inject constructor(private val glide: RequestManager): BaseSongAdapter(R.layout.song_layout) {

    override val differ = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        val songName = holder.itemView.findViewById<TextView>(R.id.tw_song_name)
        val songWriter = holder.itemView.findViewById<TextView>(R.id.tw_song_writer)
        val songImage = holder.itemView.findViewById<ImageView>(R.id.im_song)

        holder.itemView.apply {
            songName.text = song.title
            songWriter.text = song.subtitle
            glide.load(song.imageUrl).into(songImage)

            setOnClickListener {
                onItemClickListener?.let { click ->
                    click(song)
                }
            }
        }
    }
}