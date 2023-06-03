package com.example.aplicatielicenta.ui.fragments

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.adapters.AlbumAdapter
import com.example.aplicatielicenta.adapters.AllSongsAdapter
import com.example.aplicatielicenta.adapters.SongAdapter
import com.example.aplicatielicenta.data.Album
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
class HomeFragment : Fragment(R.layout.fragment_home), PlaylistClickListener {

    @Inject
    lateinit var songAdapter: SongAdapter
    
    @Inject
    lateinit var glide: RequestManager

    lateinit var mainViewModel: MainViewModel

    private lateinit var rvRecommended: RecyclerView
    private lateinit var rvAlbum: RecyclerView
    private lateinit var allSongsProgressBar: ProgressBar
    
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var albumList: List<Album>

    private lateinit var allSongsAdapter: AllSongsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        rvRecommended = view.findViewById(R.id.rv_recommended)
        rvAlbum = view.findViewById(R.id.rv_albums)
        allSongsProgressBar = view.findViewById(R.id.allSongsProgressBar)

        allSongsAdapter = AllSongsAdapter(glide, this)

        lifecycleScope.launch(Dispatchers.IO){
            allSongsAdapter.songs = allSongsAdapter.getAllSongs() as MutableList<Song>

            withContext(Dispatchers.Main){
                rvRecommended.adapter = allSongsAdapter
                rvRecommended.layoutManager = LinearLayoutManager(requireContext())
                allSongsAdapter.notifyDataSetChanged()
            }
        }

        //setupRecyclerView()
        subscribeToObservers()

        lifecycleScope.launch(Dispatchers.Main){

            albumList = getAlbumList()

            albumAdapter = AlbumAdapter(glide, albumList)

            rvAlbum.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
            rvAlbum.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.horizontal_spacing),
                resources.getDimensionPixelSize(R.dimen.vertical_spacing)))
            rvAlbum.adapter = albumAdapter
        }
        


//        songAdapter.setItemClickListener {
//            mainViewModel.playOrToggleSong(it)
//        }

        allSongsAdapter.setItemClickListener {
            mainViewModel.playOrToggleSong(it)
        }
    }

    private suspend fun getAlbumList(): List<Album> = withContext(Dispatchers.IO){
        val songCollection = FirebaseFirestore.getInstance().collection(SONG_COLLECTION)

        val genres = ArrayList<String>()
        val albums = ArrayList<Album>()

        val querySnapshot = songCollection.get().await()

            for(document in querySnapshot.documents){
                val song = document.toObject(Song::class.java)
                val genre = song?.genre

                genre?.let {
                    if(!genres.contains(it)){
                        genres.add(it)

                        val album = Album(it)
                        albums.add(album)
                    }
                }
            }


        return@withContext albums
    }

    private fun setupRecyclerView() = rvRecommended.apply {
        adapter = allSongsAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result ->
            when(result.status){
                Status.SUCCESS -> {
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs ->
                        songAdapter.songs = songs as MutableList<Song>
                    }
                }
                Status.ERROR -> Unit
                Status.LOADING -> allSongsProgressBar.isVisible = true
            }
        }
    }

    inner class SpaceItemDecoration(private val horizontalSpacing: Int, private val verticalSpacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.right = horizontalSpacing
            outRect.bottom = verticalSpacing
        }
    }

    override fun onAddToExistingPlaylistClicked(song: Song) {
        // Show the list of existing playlists dialog
        showExistingPlaylistsDialog(song)
    }

    override fun onCreateNewPlaylistClicked(song: Song) {
        // Show the prompt to enter a new playlist name dialog
        showNewPlaylistNameDialog(song)
    }

    private fun showNewPlaylistNameDialog(song: Song) {
        val input = EditText(requireContext())
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New Playlist")
            .setView(input)
            .setPositiveButton("Create") { dialog, _ ->
                val playlistName = input.text.toString()
                createPlaylist(playlistName, song)
                Toast.makeText(context, "Playlist $playlistName created successfully", Toast.LENGTH_LONG).show()
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

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_existing_playlists, null)

        val listViewPlaylists = dialogView.findViewById<ListView>(R.id.lw_dialog)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Existing Playlists")

        // Get the list of existing playlist names (e.g., from the Realtime Database)
        CoroutineScope(Dispatchers.IO).launch{
            val playlistNames = getPlaylistNames()

            withContext(Dispatchers.Main){
                // Create an ArrayAdapter to populate the ListView with playlist names
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, playlistNames)
                listViewPlaylists.adapter = adapter

                // Set the item click listener for the ListView
                listViewPlaylists.setOnItemClickListener { _, _, position, _ ->
                    val selectedPlaylist = playlistNames[position]
                    // Handle the selection (e.g., add the song to the selected playlist)
                    addToPlaylist(selectedPlaylist, song)
                    Toast.makeText(requireContext(), "Successfully added to $selectedPlaylist playlist!", Toast.LENGTH_LONG).show()

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

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener{

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

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener{
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