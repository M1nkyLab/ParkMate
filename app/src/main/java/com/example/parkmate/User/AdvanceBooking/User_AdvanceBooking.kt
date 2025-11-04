package com.example.parkmate.User.AdvanceBooking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class User_AdvanceBooking : AppCompatActivity() {

    private lateinit var plateSpinner: Spinner
    private lateinit var slotSpinner: Spinner
    private lateinit var hourSpinner: Spinner
    private lateinit var selectDateBtn: Button
    private lateinit var selectTimeBtn: Button
    private lateinit var selectedDateText: TextView
    private lateinit var selectedTimeText: TextView
    private lateinit var estimatedEndTime: TextView
    private lateinit var priceText: TextView
    private lateinit var confirmBookingBtn: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val plateList = mutableListOf<String>()
    private val slotList = mutableListOf<String>()
    private val hourList = mutableListOf<String>()

    private lateinit var plateAdapter: ArrayAdapter<String>
    private lateinit var slotAdapter: ArrayAdapter<String>
    private lateinit var hourAdapter: ArrayAdapter<String>

    private var hourlyRates: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking)

        // Initialize UI
        plateSpinner = findViewById(R.id.plateSpinner)
        slotSpinner = findViewById(R.id.slotSpinner)
        hourSpinner = findViewById(R.id.hourSpinner)
        selectDateBtn = findViewById(R.id.selectDateBtn)
        selectTimeBtn = findViewById(R.id.selectTimeBtn)
        selectedDateText = findViewById(R.id.selectedDateText)
        selectedTimeText = findViewById(R.id.selectedTimeText)
        estimatedEndTime = findViewById(R.id.estimatedEndTime)
        priceText = findViewById(R.id.priceText)
        confirmBookingBtn = findViewById(R.id.confirmBookingBtn)

        // Set up adapters
        plateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, plateList)
        plateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        plateSpinner.adapter = plateAdapter

        slotAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, slotList)
        slotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        slotSpinner.adapter = slotAdapter

        hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hourList)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        hourSpinner.adapter = hourAdapter

        // Load Firestore data
        loadUserPlates()
        loadParkingSlots()
        loadRatesFromFirestore()

        selectDateBtn.setOnClickListener { pickDate() }
        selectTimeBtn.setOnClickListener { pickTime() }

        hourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                calculateEndTime()
                calculatePrice()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- FIXED: This button now goes to the Summary Screen ---
        confirmBookingBtn.setOnClickListener { goToSummary() }
    }

    /** Load user's vehicle plates */
    private fun loadUserPlates() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("vehicles")
            .get()
            .addOnSuccessListener { documents ->
                plateList.clear()
                for (doc in documents) {
                    val plate = doc.getString("numberPlate")
                    if (!plate.isNullOrEmpty()) plateList.add(plate)
                }
                if (plateList.isEmpty()) plateList.add("No vehicles found")
                plateAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading plates: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    /** Load available parking slots */
    private fun loadParkingSlots() {
        db.collection("parking_slots")
            // --- NEW: Only show "Available" slots ---
            .whereEqualTo("status", "Available")
            .get()
            .addOnSuccessListener { documents ->
                slotList.clear()
                for (doc in documents) {
                    val slotName = doc.getString("slotId") ?: doc.id // Use slotId
                    slotList.add(slotName)
                }
                if (slotList.isEmpty()) slotList.add("No slots available")
                slotAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading slots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** Load hourly rates dynamically from Firestore */
    private fun loadRatesFromFirestore() {
        db.collection("rates").document("standard")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    hourlyRates =
                        document.data?.mapValues { it.value.toString().toDouble() } ?: emptyMap()
                    hourList.clear()
                    hourList.addAll(hourlyRates.keys.sortedBy { it.toInt() })
                    hourAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "No rate data found in Firestore!", Toast.LENGTH_LONG)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load rates!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedDateText.text = String.format("%02d/%02d/%d", d, m + 1, y)
                calculateEndTime()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickTime() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            selectedTimeText.text = String.format("%02d:%02d", h, m)
            calculateEndTime()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    /** Calculate estimated end time (handles next-day rollover) */
    private fun calculateEndTime() {
        val startTimeStr = selectedTimeText.text.toString()
        val durationStr = hourSpinner.selectedItem?.toString() ?: return
        val selectedDateStr = selectedDateText.text.toString()

        if (startTimeStr.isEmpty() || selectedDateStr.isEmpty()) {
            estimatedEndTime.text = "Estimated End Time: -"
            return
        }

        try {
            val sdfDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val startDate = sdfDateTime.parse("$selectedDateStr $startTimeStr") ?: return

            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.HOUR_OF_DAY, durationStr.toInt())

            // --- FIXED: Format end time to match start time ---
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val endDateFormatted = sdfDateTime.format(calendar.time)

            // Check if it's a different day
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val originalDate = sdfDate.parse(selectedDateStr)
            val newDate = sdfDate.parse(endDateFormatted)

            if(originalDate == newDate) {
                estimatedEndTime.text = "Estimated End Time: ${sdfTime.format(calendar.time)}"
            } else {
                estimatedEndTime.text = "Estimated End Time: ${sdfTime.format(calendar.time)} (next day)"
            }

        } catch (e: Exception) {
            estimatedEndTime.text = "Estimated End Time: -"
        }
    }

    /** Calculate total price */
    private fun calculatePrice() {
        val selectedHour = hourSpinner.selectedItem?.toString() ?: return
        val totalPrice = hourlyRates[selectedHour] ?: 0.0
        priceText.text = "Total Price: RM %.2f".format(totalPrice)
    }

    /** --- FIXED: This function no longer saves to DB. It just goes to the summary. --- */
    private fun goToSummary() {
        val plate = plateSpinner.selectedItem.toString()
        val slot = slotSpinner.selectedItem.toString()
        val date = selectedDateText.text.toString()
        val time = selectedTimeText.text.toString()
        val duration = hourSpinner.selectedItem?.toString()?.toIntOrNull() ?: 0
        val totalPrice = hourlyRates[duration.toString()] ?: 0.0
        val endTimeText = estimatedEndTime.text.toString().replace("Estimated End Time: ", "")

        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }
        if (plate == "No vehicles found") {
            Toast.makeText(this, "Please add a vehicle first", Toast.LENGTH_SHORT).show()
            return
        }
        if (slot == "No slots available") {
            Toast.makeText(this, "No slots available for booking", Toast.LENGTH_SHORT).show()
            return
        }

        // Navigate to Summary page
        val intent = Intent(this, User_AdvanceBooking_Summary::class.java)
        intent.putExtra("plate", plate)
        intent.putExtra("slot", slot)
        intent.putExtra("date", date)
        intent.putExtra("time", time)
        intent.putExtra("duration", duration)
        intent.putExtra("endTime", endTimeText)
        intent.putExtra("totalPrice", totalPrice)
        startActivity(intent)
    }
}
