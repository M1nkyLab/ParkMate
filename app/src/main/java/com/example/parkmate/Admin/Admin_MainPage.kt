package com.example.parkmate.Admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.parkmate.Auth.Auth_LoginRegister
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth

class Admin_MainPage : AppCompatActivity() {

    private lateinit var adminWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_mainpage)

        // Initialize UI elements
        adminWelcome = findViewById(R.id.adminTitle)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val scanQRButton: Button = findViewById(R.id.scanQRButton)

        val manageSlotsCard: CardView = findViewById(R.id.btnManageSlots)
        val viewBookingsCard: CardView = findViewById(R.id.btnViewBookings)
        val manageUsersCard: CardView = findViewById(R.id.btnViewUsers)
        val viewReportsCard: CardView = findViewById(R.id.btnReports)

        // Display admin name from Firebase
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        val displayName = currentUserEmail?.substringBefore("@") ?: "Admin"
        adminWelcome.text = "Welcome, Admin $displayName"

        // Logout functionality
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Auth_LoginRegister::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Manage Slots → Admin_Manage_ParkingSlot
        manageSlotsCard.setOnClickListener {
            val intent = Intent(this, Admin_Manage_ParkingSlot::class.java)
            startActivity(intent)
        }

        // View Bookings → Admin_Manage_Bookings
        viewBookingsCard.setOnClickListener {
            val intent = Intent(this, Admin_Manage_Bookings::class.java)
            startActivity(intent)
        }

        // Manage Users → Admin_Manage_User
        manageUsersCard.setOnClickListener {
            val intent = Intent(this, Admin_Manage_User::class.java)
            startActivity(intent)
        }

        // View Reports → Admin_View_Report
        viewReportsCard.setOnClickListener {
            val intent = Intent(this, Admin_View_Report::class.java)
            startActivity(intent)
        }

        // Scan QR → Admin_ScanQr
        scanQRButton.setOnClickListener {
            val intent = Intent(this, Admin_ScanQr::class.java)
            startActivity(intent)
        }
    }
}
