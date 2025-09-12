package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        auth = FirebaseAuth.getInstance()

        val emailInput: EditText = findViewById(R.id.registerEmailInput)
        val usernameInput: EditText = findViewById(R.id.registerUsernameInput)
        val plateInput: EditText = findViewById(R.id.registerPlateInput)
        val passwordInput: EditText = findViewById(R.id.registerPasswordInput)
        val confirmPasswordInput: EditText = findViewById(R.id.registerConfirmPasswordInput)
        val registerButton: Button = findViewById(R.id.registerButton)
        val loginRedirect: TextView = findViewById(R.id.loginRedirect)

        // 🔹 Redirect to Login
        loginRedirect.setOnClickListener {
            val intent = Intent(this, Login::class.java)
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
                            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()

                            // ✅ Pass username + plate to Home activity
                            val intent = Intent(this, Home::class.java)
                            intent.putExtra("username", username)
                            intent.putExtra("plate", plate)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}
