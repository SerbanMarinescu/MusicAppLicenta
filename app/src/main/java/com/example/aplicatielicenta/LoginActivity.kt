package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var logIn: Button
    private lateinit var goSgnUp: Button
    private lateinit var account: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.etEmail_LogIn)
        password = findViewById(R.id.etPassword_LogIn)
        logIn = findViewById(R.id.btn_LogIn)
        goSgnUp = findViewById(R.id.btn_goSgnUp)
        account = FirebaseAuth.getInstance()

        logIn.setOnClickListener {
            val email_user = email.text.toString()
            val password_user = password.text.toString()

            if (TextUtils.isEmpty(email_user)) {
                Toast.makeText(this, "Please specify Email!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password_user)) {
                Toast.makeText(this, "Please specify Password!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            logIn(email_user, password_user)
        }

        goSgnUp.setOnClickListener {
            startActivity(Intent(this, SgnUpActivity::class.java))
        }
    }

    private fun logIn(email: String, password: String) {

        account.signInWithEmailAndPassword(email, password).addOnCompleteListener {

            if (it.isSuccessful) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            } else {
                Toast.makeText(this, "Error logging in", Toast.LENGTH_LONG).show()
            }
        }
    }
}