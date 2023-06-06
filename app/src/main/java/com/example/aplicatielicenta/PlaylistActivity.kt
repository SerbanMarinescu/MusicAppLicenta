package com.example.aplicatielicenta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.adapters.AlbumSongAdapter
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class PlaylistActivity : AppCompatActivity(), PlaylistClickListener {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var playlistName: String

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var playlistNameTW: TextView
    private lateinit var rvPlaylist: RecyclerView
    private lateinit var songList: MutableList<Song>
    private lateinit var songAdapterPlaylist: AlbumSongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        playlistNameTW = findViewById(R.id.etPlaylistName)
        rvPlaylist = findViewById(R.id.rvPlaylists)

        playlistName = intent.getStringExtra("PlaylistName")!!

        playlistNameTW.text = playlistName
        rvPlaylist.layoutManager = LinearLayoutManager(this)


        lifecycleScope.launch(Dispatchers.IO){
            songList = getSongs() as MutableList<Song>

            withContext(Dispatchers.Main){
                songAdapterPlaylist = AlbumSongAdapter(glide, songList, this@PlaylistActivity, true, playlistName)
                rvPlaylist.adapter = songAdapterPlaylist

                songAdapterPlaylist.setItemClickListener {
                    mainViewModel.playOrToggleSong(it)
                }
            }

        }



    }

    override fun onAddToExistingPlaylistClicked(song: Song) {
        showExistingPlaylistsDialog(song)
    }

    override fun onCreateNewPlaylistClicked(song: Song) {
        showNewPlaylistNameDialog(song)
    }


    suspend fun getSongsIds(): List<String> = suspendCoroutine{ continuation ->
        val playlistRef = FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(playlistName)

        val songIdList = mutableListOf<String>()

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    songIdList.clear()
                    for(snap in snapshot.children){
                        val songId = snap.key
                        songIdList.add(songId!!)
                    }
                    continuation.resume(songIdList)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    suspend fun getSongs(): List<Song>{

        val songIds = getSongsIds()

        val fireStore = FirebaseFirestore.getInstance().collection(SONG_COLLECTION)

        val songCollection = fireStore.whereIn("mediaId", songIds)

        return songCollection.get().await().toObjects(Song::class.java)
    }

    private fun showNewPlaylistNameDialog(song: Song) {
        val input = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Create New Playlist")
            .setView(input)
            .setPositiveButton("Create") { dialog, _ ->
                val playlistName = input.text.toString()
                createPlaylist(playlistName, song)
                Toast.makeText(this, "Playlist $playlistName created successfully", Toast.LENGTH_LONG).show()
                // Logic to create a new playlist with the entered name
                // Handle playlist creation
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }


    private fun showExistingPlaylistsDialog(song: Song) {

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_existing_playlists, null)

        val listViewPlaylists = dialogView.findViewById<ListView>(R.id.lw_dialog)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Existing Playlists")

        // Get the list of existing playlist names (e.g., from the Realtime Database)
        CoroutineScope(Dispatchers.IO).launch{
            val playlistNames = getPlaylistNames()

            withContext(Dispatchers.Main){
                // Create an ArrayAdapter to populate the ListView with playlist names
                val adapter = ArrayAdapter(this@PlaylistActivity, android.R.layout.simple_list_item_1, playlistNames)
                listViewPlaylists.adapter = adapter

                // Set the item click listener for the ListView
                listViewPlaylists.setOnItemClickListener { _, _, position, _ ->
                    val selectedPlaylist = playlistNames[position]
                    // Handle the selection (e.g., add the song to the selected playlist)
                    addToPlaylist(selectedPlaylist, song)
                    Toast.makeText(this@PlaylistActivity, "Successfully added to $selectedPlaylist playlist!", Toast.LENGTH_LONG).show()

                }

                // Set the custom layout to the AlertDialog
                dialog.setView(dialogView).create()


                dialog.show()
            }
        }



    }

    private suspend fun getPlaylistNames(): MutableList<String> = suspendCoroutine{ continuation ->

        val playlistRef = FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        var playlistNames = mutableListOf<String>()

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    for(snap in snapshot.children){
                        val name = snap.key!!
                        playlistNames.add(name)
                    }
                    continuation.resume(playlistNames)
                }
                else{
                    continuation.resume(emptyList<String>() as MutableList<String>)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume(emptyList<String>() as MutableList<String>)
            }

        })

    }

    private fun addToPlaylist(selectedPlaylist: String, song: Song) {

        val playlistRef = FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(selectedPlaylist)

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()){
                    val existingMediaIds = mutableListOf<String>()

                    for(snap in snapshot.children){
                        val mediaId = snap.key!!
                        existingMediaIds.add(mediaId)
                    }


                    existingMediaIds.add(song.mediaId)

                    val updates = mutableMapOf<String, Any>()

                    for (mediaId in existingMediaIds) {
                        updates[mediaId] = true
                    }

                    playlistRef.updateChildren(updates)
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun createPlaylist(playlistName: String, song: Song) {

        val songToAdd = mapOf<String, Any>(
            song.mediaId to true
        )

        FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(playlistName).setValue(songToAdd)
    }


}