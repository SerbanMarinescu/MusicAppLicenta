package com.example.aplicatielicenta.ui.fragments

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
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
import com.example.aplicatielicenta.adapters.SongAdapter
import com.example.aplicatielicenta.data.Album
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    @Inject
    lateinit var songAdapter: SongAdapter
    
    @Inject
    lateinit var glide: RequestManager

    lateinit var mainViewModel: MainViewModel

    private lateinit var rvAllSongs: RecyclerView
    private lateinit var rvAlbum: RecyclerView
    private lateinit var allSongsProgressBar: ProgressBar
    
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var albumList: List<Album>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        rvAllSongs = view.findViewById(R.id.rv_recommended)
        rvAlbum = view.findViewById(R.id.rv_albums)
        allSongsProgressBar = view.findViewById(R.id.allSongsProgressBar)

        setupRecyclerView()
        subscribeToObservers()

        lifecycleScope.launch(Dispatchers.Main){

            albumList = getAlbumList()

            albumAdapter = AlbumAdapter(glide, albumList)

            rvAlbum.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
            rvAlbum.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.horizontal_spacing),
                resources.getDimensionPixelSize(R.dimen.vertical_spacing)))
            rvAlbum.adapter = albumAdapter
        }
        


        songAdapter.setItemClickListener {
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

    private fun setupRecyclerView() = rvAllSongs.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result ->
            when(result.status){
                Status.SUCCESS -> {
                    allSongsProgressBar.isVisible = false
                    result.data?.let { songs ->
                        songAdapter.songs = songs
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

}