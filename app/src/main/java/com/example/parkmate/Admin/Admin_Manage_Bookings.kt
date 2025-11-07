package com.example.parkmate.Admin

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.text.SimpleDateFormat
import java.util.*

class Admin_Manage_Bookings : AppCompatActivity() {

    private lateinit var recyclerBookings: RecyclerView
    private lateinit var inputSearchBooking: EditText
    private lateinit var btnAll: Button
    private lateinit var btnActive: Button
    private lateinit var btnCompleted: Button
    private lateinit var btnInstant: Button
    private lateinit var btnReserve: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var bookingAdapter: BookingAdapter

    private val allBookingsList = mutableListOf<Booking>()
    private val displayBookingsList = mutableListOf<Booking>()

    private var currentStatusFilter: Any = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_manage_bookings)

        db = FirebaseFirestore.getInstance()

        recyclerBookings = findViewById(R.id.recyclerBookings)
        inputSearchBooking = findViewById(R.id.inputSearchBooking)
        btnAll = findViewById(R.id.btnAll)
        btnActive = findViewById(R.id.btnActive)
        btnCompleted = findViewById(R.id.btnCompleted)
        btnInstant = findViewById(R.id.btnInstant)
        btnReserve = findViewById(R.id.btnReserve)

        bookingAdapter = BookingAdapter(displayBookingsList)
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
                        val booking = doc.toObject(Booking::class.java)
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
        btnActive.setOnClickListener {
            currentStatusFilter = "Active"
            updateButtonStyles(btnActive)
            filterAndSearchBookings()
        }
        btnCompleted.setOnClickListener {
            currentStatusFilter = "Completed"
            updateButtonStyles(btnCompleted)
            filterAndSearchBookings()
        }
        btnInstant.setOnClickListener {
            currentStatusFilter = "Instant Booking"
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
        val allButtons = listOf(btnAll, btnActive, btnCompleted, btnInstant, btnReserve)
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
    private val bookings: List<Booking>
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_item_bookings, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking, dateFormatter)
    }

    override fun getItemCount(): Int = bookings.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textBookingId: TextView = itemView.findViewById(R.id.textBookingId)
        private val textPlateNumber: TextView = itemView.findViewById(R.id.textPlateNumber)
        private val textBookingType: TextView = itemView.findViewById(R.id.textBookingType)
        private val textSlot: TextView = itemView.findViewById(R.id.textSlot)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)

        fun bind(booking: Booking, formatter: SimpleDateFormat) {
            textBookingId.text = "ID: ${booking.bookingId}"
            textPlateNumber.text = "Plate: ${booking.vehicleNumber}"
            textSlot.text = "Slot: ${booking.slotName}"
            textStatus.text = "Status: ${booking.status}"
            textBookingType.text = "Type: ${booking.bookingType.ifEmpty { "Unknown" }}"

            // ✅ Dynamic color for booking type
            when (booking.bookingType.lowercase(Locale.ROOT)) {
                "instant" -> textBookingType.setTextColor(Color.parseColor("#2ECC71")) // Green
                "reserve" -> textBookingType.setTextColor(Color.parseColor("#9B59B6")) // Purple
                else -> textBookingType.setTextColor(Color.parseColor("#CCCCCC"))
            }

            // ✅ Time display based on type
            if (booking.bookingType.equals("Reserve", ignoreCase = true)) {
                val startTime = booking.startTime?.toDate()?.let { formatter.format(it) } ?: "N/A"
                val endTime = booking.endTime?.toDate()?.let { formatter.format(it) } ?: "N/A"
                textTime.text = "$startTime  →  $endTime"
            } else {
                if (booking.startTime != null) {
                    val startTime = formatter.format(booking.startTime!!.toDate())
                    textTime.text = "Started: $startTime (${booking.selectedTime})"
                } else {
                    textTime.text = "Duration: ${booking.selectedTime} (Not started)"
                }
            }

            // ✅ Status color
            when (booking.status.lowercase(Locale.ROOT)) {
                "active" -> textStatus.setTextColor(Color.parseColor("#2ECC71"))
                "booked" -> textStatus.setTextColor(Color.parseColor("#3498DB"))
                "completed" -> textStatus.setTextColor(Color.parseColor("#95A5A6"))
                "expired", "cancelled" -> textStatus.setTextColor(Color.parseColor("#E74C3C"))
                else -> textStatus.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }
    }
}
