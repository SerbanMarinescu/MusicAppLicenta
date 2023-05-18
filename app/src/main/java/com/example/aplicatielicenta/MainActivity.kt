package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.adapters.SwipeSongAdapter
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.exoplayer.isPlaying
import com.example.aplicatielicenta.exoplayer.toSong
import com.example.aplicatielicenta.other.Status
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var vpSong: ViewPager2
    private lateinit var curSongImg: ImageView
    private lateinit var playPauseImg: ImageView
    private lateinit var rootLayout: RelativeLayout
    private lateinit var profileImg: CircleImageView

    private var curPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavView = findViewById(R.id.bottom_nav_view)
        vpSong = findViewById(R.id.vpSong)
        curSongImg = findViewById(R.id.ivCurSong)
        playPauseImg = findViewById(R.id.ivPlayPause)
        rootLayout = findViewById(R.id.rootLayout)
        profileImg = findViewById(R.id.iv_profile)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavView.setupWithNavController(navController)

        subscribeToObservers()

        vpSong.adapter = swipeSongAdapter

        playPauseImg.setOnClickListener{
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        vpSong.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if(playbackState?.isPlaying == true){
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                }
                else{
                    curPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        swipeSongAdapter.setItemClickListener {
            startActivity(Intent(this, SongDetailsActivity::class.java))
        }

        profileImg.setOnClickListener{

        }

    }

    private fun switchViewPagerToCurrentSong(song: Song){
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if(newItemIndex != -1){
            vpSong.currentItem = newItemIndex
            curPlayingSong = song
        }
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(this){
            it?.let { result ->
                when(result.status){
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
                            if(songs.isNotEmpty()){
                                glide.load((curPlayingSong ?:songs[0]).imageUrl).into(curSongImg)
                            }
                            switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
                        }
                    }
                    Status.ERROR -> Unit
                    Status.LOADING -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(this){
            if(it == null) return@observe

            curPlayingSong = it.toSong()
            glide.load(curPlayingSong?.imageUrl).into(curSongImg)
            switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
        }

        mainViewModel.playbackState.observe(this){
            playbackState = it
            playPauseImg.setImageResource(
                if(playbackState?.isPlaying == true) R.drawable.img_pause else R.drawable.img_play
            )
        }

        mainViewModel.isConnected.observe(this){
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status){
                    Status.ERROR -> Snackbar.make(rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this){
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status){
                    Status.ERROR -> Snackbar.make(rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }
    }
}