package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class Admin_Edit_User : AppCompatActivity() {

    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var radioGroupRole: RadioGroup
    private lateinit var radioUser: RadioButton
    private lateinit var radioAdmin: RadioButton
    private lateinit var btnSaveChanges: Button

    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_edit_user)

        // Get the User ID passed from the adapter
        userId = intent.getStringExtra("USER_ID")
        if (userId == null) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        editUsername = findViewById(R.id.editUsername)
        editEmail = findViewById(R.id.editEmail)
        radioGroupRole = findViewById(R.id.radioGroupRole)
        radioUser = findViewById(R.id.radioUser)
        radioAdmin = findViewById(R.id.radioAdmin)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Load the user's current data
        loadUserData()

        // Set listener for the save button
        btnSaveChanges.setOnClickListener {
            saveUserData()
        }
    }

    /**
     * Fetches the user's data from Firestore and populates the fields.
     */
    private fun loadUserData() {
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            editUsername.setText(user.username)
                            editEmail.setText(user.email)
                            if (user.role.equals("admin", ignoreCase = true)) {
                                radioAdmin.isChecked = true
                            } else {
                                radioUser.isChecked = true
                            }
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading user: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    /**
     * Saves the updated user data back to Firestore.
     */
    private fun saveUserData() {
        val newUsername = editUsername.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val newRole = if (radioAdmin.isChecked) "admin" else "user"

        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Username and Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = mapOf(
            "username" to newUsername,
            "email" to newEmail,
            "role" to newRole
        )

        userId?.let {
            db.collection("users").document(it).update(userUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show()
                    finish() // Close this activity and go back to the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}