package com.example.aplicatielicenta.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.exoplayer.isPlaying
import com.example.aplicatielicenta.exoplayer.toSong
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.example.aplicatielicenta.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import me.tankery.lib.circularseekbar.CircularSeekBar
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var curPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null
    private var shouldUpdateSeekbar = true

    private lateinit var songName: TextView
    private lateinit var btnPlayPause: ImageView
    private lateinit var seekBar: CircularSeekBar
    private lateinit var curTimeSong: TextView
    private lateinit var songDuration: TextView
    private lateinit var skipNext: ImageView
    private lateinit var skipPrevious: ImageView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        songName = view.findViewById(R.id.tw_song_name)
        btnPlayPause = view.findViewById(R.id.btn_Play_Pause_Song)
        seekBar = view.findViewById(R.id.seekBar)
        curTimeSong = view.findViewById(R.id.tw_cur_time)
        songDuration = view.findViewById(R.id.tw_song_duration)
        skipNext = view.findViewById(R.id.btn_skip)
        skipPrevious = view.findViewById(R.id.btn_previous)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        subscribeToObservers()

        btnPlayPause.setOnClickListener{
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }


        seekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener{

            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {
                if(fromUser){
                    setCurPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar = true
                }
            }

        })

        skipNext.setOnClickListener{
            mainViewModel.skipToPreviousSong()
        }

        skipPrevious.setOnClickListener{
            mainViewModel.skipToNextSong()
        }
    }

    private fun updateTitleAndSongImage(song: Song){
        val title = "${song.title} - ${song.subtitle}"
        songName.text = title
        //glide.load(song.imageUrl).into(ivSongImage)
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){
            it?.let { result ->
                when(result.status){
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if(curPlayingSong == null && songs.isNotEmpty()){
                                curPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner){
            if(it == null) return@observe
            curPlayingSong = it.toSong()
            updateTitleAndSongImage(curPlayingSong!!)
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState = it
            btnPlayPause.setImageResource(
                if(playbackState?.isPlaying == true) R.drawable.img_pause else R.drawable.img_play
            )
            seekBar.progress = (it?.position?.toInt() ?: 0).toFloat()
        }

        songViewModel.curPlayerPosition.observe(viewLifecycleOwner){
            if(shouldUpdateSeekbar){
                seekBar.progress = (it.toInt()).toFloat()
                setCurPlayerTimeToTextView(it)
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner){
            seekBar.max = (it.toInt()).toFloat()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            songDuration.text = dateFormat.format(it)
        }
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        curTimeSong.text = dateFormat.format(ms)
    }
}