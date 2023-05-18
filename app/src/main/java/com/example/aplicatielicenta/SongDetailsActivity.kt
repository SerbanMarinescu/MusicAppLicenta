package com.example.aplicatielicenta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.exoplayer.toSong
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.fragments.SongFragment
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.example.aplicatielicenta.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SongDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_details)

        val songFragment = SongFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_song, songFragment)
            .commit()
    }
}