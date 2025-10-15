package com.example.parkmate.User

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.parkmate.R

class User_SelectSlot_RealtimeBooking : AppCompatActivity() {

    private lateinit var gridSlots: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_select_realtimebooking)

        gridSlots = findViewById(R.id.gridSlots)

        // Example slot list (slot name + availability)
        val parkingSlots = listOf(
            Pair("A1", true),
            Pair("A2", false),
            Pair("A3", true),
            Pair("A4", true),
            Pair("B1", false),
            Pair("B2", true),
            Pair("B3", true),
            Pair("B4", false)
        )

        val timeOptions = listOf("1 Hour", "2 Hours", "3 Hours", "4 Hours")
        val baseRate = 3.0 // RM3 per hour

        for ((slotName, isAvailable) in parkingSlots) {
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

            // Spinner for time selection
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(
                    this@User_SelectSlot_RealtimeBooking,
                    android.R.layout.simple_spinner_dropdown_item,
                    timeOptions
                )
                visibility = if (isAvailable) Spinner.VISIBLE else Spinner.GONE
            }

            // Price label
            val priceView = TextView(this).apply {
                text = "Price: RM${String.format("%.2f", baseRate)}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 16)
            }

            // Update price when time changes
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val hours = position + 1
                    val price = baseRate * hours
                    priceView.text = "Price: RM${String.format("%.2f", price)}"
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
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
}
