package com.example.parkmate.Admin

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat
import java.util.*

class Admin_Manage_Bookings : AppCompatActivity() {

    private lateinit var recyclerBookings: RecyclerView
    private lateinit var inputSearchBooking: EditText
    private lateinit var btnAll: Button
    private lateinit var btnBooked: Button
    private lateinit var btnParked: Button
    private lateinit var btnExited: Button
    private lateinit var btnInstant: Button
    private lateinit var btnReserve: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var bookingAdapter: BookingAdapter

    private val allBookingsList = mutableListOf<Booking>()
    private val displayBookingsList = mutableListOf<Booking>()

    private var currentStatusFilter: Any = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_manage_bookings)

        db = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()

        recyclerBookings = findViewById(R.id.recyclerBookings)
        inputSearchBooking = findViewById(R.id.inputSearchBooking)
        btnAll = findViewById(R.id.btnAll)
        btnBooked = findViewById(R.id.btnBooked)
        btnParked = findViewById(R.id.btnParked)
        btnExited = findViewById(R.id.btnExited)
        btnInstant = findViewById(R.id.btnInstant)
        btnReserve = findViewById(R.id.btnReserve)

        bookingAdapter = BookingAdapter(displayBookingsList, functions)
        recyclerBookings.layoutManager = LinearLayoutManager(this)
        recyclerBookings.adapter = bookingAdapter

        loadBookings()
        setupFilterButtons()

        inputSearchBooking.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndSearchBookings()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadBookings() {
        db.collection("bookings")
            .orderBy("bookingId", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    allBookingsList.clear()
                    for (doc in snapshots) {
                        val booking = doc.toObject(Booking::class.java).copy(docId = doc.id)
                        allBookingsList.add(booking)
                    }
                    filterAndSearchBookings()
                }
            }
    }

    private fun setupFilterButtons() {
        updateButtonStyles(btnAll)

        btnAll.setOnClickListener {
            currentStatusFilter = "All"
            updateButtonStyles(btnAll)
            filterAndSearchBookings()
        }

        btnBooked.setOnClickListener {
            currentStatusFilter = "Booked"
            updateButtonStyles(btnBooked)
            filterAndSearchBookings()
        }

        btnParked.setOnClickListener {
            currentStatusFilter = "Parked"
            updateButtonStyles(btnParked)
            filterAndSearchBookings()
        }

        btnExited.setOnClickListener {
            currentStatusFilter = "Exited"
            updateButtonStyles(btnExited)
            filterAndSearchBookings()
        }

        btnInstant.setOnClickListener {
            currentStatusFilter = "Instant" // not "Instant Booking"
            updateButtonStyles(btnInstant)
            filterAndSearchBookings()
        }

        btnReserve.setOnClickListener {
            currentStatusFilter = "Reserve"
            updateButtonStyles(btnReserve)
            filterAndSearchBookings()
        }
    }

    private fun filterAndSearchBookings() {
        val searchQuery = inputSearchBooking.text.toString().trim()

        val filteredByStatus = when (val filter = currentStatusFilter) {
            is String -> {
                if (filter == "All") allBookingsList
                else allBookingsList.filter {
                    it.status.equals(filter, ignoreCase = true) ||
                            it.bookingType.equals(filter, ignoreCase = true)
                }
            }
            else -> allBookingsList
        }

        displayBookingsList.clear()
        if (searchQuery.isEmpty()) {
            displayBookingsList.addAll(filteredByStatus)
        } else {
            val filteredBySearch = filteredByStatus.filter {
                it.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                        it.slotName.contains(searchQuery, ignoreCase = true)
            }
            displayBookingsList.addAll(filteredBySearch)
        }

        bookingAdapter.notifyDataSetChanged()

        if (displayBookingsList.isEmpty() && allBookingsList.isNotEmpty()) {
            Toast.makeText(this, "No bookings match your filter.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtonStyles(activeButton: Button) {
        val allButtons = listOf(btnAll, btnBooked, btnParked, btnExited, btnInstant, btnReserve)
        for (button in allButtons) {
            if (button == activeButton) {
                button.setBackgroundColor(Color.parseColor("#FFFFFF"))
                button.setTextColor(Color.parseColor("#8E44AD"))
            } else {
                button.setBackgroundColor(Color.parseColor("#8E44AD"))
                button.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }
    }
}

// --- Data Class ---
data class Booking(
    val docId: String = "", // Added to store the document ID
    val bookingId: String = "",
    val vehicleNumber: String = "",
    val slotName: String = "",
    val status: String = "",
    val bookingType: String = "",
    val selectedTime: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationHours: Long = 0,
    val price: Double = 0.0,
    val userEmail: String = ""
)

// --- Adapter Class ---
class BookingAdapter(
    private val bookings: List<Booking>,
    private val functions: FirebaseFunctions
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val timeFormatter = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_item_bookings, parent, false)
        return BookingViewHolder(view, functions)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking, timeFormatter)
    }

    override fun getItemCount(): Int = bookings.size

    class BookingViewHolder(itemView: View, private val functions: FirebaseFunctions) : RecyclerView.ViewHolder(itemView) {
        private val textBookingId: TextView = itemView.findViewById(R.id.textBookingId)
        private val textPlateNumber: TextView = itemView.findViewById(R.id.textPlateNumber)
        private val textBookingType: TextView = itemView.findViewById(R.id.textBookingType)
        private val textSlot: TextView = itemView.findViewById(R.id.textSlot)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val btnSendNotification: Button = itemView.findViewById(R.id.btnSendNotification)

        fun bind(booking: Booking, formatter: SimpleDateFormat) {
            textBookingId.text = "ID: ${booking.bookingId}"
            textPlateNumber.text = "Plate: ${booking.vehicleNumber}"
            textSlot.text = "Slot: ${booking.slotName}"
            textStatus.text = "Status: ${booking.status}"
            textBookingType.text = "Type: ${booking.bookingType.ifEmpty { "Unknown" }}"

            // Booking type color
            when (booking.bookingType.lowercase(Locale.ROOT)) {
                "instant" -> textBookingType.setTextColor(Color.parseColor("#2ECC71"))
                "reserve" -> textBookingType.setTextColor(Color.parseColor("#9B59B6"))
                else -> textBookingType.setTextColor(Color.parseColor("#CCCCCC"))
            }

            if (booking.startTime != null && booking.endTime != null) {
                val startTimeStr = formatter.format(booking.startTime.toDate())
                val endTimeStr = formatter.format(booking.endTime.toDate())
                textTime.text = "$startTimeStr → $endTimeStr"
            } else {
                textTime.text = "Duration: ${booking.selectedTime}"
            }

            // Status color
            when (booking.status.lowercase(Locale.ROOT)) {
                "booked" -> textStatus.setTextColor(Color.parseColor("#3498DB"))
                "parked" -> textStatus.setTextColor(Color.parseColor("#2ECC71"))
                "exited" -> textStatus.setTextColor(Color.parseColor("#F1C40F"))
                "expired", "cancelled" -> textStatus.setTextColor(Color.parseColor("#E74C3C"))
                else -> textStatus.setTextColor(Color.parseColor("#FFFFFF"))
            }

            // Show button logic
            val now = Timestamp.now()
            if (booking.status.equals("Parked", ignoreCase = true) && booking.endTime != null && booking.endTime < now) {
                btnSendNotification.visibility = View.VISIBLE
            } else {
                btnSendNotification.visibility = View.GONE
            }

            // Button click listener
            btnSendNotification.setOnClickListener {
                sendNotification(booking.docId)
            }
        }

        private fun sendNotification(bookingId: String) {
            btnSendNotification.isEnabled = false // Prevent double clicks
            val data = hashMapOf("bookingId" to bookingId)

            functions
                .getHttpsCallable("sendManualOverstayNotification")
                .call(data)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(itemView.context, "Notification sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w("MANUAL_NOTIFICATION", "Failed to send notification", task.exception)
                        Toast.makeText(itemView.context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                    btnSendNotification.isEnabled = true // Re-enable button
                }
        }
    }
}
