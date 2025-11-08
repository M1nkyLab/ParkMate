package com.example.parkmate.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.example.parkmate.User.User_Home
import com.google.firebase.auth.FirebaseAuth

class Auth_LoginRegister : AppCompatActivity() {

    // Firebase authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_loginregister)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Buttons
        val goToLogin: Button = findViewById(R.id.goToLogin)
        val goToRegister: Button = findViewById(R.id.goToRegister)

        goToLogin.setOnClickListener {
            startActivity(Intent(this, Auth_Login::class.java))
        }

        goToRegister.setOnClickListener {
            startActivity(Intent(this, Auth_Register::class.java))
        }
    }

    // Auto-login if already signed in
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, User_Home::class.java))
            finish()
        }
    }
}
