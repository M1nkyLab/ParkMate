package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profilefragment, container, false)

        auth = FirebaseAuth.getInstance()
        val logoutButton: Button = view.findViewById(R.id.logoutButton)

        // 🔹 Logout button click
        logoutButton.setOnClickListener {
            auth.signOut() // clear Firebase session

            // Go back to Login activity
            val intent = Intent(requireContext(), LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
