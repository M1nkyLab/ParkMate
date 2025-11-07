package com.example.parkmate.User.InstantBooking

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
import java.text.NumberFormat
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.Calendar

class User_Payment_InstantBooking : AppCompatActivity() {

    // ... (Your existing variable declarations are fine) ...
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
    private var selectedTime: String = "" // This is the display string "14:00 - 16:00"
    private var numberPlate: String = ""

    private lateinit var selectedDate: String
    private lateinit var selectedStartTime: String
    private var durationHours: Int = 0


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

    // In User_Payment_InstantBooking.kt (File 9)
    private fun getIntentData() {
        // Original data
        slotName = intent.getStringExtra("slotName") ?: "N/A"
        selectedTime = intent.getStringExtra("selectedTime") ?: "N/A" // e.g., "2 Hour"
        price = intent.getDoubleExtra("price", 0.0)

        // --- 🔴 FIX 1: Use the correct intent key "selectedPlate" ---
        numberPlate = intent.getStringExtra("selectedPlate") ?: "N/A" //

        // --- 🔴 FIX 2: Parse duration from the "selectedTime" string ---
        // This is needed for the next fix
        durationHours = selectedTime.split(" ")[0].toIntOrNull() ?: 0

        // --- 🔴 FIX 3: Remove Advance Booking data that is not sent ---
        // selectedDate = intent.getStringExtra("selectedDate") ?: "" // REMOVE THIS
        // selectedStartTime = intent.getStringExtra("selectedStartTime") ?: "" // REMOVE THIS

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
            saveBookingToFirestore()
        }, 2000)
    }

    // In User_Payment_InstantBooking.kt (File 9)
    private fun saveBookingToFirestore() {
        val bookingId = System.currentTimeMillis().toString()
        val userEmail = auth.currentUser?.email ?: "Unknown Email"

        // --- FIX 4: Calculate Start and End Timestamps based on NOW ---
        val calendar = Calendar.getInstance()
        val startTime = Timestamp(calendar.time) // Booking starts NOW

        // Add the duration (parsed in Fix 2)
        if (durationHours > 0) {
            calendar.add(Calendar.HOUR_OF_DAY, durationHours)
        }
        val endTime = Timestamp(calendar.time) // This is the calculated endTime

        val bookingData = hashMapOf(
            "bookingId" to bookingId,
            "userEmail" to userEmail,
            "slotName" to slotName,
            "selectedTime" to selectedTime,
            "vehicleNumber" to numberPlate, // This will now have the correct plate
            "price" to price,
            "status" to "Booked", // Or "Active"
            "gateAccess" to true, // Real-time booking should grant immediate access

            // --- FIX 5: Change bookingType to "Realtime" (more accurate) ---
            "bookingType" to "Instant Parking", // "Reserve" is confusing

            "durationHours" to durationHours.toLong(),
            "startTime" to startTime, // The ACTUAL start time (now)
            "endTime" to endTime      // The calculated end time
        )

        db.collection("bookings").document(bookingId)
            .set(bookingData)
            .addOnSuccessListener {

                // --- 🔴 FIX 6: CRITICAL - Update the parking slot to "Booked" ---
                db.collection("parking_slots").document(slotName)
                    .update("status", "Booked")
                    .addOnSuccessListener {
                        showSuccessDialog(bookingId) // Show success after both save
                    }
                    .addOnFailureListener { e ->
                        // The booking saved, but slot update failed.
                        Toast.makeText(this, "Booking saved, but failed to update slot: ${e.message}", Toast.LENGTH_LONG).show()
                        showSuccessDialog(bookingId) // Still proceed
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save booking: ${e.message}", Toast.LENGTH_SHORT).show()

            }
    }

    private fun showSuccessDialog(bookingId: String) {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Your booking has been saved to the system.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, User_CompletePayment_InstantBooking::class.java)
                // Pass the bookingId so the next screen can load it
                intent.putExtra("bookingId", bookingId)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}