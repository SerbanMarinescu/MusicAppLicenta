package com.example.aplicatielicenta.adapters

import android.content.Context
import android.media.Image
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.AsyncListDiffer
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject

class SongAdapter @Inject constructor(private val glide: RequestManager, ): BaseSongAdapter(R.layout.song_layout) {

    private var playlistClickListener: PlaylistClickListener? = null

    fun setPlaylistClickListener(listener: PlaylistClickListener) {
        playlistClickListener = listener
    }

    override val differ = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        val songName = holder.itemView.findViewById<TextView>(R.id.tw_song_name)
        val songWriter = holder.itemView.findViewById<TextView>(R.id.tw_song_writer)
        val songImage = holder.itemView.findViewById<ImageView>(R.id.im_song)

        val likeBtn = holder.itemView.findViewById<ImageView>(R.id.btn_like)
        val playlistBtn = holder.itemView.findViewById<ImageView>(R.id.btn_playlist)

        playlistBtn.setOnClickListener{
            showPlaylistOptionsDialog(holder.itemView.context, song)
        }

        getLikes(song.mediaId, likeBtn, song)

        likeBtn.setOnClickListener{

            if(!song.isLiked){
                likeBtn.setImageResource(R.drawable.heart_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).setValue(true)
                song.isLiked = true
            }
            else{
                likeBtn.setImageResource(R.drawable.heart_not_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).removeValue()
                song.isLiked = false
            }
        }

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

    private fun showPlaylistOptionsDialog(context: Context, song: Song) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add to Playlist")
            .setPositiveButton("Create New Playlist") { dialog, _ ->
                playlistClickListener?.onCreateNewPlaylistClicked(song)
                dialog.dismiss()
            }
            .setNegativeButton("Add to Existing Playlist") { dialog, _ ->
                playlistClickListener?.onAddToExistingPlaylistClicked(song)
                dialog.dismiss()
            }


        dialog.show()

    }

    private fun getLikes(mediaId: String, likeBtn: ImageView, song: Song) {

        val likeRef = FirebaseDatabase.getInstance().reference.child("Liked")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        likeRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(mediaId).exists()){
                    likeBtn.setImageResource(R.drawable.heart_filled)
                    song.isLiked = true
                }
                else{
                    likeBtn.setImageResource(R.drawable.heart_not_filled)
                    song.isLiked = false
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    fun updateSongs(song: List<Song>){
        differ.submitList(song)
    }

}