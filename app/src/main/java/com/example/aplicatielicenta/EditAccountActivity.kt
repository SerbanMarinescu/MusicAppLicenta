package com.example.aplicatielicenta

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class EditAccountActivity : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var username: EditText
    private lateinit var saveChangesBtn: Button
    private lateinit var selectedImageUri: Uri
    private lateinit var accout: FirebaseAuth
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        profileImage = findViewById(R.id.image_profile)
        username = findViewById(R.id.etUsernameProfile)
        saveChangesBtn = findViewById(R.id.btnSaveChanges)
        accout = FirebaseAuth.getInstance()

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
            uploadImageAndUpdateDatabase(selectedImageUri)
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun uploadImageAndUpdateDatabase(selectedImageUri: Uri) {

        val storageRef = FirebaseStorage.getInstance().reference.child("ProfileImages")
            .child(System.currentTimeMillis().toString()+".jpg")

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(accout.currentUser!!.uid)


        storageRef.putFile(selectedImageUri).addOnSuccessListener {

            storageRef.downloadUrl.addOnSuccessListener{

                val updateMap = mapOf<String, Any>(
                    "imageUrl" to it.toString()
                    //"username" to username.text.toString()
                )

                userRef.updateChildren(updateMap)
            }.addOnCompleteListener {
                dialog.dismiss()
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
}