package com.example.parkmate.Admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.Auth.Auth_LoginRegister
import com.example.parkmate.R
import com.example.parkmate.Admin.Admin_Manage_Bookings

class Admin_MainPage : AppCompatActivity() {

    private lateinit var adminWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_mainpage)

        // Initialize UI
        adminWelcome = findViewById(R.id.adminTitle)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val manageParking: Button = findViewById(R.id.manageparking)
        val manageUsers: Button = findViewById(R.id.manageusers)
        val viewReport: Button = findViewById(R.id.viewreport)
        val manageBookings: Button = findViewById(R.id.managebookings)



        // Set welcome message dynamically
        val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        val displayName = currentUserEmail?.substringBefore("@") ?: "Admin"
        adminWelcome.text = "Welcome, Admin $displayName"

        // Logout button
        logoutButton.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Auth_LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Navigate to Manage Parking Page
        manageParking.setOnClickListener {
            val intent = Intent(this, Admin_Manage_ParkingSlot::class.java)
            startActivity(intent)
        }

        // Navigate to Manage Users Page
        manageUsers.setOnClickListener {
            val intent = Intent(this, Admin_ManageUser::class.java)
            startActivity(intent)
        }

        // Navigate to View Report Page
        viewReport.setOnClickListener {
            val intent = Intent(this, Admin_View_Report::class.java)
            startActivity(intent)
        }

        // navigate to the manage booking page
        manageBookings.setOnClickListener {
            val intent = Intent(this, Admin_Manage_Bookings::class.java)
            startActivity(intent)
        }
    }
}
