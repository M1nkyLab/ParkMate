package com.example.parkmate

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btnlogreg : Button = findViewById(R.id.btnlogreg)

        btnlogreg.setOnClickListener {
            val intent = Intent(this, LoginRegister::class.java)
            startActivity(intent)
        }
    }
}