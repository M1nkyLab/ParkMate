package com.example.parkmate.User.InstantBooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_SelectSlot_InstantBooking : AppCompatActivity() {

    private lateinit var gridSlots: GridLayout
    private lateinit var plateSpinner: Spinner
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val plateList = mutableListOf<String>()
    private lateinit var plateAdapter: ArrayAdapter<String>

    private var hourlyRates: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_select_instantbooking)

        plateSpinner = findViewById(R.id.plateSpinner)
        gridSlots = findViewById(R.id.gridSlots)

        // --- MODIFIED ---
        // We will create a custom adapter for the plate spinner to make it dark-themed
        plateAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            plateList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE) // Set text color to white
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setBackgroundColor(Color.parseColor("#1A1A1A")) // Dark background
                view.setTextColor(Color.WHITE) // White text
                return view
            }
        }
        plateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        plateSpinner.adapter = plateAdapter
        // --- END MODIFICATION ---

        loadUserPlates()
        loadRates()
    }

    // 🔹 Load user's registered vehicles
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
                Toast.makeText(this, "Failed to load plates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Load hourly rates from Firestore
    private fun loadRates() {
        db.collection("rates").document("standard")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Convert rates to Double map
                    hourlyRates = document.data?.mapValues { it.value.toString().toDouble() } ?: emptyMap()
                    if (hourlyRates.isEmpty()) {
                        Toast.makeText(this, "No rates found in Firestore", Toast.LENGTH_LONG).show()
                    } else {
                        fetchParkingSlots()
                    }
                } else {
                    Toast.makeText(this, "Rates not found in Firestore", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load rates", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Fetch parking slots from Firestore
    private fun fetchParkingSlots() {
        db.collection("parking_slots").get()
            .addOnSuccessListener { result ->
                gridSlots.removeAllViews()
                if (result.isEmpty) {
                    Toast.makeText(this, "No parking slots found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (document in result) {
                    val slotId = document.getString("slotId") ?: document.id
                    val statusString = document.getString("status") ?: "Occupied"
                    val isAvailable = (statusString == "Available")
                    addSlotCard(slotId, isAvailable)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load parking slots", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Create each slot card dynamically
    private fun addSlotCard(slotName: String, isAvailable: Boolean) {
        // Build time options dynamically from Firestore rates
        val sortedRates = hourlyRates.toSortedMap(compareBy { it.toInt() })
        val timeOptions = sortedRates.keys.map { "$it Hour" }

        val card = CardView(this).apply {
            radius = 18f
            cardElevation = 6f
            // --- MODIFIED ---
            // Set card background to your app's dark card color
            setCardBackgroundColor(Color.parseColor("#1A1A1A"))
            // --- END MODIFICATION ---
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(12, 12, 12, 12)
            }
        }

        val innerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val nameView = TextView(this).apply {
            text = "Slot: $slotName"
            textSize = 18f
            // --- MODIFIED ---
            setTextColor(Color.WHITE)
            // --- END MODIFICATION ---
            setPadding(0, 8, 0, 8)
        }

        val statusView = TextView(this).apply {
            text = if (isAvailable) "Available" else "Occupied"
            textSize = 15f
            setTextColor(if (isAvailable) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            setPadding(0, 4, 0, 12)
        }

        val spinner = Spinner(this).apply {
            // --- MODIFIED ---
            // Create a custom adapter to style the spinner for the dark theme
            val spinnerAdapter = object : ArrayAdapter<String>(
                this@User_SelectSlot_InstantBooking,
                android.R.layout.simple_spinner_item, // Layout for the selected item view
                timeOptions
            ) {
                // This styles the "closed" spinner view
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTextColor(Color.WHITE) // Set text color to white
                    view.gravity = Gravity.CENTER // Center align
                    return view
                }

                // This styles the dropdown items
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setBackgroundColor(Color.parseColor("#1A1A1A")) // Dark background for dropdown
                    view.setTextColor(Color.WHITE) // White text for dropdown items
                    view.setPadding(24, 24, 24, 24) // Add some padding
                    return view
                }
            }
            // Use the layout for the dropdown (this is used by getDropDownView)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = spinnerAdapter
            // --- END MODIFICATION ---
            visibility = if (isAvailable) Spinner.VISIBLE else Spinner.GONE
        }

        val priceView = TextView(this).apply {
            val firstRate = sortedRates.values.firstOrNull() ?: 0.0
            text = "Price: RM${String.format("%.2f", firstRate)}"
            textSize = 14f
            // --- MODIFIED ---
            setTextColor(Color.WHITE)
            // --- END MODIFICATION ---
            setPadding(0, 8, 0, 16)
        }

        // Update price based on spinner selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedHourKey = sortedRates.keys.elementAt(position)
                val selectedRate = sortedRates[selectedHourKey] ?: 0.0
                priceView.text = "Price: RM${String.format("%.2f", selectedRate)}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val bookButton = Button(this).apply {
            text = if (isAvailable) "Book Now" else "Unavailable"
            textSize = 14f
            isEnabled = isAvailable

            // --- MODIFIED ---
            if (isAvailable) {
                // Use the rounded purple button drawable you created
                background = ContextCompat.getDrawable(this@User_SelectSlot_InstantBooking, R.drawable.rounded_button_card)
            } else {
                setBackgroundColor(Color.LTGRAY)
            }
            setTextColor(Color.WHITE)
            // --- END MODIFICATION ---

            setPadding(8, 8, 8, 8)

            setOnClickListener {
                val selectedPlate = plateSpinner.selectedItem.toString()
                if (selectedPlate == "No vehicles found") {
                    Toast.makeText(this@User_SelectSlot_InstantBooking, "Please add a vehicle first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedIndex = spinner.selectedItemPosition
                val selectedHourKey = sortedRates.keys.elementAt(selectedIndex)
                val totalPrice = sortedRates[selectedHourKey] ?: 0.0

                val intent = Intent(this@User_SelectSlot_InstantBooking, User_Summary_InstantBooking::class.java)
                intent.putExtra("slotName", slotName)
                intent.putExtra("selectedPlate", selectedPlate)
                intent.putExtra("selectedTime", "$selectedHourKey Hour")
                intent.putExtra("price", totalPrice)
                startActivity(intent)
            }
        }

        innerLayout.addView(nameView)
        innerLayout.addView(statusView)
        innerLayout.addView(spinner)
        innerLayout.addView(priceView)
        innerLayout.addView(bookButton)

        card.addView(innerLayout)
        gridSlots.addView(card)
    }
}
