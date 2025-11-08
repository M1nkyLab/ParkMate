package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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

        // Load user and monthly booking data
        loadReportData()
    }

    private fun loadReportData() {
        // 1️⃣ Total registered users
        db.collection("users").get()
            .addOnSuccessListener { result ->
                totalUsers.text = result.size().toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }

        // 2️⃣ Monthly bookings and revenue
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        val startOfMonth = getStartOfMonth(year, month)
        val endOfMonth = getEndOfMonth(year, month)

        db.collection("bookings")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThanOrEqualTo("timestamp", endOfMonth)
            .get()
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

    // Helper function: Start of the month
    private fun getStartOfMonth(year: Int, month: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    // Helper function: End of the month
    private fun getEndOfMonth(year: Int, month: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
