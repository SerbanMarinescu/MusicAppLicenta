package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SgnUpActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sgnUp: Button
    private lateinit var goLogIn: Button
    private lateinit var account: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sgn_up)

        username = findViewById(R.id.etUsername_SgnUp)
        email = findViewById(R.id.etEmail_SgnUp)
        password = findViewById(R.id.etPassword_SgnUp)
        sgnUp = findViewById(R.id.btn_SgnUp)
        goLogIn = findViewById(R.id.btn_goLogIn)
        account = FirebaseAuth.getInstance()
        progressBar = findViewById(R.id.ProgressBar_SgnUp)


        sgnUp.setOnClickListener{
            val username_user = username.text.toString()
            val email_user = email.text.toString()
            val password_user = password.text.toString()

            if(TextUtils.isEmpty(username_user)){
                Toast.makeText(this, "Please specify Username", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(email_user)){
                Toast.makeText(this, "Please specify Email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(password_user)){
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

        //ProgressBar start
        progressBar.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)


        account.createUserWithEmailAndPassword(email, password).addOnCompleteListener {

            if(it.isSuccessful){
                saveUserInfo(username, email, password)
                startActivity(Intent(this, MainActivity::class.java))
            }
            else{
                Toast.makeText(this, "Error: ${it.exception.toString()}", Toast.LENGTH_LONG).show()
                account.signOut()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                progressBar.visibility = View.GONE
            }
        }


        //ProgressBar stop
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        progressBar.visibility = View.GONE
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