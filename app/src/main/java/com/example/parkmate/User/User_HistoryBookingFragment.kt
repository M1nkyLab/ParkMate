package com.example.parkmate.User

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

// --- Data Model ---
data class Booking(
    val bookingId: String = "",
    val slotName: String = "",
    val selectedTime: String = "",
    val vehicleNumber: String = "",
    val price: Double = 0.0,
    val status: String = "",
    val gateAccess: Boolean = false,
    val date: String = ""
)

class User_HistoryBookingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_history_fragment, container, false)

        recyclerView = view.findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bookingAdapter = BookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        loadUserBookings()

        return view
    }

    private fun loadUserBookings() {
        val userEmail = auth.currentUser?.email ?: return

        db.collection("bookings")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { result ->
                bookingList.clear()

                // --- Formatters ---
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault()) // AM/PM format

                for (doc in result) {
                    val startTime = doc.getTimestamp("startTime")
                    val endTime = doc.getTimestamp("endTime")

                    val dateFormatted: String = if (startTime != null && endTime != null) {
                        val startDate = startTime.toDate()
                        val endDate = endTime.toDate()

                        val dateStr = dateFormatter.format(startDate)
                        val startTimeStr = timeFormatter.format(startDate)
                        val endTimeStr = timeFormatter.format(endDate)

                        // Example: "Nov 08, 2025, 02:00 PM – 04:00 PM"
                        "$dateStr, $startTimeStr – $endTimeStr"
                    } else {
                        "Unknown Date"
                    }

                    val booking = Booking(
                        bookingId = doc.getString("bookingId") ?: doc.id,
                        slotName = doc.getString("slotName") ?: "Unknown",
                        selectedTime = doc.getString("selectedTime") ?: "Unknown",
                        vehicleNumber = doc.getString("vehicleNumber") ?: "N/A",
                        price = doc.getDouble("price") ?: 0.0,
                        status = doc.getString("status") ?: "Unknown",
                        gateAccess = doc.getBoolean("gateAccess") ?: false,
                        date = dateFormatted
                    )
                    bookingList.add(booking)
                }

                // Sort by newest booking first
                bookingList.sortByDescending { it.bookingId }
                bookingAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("HistoryFragment", "Error loading bookings", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load history: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- RecyclerView Adapter ---
    inner class BookingAdapter(private val list: List<Booking>) :
        RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

        inner class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val slotName: TextView = view.findViewById(R.id.slotNameText)
            val duration: TextView = view.findViewById(R.id.durationText)
            val price: TextView = view.findViewById(R.id.priceText)
            val date: TextView = view.findViewById(R.id.dateText)
            val status: TextView = view.findViewById(R.id.statusText)
            val qrCode: ImageView = view.findViewById(R.id.qrCodeView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.user_item_booking_history, parent, false)
            return BookingViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = list[position]

            holder.slotName.text = "Slot: ${booking.slotName}"
            holder.duration.text = "Duration: ${booking.selectedTime}"
            holder.price.text = "Price: RM %.2f".format(booking.price)
            holder.date.text = "Date: ${booking.date}"
            holder.status.text = "Status: ${booking.status}"

            // Optional: color-code status
            when (booking.status.lowercase(Locale.getDefault())) {
                "booked" -> holder.status.setTextColor(Color.parseColor("#FFA500")) // orange
                "completed" -> holder.status.setTextColor(Color.parseColor("#4CAF50")) // green
                "cancelled" -> holder.status.setTextColor(Color.parseColor("#F44336")) // red
                else -> holder.status.setTextColor(Color.WHITE)
            }

            generateQRCode("Booking ID: ${booking.bookingId}", holder.qrCode)
        }

        override fun getItemCount(): Int = list.size
    }

    // --- QR Code Generator ---
    private fun generateQRCode(data: String, imageView: ImageView) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 200, 200)
            val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            for (x in 0 until 200) {
                for (y in 0 until 200) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
