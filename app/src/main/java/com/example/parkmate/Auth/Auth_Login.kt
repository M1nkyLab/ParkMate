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
import com.example.parkmate.Admin.Admin_MainPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class Auth_Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isNavigating = false // Prevents double navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailInput: EditText = findViewById(R.id.loginEmailInput)
        val passwordInput: EditText = findViewById(R.id.loginPasswordInput)
        val loginButton: Button = findViewById(R.id.loginButton)
        val registerRedirect: TextView = findViewById(R.id.registerRedirect)
        val forgotPasswordText: TextView = findViewById(R.id.forgotPasswordText)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Step 1: Sign in the user
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user == null) {
                        Toast.makeText(this, "Login failed: User not found.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                    Log.d("LOGIN", "Authentication successful for ${user.email}")
                    // Step 2: User is signed in, now check their role
                    checkUserRole(user.uid)
                }
                .addOnFailureListener { e ->
                    // This happens if the password is wrong or the user doesn't exist in Auth
                    Log.w("LOGIN", "Authentication failed", e)
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        registerRedirect.setOnClickListener {
            val intent = Intent(this, Auth_Register::class.java)
            startActivity(intent)
        }

        forgotPasswordText.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserRole(userId: String) {
        // Step 3: Read the user's document from Firestore
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    Log.d("LOGIN", "User role is: $role")
                    // Step 4: Navigate based on role
                    if (role == "admin") {
                        navigateTo(Admin_MainPage::class.java)
                    } else {
                        navigateTo(User_Home::class.java)
                    }
                    // Also, update the FCM token in the background
                    updateFcmToken(userId)
                } else {
                    // This happens if the user exists in Auth but not in Firestore database
                    Log.w("LOGIN", "User document not found in Firestore for UID: $userId")
                    Toast.makeText(this, "User data not found. Please contact support.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                // This happens if Firestore rules are wrong or there's a network issue
                Log.e("LOGIN", "Failed to read user document", e)
                Toast.makeText(this, "Error checking user role: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val token = tokenTask.result
                firestore.collection("users").document(userId).update("fcmToken", token)
                    .addOnSuccessListener { Log.d("FCM", "FCM token updated successfully.") }
                    .addOnFailureListener { e -> Log.w("FCM", "Failed to update FCM token.", e) }
            } else {
                Log.w("FCM", "Fetching FCM registration token failed", tokenTask.exception)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null && !isNavigating) {
            checkUserRole(currentUser.uid)
        }
    }

    private fun navigateTo(destination: Class<*>) {
        if (!isNavigating) {
            isNavigating = true
            val intent = Intent(this, destination)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
