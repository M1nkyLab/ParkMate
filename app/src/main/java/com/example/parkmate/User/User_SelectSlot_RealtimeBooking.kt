package com.example.parkmate.User

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class User_SelectSlot_RealtimeBooking : AppCompatActivity() {

    private lateinit var gridSlots: GridLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_select_realtimebooking)

        gridSlots = findViewById(R.id.gridSlots)

        fetchParkingSlots()
    }

    /**
     * THIS IS THE UPDATED FUNCTION
     */
    private fun fetchParkingSlots() {
        db.collection("parking_slots")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "No parking slots found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (document in result) {
                    // 1. Read "slotId" instead of "name"
                    val slotId = document.getString("slotId") ?: document.id // Use document.id as fallback

                    // 2. Read "status" (String) and convert to a Boolean
                    val statusString = document.getString("status") ?: "Occupied"
                    val isAvailable = (statusString == "Available")

                    // 3. Read "baseRate". If it's missing in Firestore, it will use 3.0
                    // (I recommend you add a 'baseRate: 3.0' field to your documents)
                    val baseRate = document.getDouble("baseRate") ?: 3.0

                    // This function call now receives the correct data
                    addSlotCard(slotId, isAvailable, baseRate)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load parking slots: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addSlotCard(slotName: String, isAvailable: Boolean, baseRate: Double) {
        val timeOptions = listOf("1 Hour", "2 Hours", "3 Hours", "4 Hours")

        val card = CardView(this).apply {
            radius = 18f
            cardElevation = 6f
            useCompatPadding = true
            setCardBackgroundColor(Color.WHITE)
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
            setTextColor(Color.BLACK)
            setPadding(0, 8, 0, 8)
        }

        val statusView = TextView(this).apply {
            text = if (isAvailable) "Available" else "Occupied"
            textSize = 15f
            setTextColor(if (isAvailable) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            setPadding(0, 4, 0, 12)
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@User_SelectSlot_RealtimeBooking,
                android.R.layout.simple_spinner_dropdown_item,
                timeOptions
            )
            visibility = if (isAvailable) Spinner.VISIBLE else Spinner.GONE
        }

        val priceView = TextView(this).apply {
            text = "Price: RM${String.format("%.2f", baseRate)}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 16)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val hours = position + 1
                val price = baseRate * hours
                priceView.text = "Price: RM${String.format("%.2f", price)}"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val bookButton = Button(this).apply {
            text = if (isAvailable) "Book Now" else "Unavailable"
            textSize = 14f
            isEnabled = isAvailable
            setBackgroundColor(if (isAvailable) Color.parseColor("#800080") else Color.LTGRAY)
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 8)

            setOnClickListener {
                val selectedTime = spinner.selectedItem.toString()
                val hours = spinner.selectedItemPosition + 1
                val totalPrice = baseRate * hours

                val intent = Intent(this@User_SelectSlot_RealtimeBooking, User_BookingSummary::class.java)
                intent.putExtra("slotName", slotName)
                intent.putExtra("selectedTime", selectedTime)
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