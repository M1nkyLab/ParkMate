package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class Admin_View_Report : AppCompatActivity() {

    private lateinit var totalUsers: TextView
    private lateinit var totalBookings: TextView
    private lateinit var totalRevenue: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_view_report)

        totalUsers = findViewById(R.id.totalUsers)
        totalBookings = findViewById(R.id.totalBookings)
        totalRevenue = findViewById(R.id.totalRevenue)

        loadReportData()
    }

    private fun loadReportData() {
        // 🔹 1. Get total users
        db.collection("users").get()
            .addOnSuccessListener { result ->
                totalUsers.text = result.size().toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }

        // 🔹 2. Get total bookings + revenue
        db.collection("bookings").get()
            .addOnSuccessListener { result ->
                totalBookings.text = result.size().toString()

                var total = 0.0
                for (doc in result) {
                    total += doc.getDouble("price") ?: 0.0
                }
                totalRevenue.text = "RM %.2f".format(total)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_SHORT).show()
            }
    }
}
