package com.example.parkmate.User

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

// Firestore data model
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
        val view = inflater.inflate(R.layout.user_historyfragment, container, false)

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

                for (doc in result) {
                    val timestamp = doc.getLong("timestamp")
                    val dateFormatted = if (timestamp != null) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        sdf.format(Date(timestamp))
                    } else {
                        doc.getString("date") ?: "Unknown"
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

                // Sort by newest first (timestamp or bookingId)
                bookingList.sortByDescending { it.bookingId }

                bookingAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    // RecyclerView Adapter
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
                .inflate(R.layout.item_booking_history, parent, false)
            return BookingViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = list[position]

            holder.slotName.text = "Slot: ${booking.slotName}"
            holder.duration.text = "Duration: ${booking.selectedTime}"
            holder.price.text = "Price: RM %.2f".format(booking.price)
            holder.date.text = "Date: ${booking.date}"
            holder.status.text = "Status: ${booking.status}"

            generateQRCode("Booking ID: ${booking.bookingId}", holder.qrCode)
        }

        override fun getItemCount(): Int = list.size
    }

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
