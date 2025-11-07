package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.parkmate.R
import com.example.parkmate.User.AdvanceBooking.User_AdvanceBooking
import com.example.parkmate.User.RealtimeBooking.User_SelectSlot_RealtimeBooking
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment responsible for displaying the user's home dashboard.
 * It shows a personalized welcome message and provides access to booking options.
 */
class User_HomeFragment : Fragment() {

    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeUserName: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_homefragment, container, false)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // UI references
        welcomeTitle = view.findViewById(R.id.welcomeTitle)
        welcomeUserName = view.findViewById(R.id.welcomeUserName)
        val realtimeCard: MaterialCardView = view.findViewById(R.id.cardRealtimeBooking)
        val advanceCard: MaterialCardView = view.findViewById(R.id.cardAdvanceBooking)

        // Load username from Firestore
        loadUserName()

        // Real-time booking card click
        realtimeCard.setOnClickListener {
            val intent = Intent(requireContext(), User_SelectSlot_RealtimeBooking::class.java)
            startActivity(intent)
        }

        // Advance booking card click
        advanceCard.setOnClickListener {
            val intent = Intent(requireContext(), User_AdvanceBooking::class.java)
            startActivity(intent)
        }

        return view
    }

    /**
     * Fetch the current user's name from Firestore using their UID.
     */
    private fun loadUserName() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "User"
                        welcomeUserName.text = username
                    } else {
                        welcomeUserName.text = "User"
                        Toast.makeText(requireContext(), "User data not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    welcomeUserName.text = "User"
                    Toast.makeText(requireContext(), "Failed to load user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            welcomeUserName.text = "Guest"
        }
    }
}
