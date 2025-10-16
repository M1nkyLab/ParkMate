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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val plateInput: EditText = findViewById(R.id.registerPlateInput)
        val passwordInput: EditText = findViewById(R.id.registerPasswordInput)
        val confirmPasswordInput: EditText = findViewById(R.id.registerConfirmPasswordInput)
        val registerButton: Button = findViewById(R.id.registerButton)
        val loginRedirect: TextView = findViewById(R.id.loginRedirect)

        // 🔹 Redirect to Auth_Login
        loginRedirect.setOnClickListener {
            val intent = Intent(this, Auth_Login::class.java)
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val plate = plateInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || plate.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                            val currentUser = auth.currentUser
                            val uid = auth.currentUser?.uid


                            // create a map with user details and a default "user" role
                            if (uid != null) {
                                val userData = hashMapOf(
                                    "email" to email,
                                    "username" to username,
                                    "plate" to plate,
                                    "role" to "user"
                                )

                            // save user data to firestore
                            firestore.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,"Registration Successful",Toast.LENGTH_SHORT).show()


                                    // ✅ Pass username + plate to User_Home activity
                                    val intent = Intent(this, User_Home::class.java)
                                    intent.putExtra("username", username)
                                    intent.putExtra("plate", plate)
                                    startActivity(intent)
                                    finish()

                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this,  "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                        }
                            } else {
                                Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
        }
    }
}
