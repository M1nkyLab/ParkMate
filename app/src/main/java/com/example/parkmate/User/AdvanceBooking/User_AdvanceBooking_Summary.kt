package com.example.parkmate.User.AdvanceBooking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R

class User_AdvanceBooking_Summary : AppCompatActivity() {

    private lateinit var summarySlot: TextView
    private lateinit var summaryPlate: TextView
    private lateinit var summaryDate: TextView
    private lateinit var summaryStartTime: TextView
    private lateinit var summaryEndTime: TextView
    private lateinit var summaryDuration: TextView
    private lateinit var summaryPrice: TextView
    private lateinit var confirmPaymentBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking_summary)

        // Initialize views
        summarySlot = findViewById(R.id.summarySlot)
        summaryPlate = findViewById(R.id.summaryPlate)
        summaryDate = findViewById(R.id.summaryDate)
        summaryStartTime = findViewById(R.id.summaryStartTime)
        summaryEndTime = findViewById(R.id.summaryEndTime)
        summaryDuration = findViewById(R.id.summaryDuration)
        summaryPrice = findViewById(R.id.summaryPrice)
        confirmPaymentBtn = findViewById(R.id.confirmBookingBtn)

        // Retrieve booking details from intent
        val plate = intent.getStringExtra("plate") ?: "N/A"
        val slot = intent.getStringExtra("slot") ?: "N/A"
        val date = intent.getStringExtra("date") ?: "N/A"
        val startTime = intent.getStringExtra("time") ?: "N/A"
        val endTime = intent.getStringExtra("endTime") ?: "N/A"
        val duration = intent.getIntExtra("duration", 0)
        val totalPrice = intent.getDoubleExtra("totalPrice", 0.0)

        // Set summary values
        summarySlot.text = slot
        summaryPlate.text = plate
        summaryDate.text = date
        summaryStartTime.text = startTime
        summaryEndTime.text = endTime
        summaryDuration.text = "$duration hour(s)"
        summaryPrice.text = "RM %.2f".format(totalPrice)

        // Proceed to payment
        confirmPaymentBtn.setOnClickListener {
            val intent = Intent(this, User_Payment_AdvanceBooking::class.java)
            intent.putExtra("numberPlate", plate)           // match User_Payment
            intent.putExtra("slotName", slot)               // match User_Payment
            intent.putExtra("selectedTime", "$startTime - $endTime") // combine start/end
            intent.putExtra("price", totalPrice)           // match User_Payment
            startActivity(intent)
            finish()
        }
    }
}
