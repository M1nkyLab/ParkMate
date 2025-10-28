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

// Data class to represent a booking document from Firestore
data class Booking(
    val bookingId: String = "",
    val slotName: String = "",
    val duration: String = "",
    val price: Double = 0.0,
    val status: String = "",
    val date: String = "" // ✅ Added date field
)

class User_HistoryBookingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_historyfragment, container, false)

        recyclerView = view.findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        db = FirebaseFirestore.getInstance()

        bookingAdapter = BookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        loadUserBookings()

        return view
    }

    private fun loadUserBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                bookingList.clear()
                for (doc in result) {
                    val booking = Booking(
                        bookingId = doc.getString("bookingId") ?: doc.id,
                        slotName = doc.getString("slotName") ?: "Unknown",
                        duration = doc.getString("duration") ?: "Unknown",
                        price = doc.getDouble("price") ?: 0.0,
                        status = doc.getString("status") ?: "Unknown",
                        date = doc.getString("date") ?: "Unknown" // ✅ Get date from Firestore
                    )
                    bookingList.add(booking)
                }

                // Sort by most recent (if bookingId or date is time-based)
                bookingList.sortByDescending { it.date }

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
            val date: TextView = view.findViewById(R.id.dateText) // ✅ Added
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
            holder.duration.text = "Duration: ${booking.duration}"
            holder.price.text = "Price: RM %.2f".format(booking.price)
            holder.date.text = "Date: ${booking.date}" // ✅ Display date
            holder.status.text = "Status: ${booking.status}"

            // Generate QR code
            generateQRCode(booking.bookingId, holder.qrCode)
        }

        override fun getItemCount(): Int = list.size
    }

    // QR code generator
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
