package com.example.parkmate

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.enableEdgeToEdge
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_booking -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BookingFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
