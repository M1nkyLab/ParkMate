package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.*

class Admin_View_Report : AppCompatActivity() {

    private lateinit var totalUsers: TextView
    private lateinit var totalBookings: TextView
    private lateinit var totalRevenue: TextView
    private lateinit var btnClearReports: Button // Added button
    private val db = FirebaseFirestore.getInstance()
    private lateinit var functions: FirebaseFunctions // Added functions instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_view_report)

        totalUsers = findViewById(R.id.totalUsers)
        totalBookings = findViewById(R.id.totalBookings)
        totalRevenue = findViewById(R.id.totalRevenue)
        btnClearReports = findViewById(R.id.btnClearReports) // Find button
        functions = FirebaseFunctions.getInstance() // Initialize functions

        loadReportData()

        // Set listener for the new button
        btnClearReports.setOnClickListener {
            // Show a confirmation dialog before deleting
            AlertDialog.Builder(this)
                .setTitle("Confirm Action")
                .setMessage("Are you sure you want to permanently clear all reports older than 30 days? This action cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    clearOldReports()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadReportData() {
        // This function remains the same
        db.collection("users").get()
            .addOnSuccessListener { result ->
                totalUsers.text = result.size().toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }

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

    // New function to call the Cloud Function
    private fun clearOldReports() {
        functions
            .getHttpsCallable("clearOldReports")
            .call()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result?.data as? Map<*, *>
                    val message = result?.get("message")?.toString() ?: "Operation completed."
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    // Refresh the report data after clearing
                    loadReportData()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getStartOfMonth(year: Int, month: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfMonth(year: Int, month: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
