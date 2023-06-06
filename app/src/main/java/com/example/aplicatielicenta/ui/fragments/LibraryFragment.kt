package com.example.aplicatielicenta.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.adapters.PlaylistAdapter
import com.example.aplicatielicenta.data.Playlist
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private lateinit var rvPlaylists: RecyclerView
    private lateinit var playlistNames: List<String>
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var account: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPlaylists = view.findViewById(R.id.rvLibrary)

        account = FirebaseAuth.getInstance()

        rvPlaylists.layoutManager = LinearLayoutManager(context)


        lifecycleScope.launch(Dispatchers.IO) {
            playlistNames = getPlaylistNames()

            withContext(Dispatchers.Main){
                playlistAdapter = PlaylistAdapter(playlistNames as MutableList<String>)
                rvPlaylists.adapter = playlistAdapter
            }
        }

    }

    suspend fun getPlaylistNames(): List<String> = suspendCoroutine{ continuation ->

        val playlistRef = FirebaseDatabase.getInstance().reference.child("Playlists")
            .child(account.currentUser!!.uid)

        val playlistList = mutableListOf<String>()

        playlistRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){

                    for(snap in snapshot.children){
                        val playlist = snap.key
                        playlistList.add(playlist!!)
                    }
                    continuation.resume(playlistList)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

}