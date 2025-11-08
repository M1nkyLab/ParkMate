package com.example.parkmate.User

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class User_Edit_Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUser: FirebaseUser? = null

    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var editCurrentPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var saveProfileButton: Button
    private lateinit var progressBar: ProgressBar

    private var currentUsername: String = ""
    private var currentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser

        // Initialize UI Elements
        editUsername = findViewById(R.id.editUsername)
        editEmail = findViewById(R.id.editEmail)
        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        editNewPassword = findViewById(R.id.editNewPassword)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        progressBar = findViewById(R.id.editProfileProgressBar)

        loadUserData()

        saveProfileButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    /**
     * Load current user data from Firestore and Auth
     */
    private fun loadUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentEmail = currentUser!!.email ?: ""
        editEmail.setText(currentEmail)

        db.collection("users").document(currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUsername = document.getString("username") ?: ""
                    editUsername.setText(currentUsername)
                } else {
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Main function to coordinate saving all changes
     */
    private fun saveProfileChanges() {
        val newUsername = editUsername.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val currentPass = editCurrentPassword.text.toString()
        val newPass = editNewPassword.text.toString()

        var aChangeRequiresAuth = false

        // Check if email or password is being changed
        if (newEmail != currentEmail) aChangeRequiresAuth = true
        if (newPass.isNotEmpty()) aChangeRequiresAuth = true

        // --- VALIDATIONS ---
        if (newUsername.isEmpty()) {
            editUsername.error = "Username is required"
            editUsername.requestFocus()
            return
        }
        if (newEmail.isEmpty()) {
            editEmail.error = "Email is required"
            editEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editEmail.error = "Please enter a valid email"
            editEmail.requestFocus()
            return
        }
        if (aChangeRequiresAuth && currentPass.isEmpty()) {
            editCurrentPassword.error = "Current password is required to change email or password"
            editCurrentPassword.requestFocus()
            return
        }
        if (newPass.isNotEmpty() && newPass.length < 6) {
            editNewPassword.error = "New password must be at least 6 characters"
            editNewPassword.requestFocus()
            return
        }

        // --- PROCESSING ---
        setLoading(true)

        if (aChangeRequiresAuth) {
            // Must re-authenticate first
            reauthenticateAndPerformUpdates(currentPass, newUsername, newEmail, newPass)
        } else {
            // Only username is being changed (or no changes)
            if (newUsername != currentUsername) {
                updateUsernameInFirestore(newUsername)
            } else {
                Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private fun reauthenticateAndPerformUpdates(currentPass: String, newUsername: String, newEmail: String, newPass: String) {
        val credential = EmailAuthProvider.getCredential(currentEmail, currentPass)

        currentUser?.reauthenticate(credential)
            ?.addOnSuccessListener {
                // Re-authentication successful! Now perform updates.
                var tasksCompleted = 0
                val totalTasks = 3 // Username, Email, Password

                // 1. Update Username (if changed)
                if (newUsername != currentUsername) {
                    updateUsernameInFirestore(newUsername) { success ->
                        if (success) currentUsername = newUsername
                        tasksCompleted++
                        checkIfAllTasksDone(tasksCompleted, totalTasks)
                    }
                } else {
                    tasksCompleted++
                }

                // 2. Update Email (if changed)
                if (newEmail != currentEmail) {
                    currentUser?.updateEmail(newEmail)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email updated", Toast.LENGTH_SHORT).show()
                            currentEmail = newEmail
                            // Also update email in Firestore
                            db.collection("users").document(currentUser!!.uid).update("email", newEmail)
                        } else {
                            Toast.makeText(this, "Failed to update email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                        tasksCompleted++
                        checkIfAllTasksDone(tasksCompleted, totalTasks)
                    }
                } else {
                    tasksCompleted++
                }

                // 3. Update Password (if new one provided)
                if (newPass.isNotEmpty()) {
                    currentUser?.updatePassword(newPass)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to update password: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                        tasksCompleted++
                        checkIfAllTasksDone(tasksCompleted, totalTasks)
                    }
                } else {
                    tasksCompleted++
                }
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "Re-authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }

    /**
     * Updates username in Firestore "users" collection
     */
    private fun updateUsernameInFirestore(newUsername: String, callback: ((Boolean) -> Unit)? = null) {
        db.collection("users").document(currentUser!!.uid)
            .update("username", newUsername)
            .addOnSuccessListener {
                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
                callback?.invoke(true)
                if (callback == null) setLoading(false) // Only stop loading if this is the only operation
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update username: ${e.message}", Toast.LENGTH_SHORT).show()
                callback?.invoke(false)
                if (callback == null) setLoading(false)
            }
    }

    /**
     * Helper function to track multiple async operations
     */
    private fun checkIfAllTasksDone(completed: Int, total: Int) {
        if (completed == total) {
            setLoading(false)
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish() // Close the activity and go back to profile
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            saveProfileButton.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            saveProfileButton.visibility = View.VISIBLE
        }
    }
}