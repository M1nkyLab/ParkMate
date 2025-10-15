package com.example.parkmate.User

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import java.util.*

class User_AdvanceBooking : AppCompatActivity() {
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking)

        val selectDateBtn = findViewById<Button>(R.id.selectDateBtn)
        val selectedDateText = findViewById<TextView>(R.id.selectedDateText)
        val confirmBookingBtn = findViewById<Button>(R.id.confirmBookingBtn)

        selectDateBtn.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "$d/${m + 1}/$y"
                selectedDateText.text = "Selected Date: $selectedDate"
            }, year, month, day)
            datePicker.show()

            confirmBookingBtn.setOnClickListener {
                // Navigate to Payment Page
                val intent = Intent(this, User_BookingSummary::class.java)
                startActivity(intent)
            }
        }
    }
}
