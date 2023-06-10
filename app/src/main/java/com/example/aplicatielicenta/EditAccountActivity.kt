package com.example.aplicatielicenta

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class EditAccountActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var profileImage: CircleImageView
    private lateinit var username: EditText
    private lateinit var saveChangesBtn: Button
    private  var selectedImageUri: Uri? = null
    private lateinit var account: FirebaseAuth
    private lateinit var dialog: AlertDialog

    var checker = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        profileImage = findViewById(R.id.image_profile)
        username = findViewById(R.id.etUsernameProfile)
        saveChangesBtn = findViewById(R.id.btnSaveChanges)
        account = FirebaseAuth.getInstance()


        val dialogView = LayoutInflater.from(this).inflate(R.layout.progress_bar_layout,null)
        val pbTitle = dialogView.findViewById<TextView>(R.id.progressBar_title)
        pbTitle.text = "Saving Changes..."
        dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        profileImage.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 0)
        }
        
        saveChangesBtn.setOnClickListener{
            dialog.show()

            runBlocking {

                if(selectedImageUri != null){
                    checker = "image"
                }

                if(!TextUtils.isEmpty(username.text.toString())){
                    checker = "username"
                }

                if(selectedImageUri != null && !TextUtils.isEmpty(username.text.toString())){
                    checker = "all"
                }


                when(checker){
                    "image" ->  uploadImageAndUpdateDatabase(selectedImageUri!!)
                    "username" -> updateUserInfo()
                    "all" -> {
                        uploadImageAndUpdateDatabase(selectedImageUri!!)
                        updateUserInfo()
                        Toast.makeText(this@EditAccountActivity, "Info updated successfully", Toast.LENGTH_LONG).show()
                    }

                    else -> Unit
                }
                dialog.dismiss()
            }

            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        setUserImage()
    }

    private fun setUserImage() {

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(account.currentUser!!.uid)

        userRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val user = snapshot.getValue(User::class.java)
                    glide.load(user!!.imageUrl).into(profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun updateUserInfo(){

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(account.currentUser!!.uid)

        val username = username.text.toString()

        if(TextUtils.isEmpty(username)){
            Toast.makeText(this, "No username specified", Toast.LENGTH_LONG).show()
        }
        else{
            val userData = mapOf<String, Any>(
                "username" to username
            )


            userRef.updateChildren(userData)
            Toast.makeText(this, "Username set", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadImageAndUpdateDatabase(selectedImageUri: Uri) {

        val storageRef = FirebaseStorage.getInstance().reference.child("ProfileImages")
            .child(System.currentTimeMillis().toString()+".jpg")

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(account.currentUser!!.uid)


        storageRef.putFile(selectedImageUri).addOnSuccessListener {

            storageRef.downloadUrl.addOnSuccessListener{

                val updateMap = mapOf<String, Any>(
                    "imageUrl" to it.toString(),
                    //"username" to username.text.toString()
                )

                userRef.updateChildren(updateMap)
                glide.load(selectedImageUri).into(profileImage)
            }.addOnCompleteListener {
                Toast.makeText(this, "Image set", Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data!!
            profileImage.setImageURI(selectedImageUri)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}