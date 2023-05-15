package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var logIn: Button
    private lateinit var goSgnUp: Button
    private lateinit var account: FirebaseAuth
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.etEmail_LogIn)
        password = findViewById(R.id.etPassword_LogIn)
        logIn = findViewById(R.id.btn_LogIn)
        goSgnUp = findViewById(R.id.btn_goSgnUp)
        account = FirebaseAuth.getInstance()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.progress_bar_layout,null)
        val pbTitle = dialogView.findViewById<TextView>(R.id.progressBar_title)
        pbTitle.text = "Logging in..."

        dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        logIn.setOnClickListener {

            dialog.show()

            val email_user = email.text.toString()
            val password_user = password.text.toString()

            if (TextUtils.isEmpty(email_user)) {
                dialog.dismiss()
                Toast.makeText(this, "Please specify Email!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password_user)) {
                dialog.dismiss()
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
                dialog.dismiss()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                Toast.makeText(this, "Error logging in", Toast.LENGTH_LONG).show()
            }
        }
    }
}