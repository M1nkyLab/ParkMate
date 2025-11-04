package com.example.parkmate.User.RealtimeBooking

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// We REMOVE Timestamp and Calendar imports. They are no longer needed here.
import java.text.NumberFormat
import java.util.Locale

class User_Payment_RealtimeBooking : AppCompatActivity() {

    private lateinit var tvPaymentSlot: TextView
    private lateinit var tvPaymentDuration: TextView
    private lateinit var btnPayNow: Button
    private lateinit var paymentOptions: RadioGroup
    private lateinit var cardDetailsLayout: LinearLayout
    private lateinit var otherPaymentMethodLayout: TextView
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var etCvv: EditText
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var price: Double = 0.0
    private var slotName: String = ""
    private var selectedTime: String = "" // This will be "2 Hour", "3 Hour", etc.
    private var vehicleNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_payment)

        initializeViews()
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        getIntentData()
        setupListeners()
    }

    private fun initializeViews() {
        tvPaymentSlot = findViewById(R.id.tvPaymentSlot)
        tvPaymentDuration = findViewById(R.id.tvPaymentDuration)
        btnPayNow = findViewById(R.id.btnPayNow)
        paymentOptions = findViewById(R.id.paymentOptions)
        cardDetailsLayout = findViewById(R.id.cardDetailsLayout)
        otherPaymentMethodLayout = findViewById(R.id.otherPaymentMethodLayout)
        etCardNumber = findViewById(R.id.etCardNumber)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        etCvv = findViewById(R.id.etCvv)
    }

    private fun getIntentData() {
        slotName = intent.getStringExtra("slotName") ?: "N/A"
        selectedTime = intent.getStringExtra("selectedTime") ?: "N/A"
        vehicleNumber = intent.getStringExtra("selectedPlate") ?: "N/A"
        price = intent.getDoubleExtra("price", 0.0)

        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        val formattedPrice = format.format(price)

        tvPaymentSlot.text = "Parking Slot: $slotName"
        tvPaymentDuration.text = "Duration: $selectedTime"
        btnPayNow.text = "Pay $formattedPrice"
    }

    private fun setupListeners() {
        paymentOptions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.option_card -> {
                    cardDetailsLayout.visibility = View.VISIBLE
                    otherPaymentMethodLayout.visibility = View.GONE
                }
                R.id.option_tng, R.id.option_fpx -> {
                    cardDetailsLayout.visibility = View.GONE
                    otherPaymentMethodLayout.visibility = View.VISIBLE
                }
            }
        }

        btnPayNow.setOnClickListener {
            handlePayment()
        }
    }

    private fun handlePayment() {
        val selectedOption = paymentOptions.checkedRadioButtonId
        if (selectedOption == -1) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedOption == R.id.option_card && !isCardInfoValid()) {
            return
        }

        simulatePaymentProcess()
    }

    private fun isCardInfoValid(): Boolean {
        if (etCardNumber.text.length < 16) {
            etCardNumber.error = "Enter a valid 16-digit card number"
            return false
        }
        if (etExpiryDate.text.length < 5) {
            etExpiryDate.error = "Enter expiry date (MM/YY)"
            return false
        }
        if (etCvv.text.length < 3) {
            etCvv.error = "Enter 3-digit CVV"
            return false
        }
        return true
    }

    private fun simulatePaymentProcess() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Processing Payment...")
            setCancelable(false)
            show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            progressDialog.dismiss()
            showSuccessDialog()
        }, 2000)
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Your payment has been processed successfully.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()

                val bookingId = System.currentTimeMillis().toString()
                val userEmail = auth.currentUser?.email ?: "Unknown Email"

                // --- START OF MODIFIED LOGIC ---
                // We only save the *number* of hours.
                // We parse the "2 Hour" string to get the number 2
                val durationHours = selectedTime.split(" ")[0].toLongOrNull() ?: 0
                // --- END OF MODIFIED LOGIC ---

                val bookingData = hashMapOf(
                    "bookingId" to bookingId,
                    "slotName" to slotName,
                    "selectedTime" to selectedTime, // This is just the "2 Hour" string for display
                    "price" to price,
                    "status" to "Booked", // Status is "Booked", NOT "Active"
                    "gateAccess" to false,
                    "userEmail" to userEmail,
                    "vehicleNumber" to vehicleNumber,

                    // --- SAVE DURATION FOR THE ADMIN'S SCANNER ---
                    "durationHours" to durationHours, // We save the number (e.g., 2)

                    "bookingType" to "Realtime"
                )

                db.collection("bookings").document(bookingId).set(bookingData)
                    .addOnSuccessListener {
                        // Update slot status to "Booked"
                        db.collection("parking_slots").document(slotName)
                            .update("status", "Booked")

                        // Go to complete page (for QR code)
                        val intent = Intent(this, User_CompletePayment_RealtimeBooking::class.java)
                        intent.putExtra("bookingId", bookingId)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save booking: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setCancelable(false)
            .show()
    }
}