package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R

class User_Payment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_payment)

        val btnPayNow = findViewById<Button>(R.id.btnPayNow)

        btnPayNow.setOnClickListener {
            // Navigate to Booking Complete Page
            val intent = Intent(this, User_CompletePayment::class.java)
            startActivity(intent)
        }
    }
}
