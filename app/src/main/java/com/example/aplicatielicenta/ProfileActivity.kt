package com.example.aplicatielicenta

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var profileImage: CircleImageView
    private lateinit var username: TextView
    private lateinit var logOutBtn: Button
    private lateinit var editAccountBtn: Button
    private lateinit var account: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        logOutBtn = findViewById(R.id.btn_logOut)
        profileImage = findViewById(R.id.image_profile)
        username = findViewById(R.id.etUsernameProfile)
        editAccountBtn = findViewById(R.id.btnEditAccount)
        account = FirebaseAuth.getInstance()

        val myPrefs = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val prefsEditor = myPrefs.edit()

        userInfo()

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

        userRef.addValueEventListener(object : ValueEventListener{
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
}