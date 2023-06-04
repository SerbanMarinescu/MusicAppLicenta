package com.example.aplicatielicenta.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.adapters.AllSongsAdapter
import com.example.aplicatielicenta.adapters.SongAdapter
import com.example.aplicatielicenta.data.Song
import com.google.firebase.firestore.FirebaseFirestore
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
class SearchFragment : Fragment(R.layout.fragment_search), PlaylistClickListener {

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var songAdapter: SongAdapter

    private lateinit var rvAllSongs: RecyclerView
    private lateinit var allSongsAdapter: AllSongsAdapter
    private lateinit var searchText: EditText

    lateinit var mainViewModel: MainViewModel

    private var displayedSongs: List<Song>? = null

    private val fragmentScope = CoroutineScope(Dispatchers.Main)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchText = view.findViewById(R.id.et_Search)
        rvAllSongs = view.findViewById(R.id.rw_Search)

        rvAllSongs.layoutManager = LinearLayoutManager(context)
        //allSongsAdapter = AllSongsAdapter(glide)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        songAdapter.setPlaylistClickListener(this@SearchFragment)


        songAdapter.setItemClickListener {
            mainViewModel.playOrToggleSong(it)
        }

        rvAllSongs.adapter = songAdapter


        subscribeToObservers()



        searchText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val searchQuery = p0.toString().trim()

                if(searchQuery.isEmpty()){
                    resetSongList()
                }
                else{
                    searchSongs(searchQuery)
                }

            }

            override fun afterTextChanged(p0: Editable?) {


            }

        })

    }

    private fun searchSongs(ch: String) {

        val filteredSongs = songAdapter.songs.filter {
            it.title.contains(ch, ignoreCase = true)
        }

        songAdapter.updateSongs(filteredSongs)
        songAdapter.notifyDataSetChanged()
    }

    private fun resetSongList() {

        lifecycleScope.launch(Dispatchers.Main){
            val list = getSongs()
            songAdapter.updateSongs(list)
            songAdapter.notifyDataSetChanged()
        }

    }


    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result ->
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

    suspend fun getSongs(): List<Song>{
        return FirebaseFirestore.getInstance().collection(SONG_COLLECTION).get().await().toObjects(Song::class.java)
    }


    override fun onAddToExistingPlaylistClicked(song: Song) {
        showExistingPlaylistsDialog(song)
    }

    override fun onCreateNewPlaylistClicked(song: Song) {
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