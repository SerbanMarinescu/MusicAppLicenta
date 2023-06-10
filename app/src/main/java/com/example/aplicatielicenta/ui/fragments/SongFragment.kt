package com.example.aplicatielicenta.ui.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    private lateinit var likeBtn: ImageView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        songName = view.findViewById(R.id.tw_song_name)
        btnPlayPause = view.findViewById(R.id.btn_Play_Pause_Song)
        seekBar = view.findViewById(R.id.seekBar)
        curTimeSong = view.findViewById(R.id.tw_cur_time)
        songDuration = view.findViewById(R.id.tw_song_duration)
        skipNext = view.findViewById(R.id.btn_skip)
        skipPrevious = view.findViewById(R.id.btn_previous)
        likeBtn = view.findViewById(R.id.btn_Like)

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

        startScrollingAnimation()

        getLikes(song.mediaId, likeBtn, song)

        likeBtn.setOnClickListener{
            if(!song.isLiked){
                likeBtn.setImageResource(R.drawable.heart_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).setValue(true)
                song.isLiked = true
            }
            else{
                likeBtn.setImageResource(R.drawable.heart_not_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).removeValue()
                song.isLiked = false
            }
        }

        //glide.load(song.imageUrl).into(songImage)
    }



    private fun startScrollingAnimation() {

        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, -1.0f,
            Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f
        )
        animation.duration = 8000 // Duration for one complete animation cycle
        animation.repeatCount = Animation.INFINITE // Infinite animation loop
        animation.interpolator = null // Use default interpolator for linear animation

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)

        songName.startAnimation(animationSet)

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

    private fun getLikes(mediaId: String, likeBtn: ImageView, song: Song) {

        val likeRef = FirebaseDatabase.getInstance().reference.child("Liked")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        likeRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(mediaId).exists()){
                    likeBtn.setImageResource(R.drawable.heart_filled)
                    song.isLiked = true
                }
                else{
                    likeBtn.setImageResource(R.drawable.heart_not_filled)
                    song.isLiked = false
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}