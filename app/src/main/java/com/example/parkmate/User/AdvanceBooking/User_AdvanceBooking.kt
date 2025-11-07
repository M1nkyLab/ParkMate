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
import com.google.firebase.Timestamp // Import Timestamp
import java.text.SimpleDateFormat
import java.util.*

class User_AdvanceBooking : AppCompatActivity() {

    private lateinit var plateSpinner: Spinner
    private lateinit var slotSpinner: Spinner
    private lateinit var hourSpinner: Spinner
    private lateinit var selectDateBtn: Button
    private lateinit var selectTimeBtn: Button
    private lateinit var findSlotsBtn: Button // NEW: Button to trigger search
    private lateinit var selectedDateText: TextView
    private lateinit var selectedTimeText: TextView
    private lateinit var estimatedEndTime: TextView
    private lateinit var priceText: TextView
    private lateinit var confirmBookingBtn: Button
    private lateinit var loadingSpinner: ProgressBar // NEW: Loading indicator

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val plateList = mutableListOf<String>()
    // --- NEW: We store all slots, not just available ones ---
    private val allSlotsList = mutableListOf<String>()
    private val availableSlotsList = mutableListOf<String>()
    private val hourList = mutableListOf<String>()

    private lateinit var plateAdapter: ArrayAdapter<String>
    private lateinit var slotAdapter: ArrayAdapter<String>
    private lateinit var hourAdapter: ArrayAdapter<String>

