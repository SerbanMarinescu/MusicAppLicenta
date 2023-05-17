package com.example.aplicatielicenta.adapters

import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import com.example.aplicatielicenta.R

class SwipeSongAdapter: BaseSongAdapter(R.layout.swipe_item) {

    override val differ = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        val songName = holder.itemView.findViewById<TextView>(R.id.tw_song_name)

        holder.itemView.apply {

            val text = "${song.title} - ${song.subtitle}"
            songName.text = text

            setOnClickListener {
                onItemClickListener?.let { click ->
                    click(song)
                }
            }
        }
    }
}