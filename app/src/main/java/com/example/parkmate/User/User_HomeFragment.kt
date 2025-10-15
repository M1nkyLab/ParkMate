package com.example.parkmate.User

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.Intent // Required for starting new activities
import androidx.fragment.app.Fragment
import com.example.parkmate.R
import com.google.android.material.card.MaterialCardView

/**
 * Fragment responsible for displaying the user's home dashboard.
 * It shows user details (username, plate) and provides access to booking options.
 */
class User_HomeFragment : Fragment() { // Renamed class to User_HomeFragment for standard convention

    // Companion object to easily create a new instance with arguments (best practice)
    companion object {
        private const val ARG_USERNAME = "username"
        private const val ARG_PLATE = "plate"

        // newInstance now correctly returns an instance of User_HomeFragment
        fun newInstance(username: String, plate: String): User_HomeFragment {
            val fragment = User_HomeFragment()
            val args = Bundle().apply {
                putString(ARG_USERNAME, username)
                putString(ARG_PLATE, plate)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.user_homefragment, container, false)

        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)
        val realtimeCard: MaterialCardView = view.findViewById(R.id.cardRealtimeBooking)
        val advanceCard: MaterialCardView = view.findViewById(R.id.cardAdvanceBooking)

        // Retrieve arguments passed from the hosting Activity (MainActivity)
        val username = arguments?.getString(ARG_USERNAME) ?: "User"
        val plate = arguments?.getString(ARG_PLATE) ?: "N/A"

        welcomeText.text = "Welcome, $username\nPlate: $plate "

        // Set up the click listener for the Real-time Booking Card
        realtimeCard.setOnClickListener {
            // ACTION: Start the User_RealtimeBooking Activity
            val intent = Intent(requireContext(), User_SelectSlot_RealtimeBooking::class.java)
            startActivity(intent)
        }

        // Set up the click listener for the Advance Booking Card
        advanceCard.setOnClickListener {
            // ACTION: Start the User_AdvanceBooking Activity
            val intent = Intent(requireContext(), User_AdvanceBooking::class.java)
            startActivity(intent)
        }

        return view
    }
}
