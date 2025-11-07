package com.example.parkmate.User.AdvanceBooking

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
import java.text.SimpleDateFormat

class User_Payment_AdvanceBooking : AppCompatActivity() {

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

    // We need to store the raw data to calculate the timestamps
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

    private fun getIntentData() {
        // Original data
        slotName = intent.getStringExtra("slotName") ?: "N/A"
        selectedTime = intent.getStringExtra("selectedTime") ?: "N/A"
        numberPlate = intent.getStringExtra("numberPlate") ?: "N/A"
        price = intent.getDoubleExtra("price", 0.0)

        // Get the raw date/time data from the summary screen
        selectedDate = intent.getStringExtra("selectedDate") ?: ""
        selectedStartTime = intent.getStringExtra("selectedStartTime") ?: ""
        durationHours = intent.getIntExtra("durationHours", 0)


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

    private fun saveBookingToFirestore() {
        val bookingId = System.currentTimeMillis().toString()
        val userEmail = auth.currentUser?.email ?: "Unknown Email"

        // Calculate Start and End Timestamps
        var startTime: Timestamp? = null
        var endTime: Timestamp? = null

        if (selectedDate.isNotEmpty() && selectedStartTime.isNotEmpty()) {
            try {
                // Parse the date and time strings
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val startDate = sdf.parse("$selectedDate $selectedStartTime")
                if (startDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = startDate
                    startTime = Timestamp(calendar.time) // This is the startTime

                    // Add the duration
                    calendar.add(Calendar.HOUR_OF_DAY, durationHours)
                    endTime = Timestamp(calendar.time) // This is the endTime
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error parsing date: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val bookingData = hashMapOf(
            "bookingId" to bookingId,
            "userEmail" to userEmail,
            "slotName" to slotName,
            "selectedTime" to selectedTime, // This is the display string "14:00 - 16:00"
            "vehicleNumber" to numberPlate,
            "price" to price,
            "status" to "Booked",
            "gateAccess" to false,
            "bookingType" to "Reserve", //
            "durationHours" to durationHours.toLong(),
            "startTime" to startTime, // The SCHEDULED start time
            "endTime" to endTime      // The SCHEDULED end time
        )

        // --- CRITICAL FIX: Save to the root "bookings" collection ---
        db.collection("bookings").document(bookingId)
            .set(bookingData)
            .addOnSuccessListener {
                // --- CRITICAL FIX: DO NOT UPDATE THE PARKING SLOT STATUS ---
                // An advance booking should NOT make the slot "Booked" today.
                // The slot remains "Available" until the user scans at the gate.

                // Pass the unique bookingId to the next screen
                showSuccessDialog(bookingId)
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
                val intent = Intent(this, User_CompletePayment_AdvanceBooking::class.java)
                // Pass the bookingId so the next screen can load it
                intent.putExtra("bookingId", bookingId)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}