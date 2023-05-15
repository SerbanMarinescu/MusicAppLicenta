package com.example.aplicatielicenta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var mystring: String

    private lateinit var tw_main:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tw_main = findViewById(R.id.tw_main)

        tw_main.text = mystring
    }
}