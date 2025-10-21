package com.example.parkmate.User

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import java.text.SimpleDateFormat
import java.util.*

class User_AdvanceBooking : AppCompatActivity() {

    private lateinit var selectDateBtn: Button
    private lateinit var selectedDateText: TextView
    private lateinit var slotSpinner: Spinner
    private lateinit var hourSpinner: Spinner
    private lateinit var confirmBookingBtn: Button
    private lateinit var priceText: TextView
    private lateinit var inputPlate: EditText

    private var selectedDate: String = ""
    private var selectedSlot: String = ""
    private var selectedDuration: Int = 0
    private var totalPrice: Double = 0.0

    private val pricePerHour = 2.5 // RM2.50/hour (example rate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_advancebooking)

        // Initialize UI elements
        selectDateBtn = findViewById(R.id.selectDateBtn)
        selectedDateText = findViewById(R.id.selectedDateText)
        slotSpinner = findViewById(R.id.slotSpinner)
        hourSpinner = findViewById(R.id.hourSpinner)
        confirmBookingBtn = findViewById(R.id.confirmBookingBtn)
        priceText = findViewById(R.id.priceText)
        

        // 1️⃣ Date Picker
        val calendar = Calendar.getInstance()
        selectDateBtn.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = sdf.format(calendar.time)
                    selectedDateText.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // 2️⃣ Slot Spinner (example slots)
        val slots = listOf("A01", "A02", "A03", "A04", "A05")
        val slotAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, slots)
        slotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        slotSpinner.adapter = slotAdapter
        slotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSlot = slots[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3️⃣ Duration Spinner (hours)
        val hours = listOf(1, 2, 3, 4, 5)
        val hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        hourSpinner.adapter = hourAdapter
        hourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedDuration = hours[position]
                calculatePrice()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 4️⃣ Confirm Booking Button -> Go to Summary Page
        confirmBookingBtn.setOnClickListener {
            val plateNumber = inputPlate.text.toString().trim()

            if (selectedDate.isEmpty() || plateNumber.isEmpty()) {
                Toast.makeText(this, "Please select a date and enter plate number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass the temporary booking data to Booking Summary Page
            val intent = Intent(this, User_BookingSummary::class.java).apply {
                putExtra("slotName", selectedSlot)
                putExtra("dateOfBooking", selectedDate)
                putExtra("duration", selectedDuration)
                putExtra("price", totalPrice)
                putExtra("plateNumber", plateNumber)
            }

            startActivity(intent)
        }
    }

    private fun calculatePrice() {
        totalPrice = selectedDuration * pricePerHour
        priceText.text = "Total Price: RM %.2f".format(totalPrice)
    }
}
