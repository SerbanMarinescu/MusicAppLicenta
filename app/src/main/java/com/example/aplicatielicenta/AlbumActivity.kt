package com.example.aplicatielicenta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.adapters.AlbumSongAdapter
import com.example.aplicatielicenta.adapters.SongAdapter
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AlbumActivity : AppCompatActivity() {

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
                albumSongAdapter = AlbumSongAdapter(glide, songs)
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
}