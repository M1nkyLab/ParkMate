package com.example.parkmate.Auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.example.parkmate.User.User_Home
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class Auth_Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailInput: EditText = findViewById(R.id.registerEmailInput)
        val usernameInput: EditText = findViewById(R.id.registerUsernameInput)
        val passwordInput: EditText = findViewById(R.id.registerPasswordInput)
        val confirmPasswordInput: EditText = findViewById(R.id.registerConfirmPasswordInput)
        val registerButton: Button = findViewById(R.id.registerButton)
        val loginRedirect: TextView = findViewById(R.id.loginRedirect)

        loginRedirect.setOnClickListener {
            val intent = Intent(this, Auth_Login::class.java)
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                // Get FCM token FIRST
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    val fcmToken = if (tokenTask.isSuccessful) {
                                        tokenTask.result
                                    } else {
                                        Log.w("FCM", "Fetching FCM token failed, saving empty token.", tokenTask.exception)
                                        "" // Save an empty token if it fails
                                    }

                                    // Add all user data, INCLUDING the token, to the map
                                    val userData = hashMapOf(
                                        "email" to email,
                                        "username" to username,
                                        "role" to "user",
                                        "fcmToken" to fcmToken // Add token here
                                    )

                                    // Save everything in one operation
                                    firestore.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Log.d("REGISTER", "User profile created with FCM Token.")
                                            navigateToHome(username)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(this, "User ID not found after creation", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun navigateToHome(username: String) {
        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, User_Home::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }
}
