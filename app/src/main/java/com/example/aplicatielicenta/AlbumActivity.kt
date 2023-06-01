package com.example.aplicatielicenta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

@AndroidEntryPoint
class AlbumActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var albumImg: CircleImageView
    private lateinit var albumName: TextView
    private lateinit var albumRv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        albumImg = findViewById(R.id.ivAlbum)
        albumName = findViewById(R.id.etAlbum)
        albumRv = findViewById(R.id.rvSongGenres)

        val img = intent.getIntExtra("albumImage",0)
        val name = intent.getStringExtra("albumName")

        albumName.text = name
        glide.load(img).into(albumImg)
    }
}