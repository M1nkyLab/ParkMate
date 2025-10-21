package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import java.text.NumberFormat
import java.util.Locale

class User_BookingSummary : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_bookingsummary)

        // --- 1. Get data from the previous activity ---
        val slotName = intent.getStringExtra("slotName") ?: "Unknown Slot"
        val selectedPlate = intent.getStringExtra("selectedPlate") ?: "No Vehicle Selected"
        val selectedTime = intent.getStringExtra("selectedTime") ?: "No Duration Selected"
        val price = intent.getDoubleExtra("price", 0.0)

        // --- 2. Find TextViews from XML layout ---
        val tvSlotName = findViewById<TextView>(R.id.summarySlot)
        val tvSelectedPlate = findViewById<TextView>(R.id.summaryPlate)
        val tvSelectedTime = findViewById<TextView>(R.id.summaryTime)
        val tvTotalPrice = findViewById<TextView>(R.id.summaryPrice)

        // --- 3. Format price ---
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        val formattedPrice = format.format(price)

        // --- 4. Display data ---
        tvSlotName.text = "Slot: $slotName"
        tvSelectedPlate.text = "Plate: $selectedPlate"
        tvSelectedTime.text = "Duration: $selectedTime"
        tvTotalPrice.text = "Total: $formattedPrice"

        // --- 5. Proceed to payment ---
        val btnProceedPayment = findViewById<Button>(R.id.btnProceedPayment)
        btnProceedPayment.setOnClickListener {
            if (selectedPlate == "No Vehicle Selected") {
                Toast.makeText(this, "Please select a vehicle first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentIntent = Intent(this, User_Payment::class.java)
            paymentIntent.putExtra("slotName", slotName)
            paymentIntent.putExtra("selectedPlate", selectedPlate)
            paymentIntent.putExtra("selectedTime", selectedTime)
            paymentIntent.putExtra("price", price)
            startActivity(paymentIntent)
        }
    }
}
