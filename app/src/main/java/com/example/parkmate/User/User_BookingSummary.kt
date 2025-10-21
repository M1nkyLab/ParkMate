package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView // <-- Import TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import java.text.NumberFormat // <-- Import for currency formatting
import java.util.Locale // <-- Import for currency formatting

class User_BookingSummary : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_bookingsummary)

        // --- 1. Get data from the previous activity (User_SelectSlot_RealtimeBooking) ---
        val slotName = intent.getStringExtra("slotName")
        val selectedTime = intent.getStringExtra("selectedTime")
        val price = intent.getDoubleExtra("price", 0.0) // 0.0 is a default value if not found

        // --- 2. Find your TextViews from your XML layout ---
        // !! IMPORTANT: You MUST change these R.id names to match your user_bookingsummary.xml file !!
        val tvSlotName = findViewById<TextView>(R.id.summarySlot) // <--- GUESSING THIS ID
        val tvSelectedTime = findViewById<TextView>(R.id.summaryTime) // <--- GUESSING THIS ID
        val tvTotalPrice = findViewById<TextView>(R.id.summaryPrice) // <--- GUESSING THIS ID


        // --- 3. Display the data in the TextViews ---

        // This formats the price double (e.g., 6.0) into a currency string (e.g., "RM 6.00")
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        val formattedPrice = format.format(price)

        tvSlotName.text = "Slot: $slotName"
        tvSelectedTime.text = "Duration: $selectedTime"
        tvTotalPrice.text = "Total: $formattedPrice"


        // --- 4. Find the button and prepare to pass data to the NEXT activity ---
        val btnProceedPayment = findViewById<Button>(R.id.btnProceedPayment)

        btnProceedPayment.setOnClickListener {
            // Navigate to Payment Page
            val paymentIntent = Intent(this, User_Payment::class.java)

            // Pass the details along to the User_Payment activity
            paymentIntent.putExtra("slotName", slotName)
            paymentIntent.putExtra("selectedTime", selectedTime)
            paymentIntent.putExtra("price", price) // Pass the original double value

            startActivity(paymentIntent)
        }
    }
}