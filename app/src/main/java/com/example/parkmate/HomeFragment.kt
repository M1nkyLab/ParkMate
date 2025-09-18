package com.example.parkmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.homefragment, container, false)

        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)

        val username = arguments?.getString("username") ?: "User"
        val plate = arguments?.getString("plate") ?: "N/A"

        welcomeText.text = "Welcome, $username\nPlate: $plate "

        return view
    }
}
