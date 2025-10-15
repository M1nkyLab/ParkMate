package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.parkmate.User.User_VehicleActivity
import com.example.parkmate.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class User_Home : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Load default fragment (User_HomeFragment)
        loadFragment(User_HomeFragment())

        // Handle navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(User_HomeFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(User_ProfileFragment())
                    true
                }
                R.id.nav_booking -> {
                    loadFragment(User_HistoryBookingFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

class User_AddNewVehicle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_addnewvehicle) // your layout

        // Navigate to activity vehicle when button is clicked
        val button = findViewById<Button>(R.id.saveVehicleButton)
        button.setOnClickListener {
            val intent = Intent(this, User_VehicleActivity::class.java)
            startActivity(intent)
        }

    }
}

class User_VehicleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_showvehicle)

        // Navigate to AddNewVehicleActivity when button is clicked
        val button = findViewById<Button>(R.id.addVehicleButton)
        button.setOnClickListener {
            val intent = Intent(this, User_AddNewVehicle::class.java)
            startActivity(intent)
        }
    }
}