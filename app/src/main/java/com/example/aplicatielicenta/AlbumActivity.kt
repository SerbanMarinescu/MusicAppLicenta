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
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.adapters.AlbumSongAdapter
import com.example.aplicatielicenta.adapters.SongAdapter
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
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class AlbumActivity : AppCompatActivity(), PlaylistClickListener {

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var songAdapter: SongAdapter

    private lateinit var albumImg: CircleImageView
    private lateinit var albumName: TextView
    private lateinit var albumRv: RecyclerView
    private lateinit var albumSongAdapter: AlbumSongAdapter

    private val mainViewModel: MainViewModel by viewModels()

    private var fireStore = FirebaseFirestore.getInstance()
    private var songCollection = fireStore.collection(SONG_COLLECTION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        albumImg = findViewById(R.id.ivAlbum)
        albumName = findViewById(R.id.etAlbum)
        albumRv = findViewById(R.id.rvSongGenres)


        albumRv.layoutManager = LinearLayoutManager(this)


        val img = intent.getIntExtra("albumImage",0)
        val name = intent.getStringExtra("albumName")

        var songs: List<Song>

        lifecycleScope.launch(Dispatchers.IO){
            songs = fetchData(name!!)

            withContext(Dispatchers.Main){
                albumSongAdapter = AlbumSongAdapter(glide,
                    songs as MutableList<Song>, this@AlbumActivity, false, "")
                albumRv.adapter = albumSongAdapter
                albumSongAdapter.notifyDataSetChanged()


                albumSongAdapter.setItemClickListener {
                    mainViewModel.playOrToggleSong(it)
                }
            }
        }


        albumName.text = name
        glide.load(img).into(albumImg)

        subscribeToObservers()
    }


    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(this){ result ->
            when(result.status){
                Status.SUCCESS -> {
                    //allSongsProgressBar.isVisible = false
                    result.data?.let { songs ->
                        songAdapter.songs = songs
                        //songAdapter.notifyDataSetChanged()
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> Unit //allSongsProgressBar.isVisible = true
            }
        }
    }

    suspend fun fetchData(genre: String): List<Song>{

        return songCollection.whereEqualTo("genre", genre).get().await().toObjects(Song::class.java)
    }

    override fun onAddToExistingPlaylistClicked(song: Song) {
        showExistingPlaylistsDialog(song)
    }

    override fun onCreateNewPlaylistClicked(song: Song) {
        showNewPlaylistNameDialog(song)
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
                val adapter = ArrayAdapter(this@AlbumActivity, android.R.layout.simple_list_item_1, playlistNames)
                listViewPlaylists.adapter = adapter

                // Set the item click listener for the ListView
                listViewPlaylists.setOnItemClickListener { _, _, position, _ ->
                    val selectedPlaylist = playlistNames[position]
                    // Handle the selection (e.g., add the song to the selected playlist)
                    addToPlaylist(selectedPlaylist, song)
                    Toast.makeText(this@AlbumActivity, "Successfully added to $selectedPlaylist playlist!", Toast.LENGTH_LONG).show()

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