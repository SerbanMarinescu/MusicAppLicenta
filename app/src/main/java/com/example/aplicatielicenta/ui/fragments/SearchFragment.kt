package com.example.aplicatielicenta.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {

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

}