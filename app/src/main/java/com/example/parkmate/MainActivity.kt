package com.example.parkmate

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.User.User_Home

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        val btnstart : Button = findViewById(R.id.btnstart)

        btnstart.setOnClickListener {
            val intent = Intent(this, User_Home::class.java)
            startActivity(intent)
        }
    }
}