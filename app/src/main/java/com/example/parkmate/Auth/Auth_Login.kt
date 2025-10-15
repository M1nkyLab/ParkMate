package com.example.parkmate.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.example.parkmate.User.User_Home
import com.example.parkmate.Admin.Admin_MainPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Auth_Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isNavigating = false // 🔹 Prevents double navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailInput: EditText = findViewById(R.id.loginEmailInput)
        val passwordInput: EditText = findViewById(R.id.loginPasswordInput)
        val loginButton: Button = findViewById(R.id.loginButton)
        val registerRedirect: TextView = findViewById(R.id.registerRedirect)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val userId = user.uid
                                firestore.collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val role = document.getString("role")
                                            if (role == "admin") {
                                                Toast.makeText(this, "Welcome Admin", Toast.LENGTH_SHORT).show()
                                                navigateTo(Admin_MainPage::class.java)
                                            } else {
                                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                                                navigateTo(User_Home::class.java)
                                            }
                                        } else {
                                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to get user role", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerRedirect.setOnClickListener {
            val intent = Intent(this, Auth_Register::class.java)
            startActivity(intent)
        }
    }

    // 🔹 Auto-login role check
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null && !isNavigating) {
            val userId = currentUser.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        if (role == "admin") {
                            navigateTo(Admin_MainPage::class.java)
                        } else {
                            navigateTo(User_Home::class.java)
                        }
                    }
                }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        if (!isNavigating) {
            isNavigating = true
            val intent = Intent(this, destination)
            startActivity(intent)
            finish()
        }
    }
}
