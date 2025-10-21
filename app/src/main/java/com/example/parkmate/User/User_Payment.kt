package com.example.parkmate.User

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import java.text.NumberFormat
import java.util.Locale

class User_Payment : AppCompatActivity() {

    private lateinit var tvPaymentSlot: TextView
    private lateinit var tvPaymentDuration: TextView
    private lateinit var btnPayNow: Button
    private lateinit var paymentOptions: RadioGroup
    private lateinit var cardDetailsLayout: LinearLayout
    private lateinit var otherPaymentMethodLayout: TextView
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var etCvv: EditText

    private var price: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_payment)

        // Initialize views
        initializeViews()

        // Get data from BookingSummary and display it
        getIntentData()

        // Set up listeners
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
        val slotName = intent.getStringExtra("slotName") ?: "N/A"
        val selectedTime = intent.getStringExtra("selectedTime") ?: "N/A"
        price = intent.getDoubleExtra("price", 0.0)

        // Format currency
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        val formattedPrice = format.format(price)

        // Set text
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

        if (selectedOption == R.id.option_card) {
            if (!isCardInfoValid()) {
                return // Exit if validation fails
            }
        }

        // All good, simulate payment process
        simulatePaymentProcess()
    }

    private fun isCardInfoValid(): Boolean {
        if (etCardNumber.text.length < 16) {
            etCardNumber.error = "Enter a valid 16-digit card number"
            return false
        }
        if (etExpiryDate.text.length < 5) { // MM/YY format
            etExpiryDate.error = "Enter a valid expiry date (MM/YY)"
            return false
        }
        if (etCvv.text.length < 3) {
            etCvv.error = "Enter a valid 3-digit CVV"
            return false
        }
        return true
    }

    private fun simulatePaymentProcess() {
        // Show a "Processing..." dialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Processing Payment...")
            setCancelable(false)
            show()
        }

        // Simulate a network delay of 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            progressDialog.dismiss()
            showSuccessDialog()
        }, 2000)
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Your parking slot has been booked. A receipt has been sent to your email.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // TODO: Navigate to a receipt/home page
                val intent = Intent(this, User_CompletePayment::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // For now, just close the payment activity
            }
            .setCancelable(false)
            .show()
    }
}
