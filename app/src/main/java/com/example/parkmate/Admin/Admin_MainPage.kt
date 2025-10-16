package com.example.parkmate.Admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.Auth.Auth_LoginRegister
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth

class Admin_MainPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var adminWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_mainpage)

        auth = FirebaseAuth.getInstance()

        // Initialize UI
        adminWelcome = findViewById(R.id.adminTitle) // or a TextView for welcome message
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val manageparking: Button = findViewById(R.id.manageparking)

        // 🔹 Set welcome message dynamically
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Admin"
            adminWelcome.text = "Welcome, Admin $displayName"
        }

        // 🔹 Logout
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Auth_LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 🔹 Navigate to Manage Parking Page
        manageparking.setOnClickListener {
            val intent = Intent(this, Admin_Manage_ParkingSlot::class.java)
            startActivity(intent)
        }
    }
}
