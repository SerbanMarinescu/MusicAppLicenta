package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint

class SgnUpActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sgnUp: Button
    private lateinit var goLogIn: Button
    private lateinit var account: FirebaseAuth
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sgn_up)

        username = findViewById(R.id.etUsername_SgnUp)
        email = findViewById(R.id.etEmail_SgnUp)
        password = findViewById(R.id.etPassword_SgnUp)
        sgnUp = findViewById(R.id.btn_SgnUp)
        goLogIn = findViewById(R.id.btn_goLogIn)
        account = FirebaseAuth.getInstance()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.progress_bar_layout,null)
        val pbTitle = dialogView.findViewById<TextView>(R.id.progressBar_title)
        pbTitle.text = "Creating account..."

        dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()


        sgnUp.setOnClickListener{

            dialog.show()

            val username_user = username.text.toString()
            val email_user = email.text.toString()
            val password_user = password.text.toString()

            if(TextUtils.isEmpty(username_user)){
                dialog.dismiss()
                Toast.makeText(this, "Please specify Username", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(email_user)){
                dialog.dismiss()
                Toast.makeText(this, "Please specify Email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(password_user)){
                dialog.dismiss()
                Toast.makeText(this, "Please specify Password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            createAccount(username_user, email_user, password_user)
        }

        goLogIn.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun createAccount(username: String, email: String, password: String){

        account.createUserWithEmailAndPassword(email, password).addOnCompleteListener {

            if(it.isSuccessful){
                saveUserInfo(username, email, password)
                dialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else{
                dialog.dismiss()
                Toast.makeText(this, "Error: ${it.exception.toString()}", Toast.LENGTH_LONG).show()
                account.signOut()
            }
        }

    }

    private fun saveUserInfo(username: String, email: String, password: String) {

        val currentUser = account.currentUser!!.uid
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(currentUser)

        val userData = mapOf<String, Any>(
            "uid" to currentUser,
            "username" to username,
            "email" to email,
            "password" to password
        )

        userRef.setValue(userData)

    }
}