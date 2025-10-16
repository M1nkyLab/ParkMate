package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.parkmate.Auth.Auth_LoginRegister
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var logoutButton: Button
    private lateinit var addVehicleButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_profilefragment, container, false)

        // 🔹 Firebase setup
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 🔹 UI elements
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        logoutButton = view.findViewById(R.id.logoutButton)
        addVehicleButton = view.findViewById(R.id.AddnewVehicleButton)

        // 🔹 Load user info from Firestore
        loadUserInfo()

        // 🔹 Logout button click
        logoutButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(requireContext(), Auth_LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 🔹 Add New Vehicle button click
        addVehicleButton.setOnClickListener {
            val intent = Intent(requireContext(), User_VehicleActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    /**
     * Fetch username and email from Firestore and display them.
     */
    private fun loadUserInfo() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "User"
                        val email = document.getString("email") ?: currentUser.email ?: "No email"

                        profileName.text = username
                        profileEmail.text = email
                    } else {
                        profileName.text = "User"
                        profileEmail.text = currentUser.email ?: "No email"
                        Toast.makeText(requireContext(), "User data not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            profileName.text = "Guest"
            profileEmail.text = "Not logged in"
        }
    }
}
