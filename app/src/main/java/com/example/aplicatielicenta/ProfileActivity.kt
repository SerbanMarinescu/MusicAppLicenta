package com.example.aplicatielicenta

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.adapters.LikedSongsAdapter
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.data.User
import com.example.aplicatielicenta.other.Constants
import com.example.aplicatielicenta.ui.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var profileImage: CircleImageView
    private lateinit var username: TextView
    private lateinit var logOutBtn: Button
    private lateinit var editAccountBtn: Button
    private lateinit var account: FirebaseAuth
    private lateinit var rvLike: RecyclerView
    private lateinit var likedSongsAdapter: LikedSongsAdapter
    private lateinit var songList: List<Song>

    private val mainViewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        logOutBtn = findViewById(R.id.btn_logOut)
        profileImage = findViewById(R.id.image_profile)
        username = findViewById(R.id.etUsernameProfile)
        editAccountBtn = findViewById(R.id.btnEditAccount)
        rvLike = findViewById(R.id.rvLikedSongs)
        account = FirebaseAuth.getInstance()


        val myPrefs = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val prefsEditor = myPrefs.edit()

        userInfo()

        lifecycleScope.launch(Dispatchers.IO){

            songList = getSongs()

            withContext(Dispatchers.Main){

                likedSongsAdapter = LikedSongsAdapter(glide, songList)

                likedSongsAdapter.setItemClickListener {
                    mainViewModel.playOrToggleSong(it)
                }

                rvLike.adapter = likedSongsAdapter
                rvLike.layoutManager = LinearLayoutManager(this@ProfileActivity, LinearLayoutManager.HORIZONTAL, false)
                rvLike.addItemDecoration(object : RecyclerView.ItemDecoration(){
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.right = resources.getDimensionPixelSize(R.dimen.horizontal_spacing) // Set spacing between items (right margin)
                    }
                })
            }
        }




        editAccountBtn.setOnClickListener{
            startActivity(Intent(this, EditAccountActivity::class.java))
        }

        logOutBtn.setOnClickListener{
            account.signOut()
            prefsEditor.clear()
            prefsEditor.apply()
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    private fun userInfo(){

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(account.currentUser!!.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val user = snapshot.getValue(User::class.java)
                    glide.load(user!!.imageUrl).into(profileImage)
                    username.text = user.username
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    suspend fun getSongIds(): List<String> = suspendCoroutine { continuation ->



        val songIdList = mutableListOf<String>()
        val likeRef = FirebaseDatabase.getInstance().reference
            .child("Liked").child(FirebaseAuth.getInstance().currentUser!!.uid)

        likeRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){

                    songIdList.clear()

                    for(snap in snapshot.children){
                        val songId = snap.key
                        songIdList.add(songId!!)
                    }

                    continuation.resume(songIdList)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    suspend fun getSongs(): List<Song>{

        val songIds = getSongIds()

        val fireStore = FirebaseFirestore.getInstance().collection(Constants.SONG_COLLECTION)

        val songCollection = fireStore.whereIn("mediaId", songIds)

        return songCollection.get().await().toObjects(Song::class.java)
    }
}