    private var hourlyRates: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking)

        // Initialize UI
        initializeViews()
        setupAdapters()

        // Load static data
        loadUserPlates()
        loadAllParkingSlots() // NEW: Load all slots once
        loadRatesFromFirestore()

        // Setup Listeners
        setupListeners()

        // Initial UI state
        updateUIState(isReady = false)
    }

    private fun initializeViews() {
        plateSpinner = findViewById(R.id.plateSpinner)
        slotSpinner = findViewById(R.id.slotSpinner)
        hourSpinner = findViewById(R.id.hourSpinner)
        selectDateBtn = findViewById(R.id.selectDateBtn)
        selectTimeBtn = findViewById(R.id.selectTimeBtn)

        // --- Make sure you have these IDs in your user_advancebooking.xml ---
        findSlotsBtn = findViewById(R.id.findSlotsBtn)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        // ------------------------------------------------------------------

        selectedDateText = findViewById(R.id.selectedDateText)
        selectedTimeText = findViewById(R.id.selectedTimeText)
        estimatedEndTime = findViewById(R.id.estimatedEndTime)
        priceText = findViewById(R.id.priceText)
        confirmBookingBtn = findViewById(R.id.confirmBookingBtn)
    }

    private fun setupAdapters() {
        plateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, plateList)
        plateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        plateSpinner.adapter = plateAdapter

        // --- NEW: Adapter now points to the availableSlotsList ---
        slotAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableSlotsList)
        slotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        slotSpinner.adapter = slotAdapter

        hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hourList)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        hourSpinner.adapter = hourAdapter
    }

    private fun setupListeners() {
        selectDateBtn.setOnClickListener { pickDate() }
        selectTimeBtn.setOnClickListener { pickTime() }
        findSlotsBtn.setOnClickListener { findAvailableSlots() } // NEW

        // This listener now just calculates price
        hourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                calculatePrice()
                updateUIState(isReady = false) // Require re-check if duration changes
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

    /** --- NEW: Load ALL parking slots ---
     * We get all slot names once, so we can filter them later.
     */
    private fun loadAllParkingSlots() {
        db.collection("parking_slots")
            .get()
            .addOnSuccessListener { documents ->
                allSlotsList.clear()
                for (doc in documents) {
                    val slotName = doc.getString("slotId") ?: doc.id
                    allSlotsList.add(slotName)
                }
                if (allSlotsList.isEmpty()) {
                    Toast.makeText(this, "No parking slots configured in system.", Toast.LENGTH_SHORT).show()
                }
                // Pre-fill list with a prompt
                availableSlotsList.add("Check availability")
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
                    if (hourList.isNotEmpty()) {
                        calculatePrice()
                    }
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
                updateUIState(isReady = false) // Require user to search again
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
            updateUIState(isReady = false) // Require user to search again
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }


    /** --- NEW: This is the core logic to check availability --- */
    private fun findAvailableSlots() {
        val date = selectedDateText.text.toString()
        val time = selectedTimeText.text.toString()
        val durationStr = hourSpinner.selectedItem?.toString()

        if (date.isEmpty() || time.isEmpty() || durationStr == null) {
            Toast.makeText(this, "Please select date, time, and duration.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingSpinner.visibility = View.VISIBLE
        updateUIState(isReady = false)
        availableSlotsList.clear()
        availableSlotsList.add("Searching...")
        slotAdapter.notifyDataSetChanged()

        // 1. Calculate the user's desired time range
        val (queryStartTime, queryEndTime) = try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val startDate = sdf.parse("$date $time") ?: return
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            val start = Timestamp(calendar.time)

            calendar.add(Calendar.HOUR_OF_DAY, durationStr.toInt())
            val end = Timestamp(calendar.time)

            Pair(start, end)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date/time format.", Toast.LENGTH_SHORT).show()
            loadingSpinner.visibility = View.GONE
            return
        }

        // 2. Find all bookings that CONFLICT with this time range
        // A booking conflicts if:
        // (booking.startTime < queryEndTime) AND (booking.endTime > queryStartTime)
        db.collection("bookings")
            .whereLessThan("startTime", queryEndTime) // Find bookings that start *before* our end
            .get()
            .addOnSuccessListener { snapshot ->

                val conflictingSlots = mutableSetOf<String>()
                for (doc in snapshot) {
                    val bookingEndTime = doc.getTimestamp("endTime")
                    // Now check the second half of the overlap:
                    // (booking.endTime > queryStartTime)
                    if (bookingEndTime != null && bookingEndTime.toDate().after(queryStartTime.toDate())) {
                        val slotName = doc.getString("slotName")
                        if (slotName != null) {
                            conflictingSlots.add(slotName)
                        }
                    }
                }

                // 3. Filter the 'allSlotsList' to get available slots
                availableSlotsList.clear()
                val available = allSlotsList.filter { it !in conflictingSlots }
                availableSlotsList.addAll(available)

                // 4. Update the UI
                slotAdapter.notifyDataSetChanged()
                loadingSpinner.visibility = View.GONE

                if (availableSlotsList.isEmpty()) {
                    Toast.makeText(this, "No slots available for this time.", Toast.LENGTH_SHORT).show()
                    availableSlotsList.add("No slots found")
                    slotAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Found ${availableSlotsList.size} available slots.", Toast.LENGTH_SHORT).show()
                    updateUIState(isReady = true) // Enable booking
                }
            }
            .addOnFailureListener { e ->
                loadingSpinner.visibility = View.GONE
                Toast.makeText(this, "Error finding slots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** Calculate total price */
    private fun calculatePrice() {
        val selectedHour = hourSpinner.selectedItem?.toString() ?: return
        val totalPrice = hourlyRates[selectedHour] ?: 0.0
        priceText.text = "Total Price: RM %.2f".format(totalPrice)
        calculateEndTime() // Also update end time when duration changes
    }

    /** --- NEW: Calculate estimated end time (handles next-day rollover) --- */
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

            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val endDateFormatted = sdfDateTime.format(calendar.time)

            // Check if it's a different day
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val originalDate = sdfDate.parse(selectedDateStr)
            val newDate = sdfDate.parse(endDateFormatted.split(" ")[0]) // Get just the date part

            if(originalDate == newDate) {
                estimatedEndTime.text = "Estimated End Time: ${sdfTime.format(calendar.time)}"
            } else {
                estimatedEndTime.text = "Estimated End Time: ${sdfTime.format(calendar.time)}"
            }

        } catch (e: Exception) {
            estimatedEndTime.text = "Estimated End Time: -"
        }
    }


    /** --- NEW: Enable/Disable buttons based on state --- */
    private fun updateUIState(isReady: Boolean) {
        slotSpinner.isEnabled = isReady
        confirmBookingBtn.isEnabled = isReady
        if (!isReady) {
            confirmBookingBtn.alpha = 0.5f // Gray out button
        } else {
            confirmBookingBtn.alpha = 1.0f
        }
    }

    /** --- FIXED: This function no longer saves to DB. It just goes to the summary. --- */
    private fun goToSummary() {
        val plate = plateSpinner.selectedItem.toString()
        val slot = slotSpinner.selectedItem?.toString() ?: "" // Handle null selection
        val date = selectedDateText.text.toString()
        val time = selectedTimeText.text.toString()
        val duration = hourSpinner.selectedItem?.toString()?.toIntOrNull() ?: 0
        val totalPrice = hourlyRates[duration.toString()] ?: 0.0
        val endTimeText = estimatedEndTime.text.toString().replace("Estimated End Time: ", "")

        // --- Validation ---
        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }
        if (plate == "No vehicles found") {
            Toast.makeText(this, "Please add a vehicle first", Toast.LENGTH_SHORT).show()
            return
        }
        if (slot.isEmpty() || slot == "No slots available" || slot == "No slots found" || slot == "Check availability" || slot == "Searching...") {
            Toast.makeText(this, "Please find and select an available slot", Toast.LENGTH_SHORT).show()
            return
        }

        // Navigate to Summary page
        val intent = Intent(this, User_Summary_AdvanceBooking::class.java)
        intent.putExtra("plate", plate)
        intent.putExtra("slot", slot)
        intent.putExtra("date", date)
        intent.putExtra("time", time)
        intent.putExtra("duration", duration)
        intent.putExtra("endTime", endTimeText) // e.g. "18:00"
        intent.putExtra("totalPrice", totalPrice)
        startActivity(intent)
    }
}