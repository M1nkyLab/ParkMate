package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R

class User_BookingSummary : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_bookingsummary)

        val btnProceedPayment = findViewById<Button>(R.id.btnProceedPayment)

        btnProceedPayment.setOnClickListener {
            // Navigate to Payment Page
            val intent = Intent(this, User_Payment::class.java)
            startActivity(intent)
        }
    }
}
