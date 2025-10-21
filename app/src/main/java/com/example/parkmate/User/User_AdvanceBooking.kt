package com.example.parkmate.User

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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
    private val hourList = (1..12).map { it.toString() } // example 1-12 hours

    private lateinit var plateAdapter: ArrayAdapter<String>
    private lateinit var slotAdapter: ArrayAdapter<String>
    private lateinit var hourAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking)

        // Initialize views
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

        // Load data from Firebase
        loadUserPlates()
        loadParkingSlots()

        // Pick date
        selectDateBtn.setOnClickListener { pickDate() }

        // Pick time
        selectTimeBtn.setOnClickListener { pickTime() }

        // Update estimated end time when hours change
        hourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                calculateEndTime()
                calculatePrice()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Confirm booking
        confirmBookingBtn.setOnClickListener { saveBooking() }
    }

    private fun loadUserPlates() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("vehicles")
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
                Toast.makeText(this, "Error loading plates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadParkingSlots() {
        db.collection("parking_slots")
            .get()
            .addOnSuccessListener { documents ->
                slotList.clear()
                for (doc in documents) {
                    val slotName = doc.getString("name") ?: doc.id
                    slotList.add(slotName)
                }
                if (slotList.isEmpty()) slotList.add("No slots available")
                slotAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading slots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            selectedDateText.text = "${d}/${m + 1}/$y"
            calculateEndTime()
        }, year, month, day).show()
    }

    private fun pickTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val mFormatted = if (m < 10) "0$m" else "$m"
            selectedTimeText.text = "$h:$mFormatted"
            calculateEndTime()
        }, hour, minute, true).show()
    }

    private fun calculateEndTime() {
        val startTimeStr = selectedTimeText.text.toString()
        val durationStr = hourSpinner.selectedItem.toString()

        if (startTimeStr == "No time selected" || durationStr.isEmpty()) {
            estimatedEndTime.text = "Estimated End Time: -"
            return
        }

        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startDate = sdf.parse(startTimeStr) ?: return
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.HOUR_OF_DAY, durationStr.toInt())

            estimatedEndTime.text = "Estimated End Time: ${sdf.format(calendar.time)}"
        } catch (e: Exception) {
            estimatedEndTime.text = "Estimated End Time: -"
        }
    }

    private fun calculatePrice() {
        val duration = hourSpinner.selectedItem.toString().toIntOrNull() ?: 0
        val ratePerHour = 5.0 // example RM 5 per hour
        val totalPrice = duration * ratePerHour
        priceText.text = "Total Price: RM %.2f".format(totalPrice)
    }

    private fun saveBooking() {
        val userId = auth.currentUser?.uid ?: return
        val plate = plateSpinner.selectedItem.toString()
        val slot = slotSpinner.selectedItem.toString()
        val date = selectedDateText.text.toString()
        val time = selectedTimeText.text.toString()
        val duration = hourSpinner.selectedItem.toString().toIntOrNull() ?: 0
        val totalPrice = duration * 5.0

        if (date == "No date selected" || time == "No time selected") {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingData = hashMapOf(
            "plate" to plate,
            "slot" to slot,
            "date" to date,
            "startTime" to time,
            "durationHours" to duration,
            "endTime" to estimatedEndTime.text.toString().replace("Estimated End Time: ", ""),
            "totalPrice" to totalPrice,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving booking: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
