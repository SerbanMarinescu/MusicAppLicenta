package com.example.aplicatielicenta.adapters

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import javax.inject.Inject

class AlbumSongAdapter @Inject constructor(private val glide: RequestManager, private val songList: MutableList<Song>,
                                           private val playlistClickListener: PlaylistClickListener,
                                           private val isPlaylist: Boolean = false,
                                           private val playlistName: String
): RecyclerView.Adapter<AlbumSongAdapter.AlbumSongViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumSongViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.song_layout, parent, false)
        return AlbumSongViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AlbumSongViewHolder, position: Int) {

        val song = songList[position]

        holder.songName.text = song.title
        holder.songWriter.text = song.subtitle

        glide.load(song.imageUrl).into(holder.songImage)

        if(isPlaylist){
            holder.removeSong.apply {
                visibility = View.VISIBLE
                setOnClickListener{
                    removeSong(song)
                    songList.remove(song)
                    notifyDataSetChanged()
                }
            }
        }
        else{
            holder.removeSong.visibility = View.GONE
        }


        holder.playlistBtn.setOnClickListener{
            showPlaylistOptionsDialog(holder.itemView.context, song)
        }


        getLikes(song.mediaId, holder.likeBtn, song)

        holder.likeBtn.setOnClickListener{

            if(!song.isLiked){
                holder.likeBtn.setImageResource(R.drawable.heart_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).setValue(true)
                song.isLiked = true
            }
            else{
                holder.likeBtn.setImageResource(R.drawable.heart_not_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).removeValue()
                song.isLiked = false
            }
        }

        holder.itemView.setOnClickListener{
            onItemClickListener?.let {
                it(song)
            }
        }
    }

    private fun removeSong(song: Song) {
        val ref = FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(playlistName).child(song.mediaId).removeValue()
    }

    private fun showPlaylistOptionsDialog(context: Context, song: Song) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add to Playlist")
            .setPositiveButton("Create New Playlist") { dialog, _ ->
                playlistClickListener.onCreateNewPlaylistClicked(song)
                dialog.dismiss()
            }
            .setNegativeButton("Add to Existing Playlist") { dialog, _ ->
                playlistClickListener.onAddToExistingPlaylistClicked(song)
                dialog.dismiss()
            }


        dialog.show()
    }

    override fun getItemCount(): Int {
        return songList.size
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

    var onItemClickListener: ((Song) -> Unit)? = null

    fun setItemClickListener(listener: (Song) -> Unit){
        onItemClickListener = listener
    }

    inner class AlbumSongViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview){
        val songName: TextView
        val songWriter: TextView
        val songImage: ImageView
        var likeBtn: ImageView
        val playlistBtn: ImageView
        val removeSong: ImageView

        init {
            songName = itemView.findViewById(R.id.tw_song_name)
            songWriter = itemView.findViewById(R.id.tw_song_writer)
            songImage = itemView.findViewById(R.id.im_song)
            likeBtn = itemview.findViewById(R.id.btn_like)
            playlistBtn = itemview.findViewById(R.id.btn_playlist)
            removeSong = itemview.findViewById(R.id.btnRemoveSong)
        }
    }

}