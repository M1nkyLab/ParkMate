package com.example.parkmate.Admin

import android.content.Context
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
import com.google.firebase.Timestamp // Import Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin_Manage_Bookings Activity
 * This screen allows the admin to view, search, and filter all user bookings.
 * It uses a snapshot listener for real-time updates.
 */
class Admin_Manage_Bookings : AppCompatActivity() {

    private lateinit var recyclerBookings: RecyclerView
    private lateinit var inputSearchBooking: EditText
    private lateinit var btnAll: Button
    private lateinit var btnActive: Button
    private lateinit var btnCompleted: Button
    private lateinit var btnCancelled: Button // Will filter for "Expired" and "Cancelled"

    private lateinit var db: FirebaseFirestore
    private lateinit var bookingAdapter: BookingAdapter

    // This list holds all bookings from Firestore
    private val allBookingsList = mutableListOf<Booking>()
    // This list holds only the bookings we want to display (after filtering)
    private val displayBookingsList = mutableListOf<Booking>()

    // This will hold the currently selected filter, defaulting to "All"
    private var currentStatusFilter: Any = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_manage_bookings)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Find all UI components
        recyclerBookings = findViewById(R.id.recyclerBookings)
        inputSearchBooking = findViewById(R.id.inputSearchBooking)
        btnAll = findViewById(R.id.btnAll)
        btnActive = findViewById(R.id.btnActive)
        btnCompleted = findViewById(R.id.btnCompleted)
        btnCancelled = findViewById(R.id.btnCancelled)

        // Setup RecyclerView
        bookingAdapter = BookingAdapter(displayBookingsList)
        recyclerBookings.layoutManager = LinearLayoutManager(this)
        recyclerBookings.adapter = bookingAdapter

        // Load data from Firestore
        loadBookings()

        // Setup filter button listeners
        setupFilterButtons()

        // Setup search bar listener
        inputSearchBooking.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // When text changes, re-apply the current filter and search
                filterAndSearchBookings()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Attaches a real-time listener to the 'bookings' collection.
     * Sorts by bookingId (which is a timestamp) to show the newest bookings first.
     */
    private fun loadBookings() {
        db.collection("bookings")
            .orderBy("bookingId", Query.Direction.DESCENDING) // Show newest first
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    allBookingsList.clear()
                    for (doc in snapshots) {
                        // Map Firestore doc to our Booking data class
                        val booking = doc.toObject(Booking::class.java)
                        allBookingsList.add(booking)
                    }
                    // After loading, apply the current filters
                    filterAndSearchBookings()
                }
            }
    }

    /**
     * Sets up click listeners for all filter buttons.
     */
    private fun setupFilterButtons() {
        // Set "All" as the default selected button
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
        btnCancelled.setOnClickListener {
            // Filter for both "Expired" (from timer) and "Cancelled" (if you add this)
            currentStatusFilter = listOf("Expired", "Cancelled")
            updateButtonStyles(btnCancelled)
            filterAndSearchBookings()
        }
    }

    /**
     * This function applies both the search query AND the filter status.
     */
    private fun filterAndSearchBookings() {
        val searchQuery = inputSearchBooking.text.toString().trim()

        // 1. Filter by Status (using the stored currentStatusFilter)
        val filteredByStatus = when (val filter = currentStatusFilter) {
            is String -> {
                if (filter == "All") {
                    allBookingsList // No filter
                } else {
                    allBookingsList.filter { it.status.equals(filter, ignoreCase = true) }
                }
            }
            is List<*> -> {
                // For the "Cancelled" button, we check for "Expired" or "Cancelled"
                allBookingsList.filter { booking ->
                    filter.any { status -> (status as String).equals(booking.status, ignoreCase = true) }
                }
            }
            else -> allBookingsList
        }

        // 2. Filter by Search Query (based on the status-filtered list)
        displayBookingsList.clear()
        if (searchQuery.isEmpty()) {
            displayBookingsList.addAll(filteredByStatus)
        } else {
            val filteredBySearch = filteredByStatus.filter {
                // Check against vehicleNumber and slotName
                it.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                        it.slotName.contains(searchQuery, ignoreCase = true)
            }
            displayBookingsList.addAll(filteredBySearch)
        }

        // 3. Update the RecyclerView
        bookingAdapter.notifyDataSetChanged()

        if (displayBookingsList.isEmpty() && allBookingsList.isNotEmpty()) {
            Toast.makeText(this, "No bookings match your filter.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to make the selected filter button look "active"
     */
    private fun updateButtonStyles(activeButton: Button) {
        val allButtons = listOf(btnAll, btnActive, btnCompleted, btnCancelled)
        for (button in allButtons) {
            if (button == activeButton) {
                // Active button style
                button.setBackgroundColor(Color.parseColor("#FFFFFF")) // White
                button.setTextColor(Color.parseColor("#8E44AD")) // Purple
            } else {
                // Inactive button style
                button.setBackgroundColor(Color.parseColor("#8E44AD")) // Purple
                button.setTextColor(Color.parseColor("#FFFFFF")) // White
            }
        }
    }
}


// --- Data Class ---
// This defines what a "Booking" object looks like, based on your database screenshot.
// We make Timestamps nullable because Realtime bookings don't have them until scanned.

data class Booking(
    val bookingId: String = "",
    val vehicleNumber: String = "",
    val slotName: String = "",
    val status: String = "",
    val bookingType: String = "",
    val selectedTime: String = "", // For display, e.g., "15:53 - 18:53" or "2 Hour"
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationHours: Long = 0,
    val price: Double = 0.0,
    val userEmail: String = ""
)

// --- Adapter Class ---
// This class manages how the list of Bookings is shown in the RecyclerView.

class BookingAdapter(
    private val bookings: List<Booking>
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    // A helper to format Timestamps into readable strings
    private val dateFormatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

    /**
     * Creates the view for each row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_item_bookings, parent, false)
        return BookingViewHolder(view)
    }

    /**
     * Binds the data from a Booking object to the views in a row.
     */
    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking, dateFormatter)
    }

    override fun getItemCount(): Int = bookings.size

    /**
     * The ViewHolder. It holds the references to the UI components in admin_item_bookings.xml.
     */
    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textBookingId: TextView = itemView.findViewById(R.id.textBookingId)
        private val textPlateNumber: TextView = itemView.findViewById(R.id.textPlateNumber)
        private val textSlot: TextView = itemView.findViewById(R.id.textSlot)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)

        // This function fills the views with data
        fun bind(booking: Booking, formatter: SimpleDateFormat) {
            textBookingId.text = "ID: ${booking.bookingId}"
            textPlateNumber.text = "Plate: ${booking.vehicleNumber}"
            textSlot.text = "Slot: ${booking.slotName}"
            textStatus.text = "Status: ${booking.status}"

            // Set time display based on booking type
            if (booking.bookingType == "Advance") {
                // For Advance booking, show the scheduled start and end time
                val startTime = booking.startTime?.toDate()?.let { formatter.format(it) } ?: "N/A"
                val endTime = booking.endTime?.toDate()?.let { formatter.format(it) } ?: "N/A"
                textTime.text = "$startTime  ->  $endTime"
            } else {
                // For Realtime, show when it started (if it has) and the duration
                if (booking.startTime != null) {
                    val startTime = formatter.format(booking.startTime!!.toDate())
                    textTime.text = "Started: $startTime (${booking.selectedTime})"
                } else {
                    // It's "Booked" but not "Active" yet
                    textTime.text = "Duration: ${booking.selectedTime} (Not started)"
                }
            }

            // Set status color
            when (booking.status.toLowerCase(Locale.ROOT)) {
                "active" -> textStatus.setTextColor(Color.parseColor("#2ECC71")) // Green
                "booked" -> textStatus.setTextColor(Color.parseColor("#3498DB")) // Blue
                "completed" -> textStatus.setTextColor(Color.parseColor("#95A5A6")) // Gray
                "expired" -> textStatus.setTextColor(Color.parseColor("#E74C3C")) // Red
                "cancelled" -> textStatus.setTextColor(Color.parseColor("#E74C3C")) // Red
                else -> textStatus.setTextColor(Color.parseColor("#FFFFFF")) // White
            }
        }
    }
}