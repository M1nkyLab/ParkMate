package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.parkmate.User.User_VehicleActivity
import com.example.parkmate.Auth.Auth_LoginRegister
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth

class User_ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_profilefragment, container, false)

        auth = FirebaseAuth.getInstance()
        val logoutButton: Button = view.findViewById(R.id.logoutButton)

        // 🔹 Logout button click
        logoutButton.setOnClickListener {
            auth.signOut() // clear Firebase session

            // Go back to Auth_Login activity
            val intent = Intent(requireContext(), Auth_LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

            val AddnewVehicleButton: Button = view.findViewById(R.id.AddnewVehicleButton)
            AddnewVehicleButton.setOnClickListener {
                val intent = Intent(requireContext(), User_VehicleActivity::class.java)
                startActivity(intent)
            }
        return view
    }
}
