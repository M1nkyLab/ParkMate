package com.example.parkmate.User.RealtimeBooking

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.example.parkmate.User.User_Home
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.text.NumberFormat
import java.util.Locale

class User_CompletePayment_RealtimeBooking : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var slotInfo: TextView
    private lateinit var qrCodeView: ImageView
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_completepayment)

        slotInfo = findViewById(R.id.slotInfo)
        qrCodeView = findViewById(R.id.qrCode)
        backButton = findViewById(R.id.backToHomeBtn)
        db = FirebaseFirestore.getInstance()

        val bookingId = intent.getStringExtra("bookingId") ?: return

        loadBookingData(bookingId)

        backButton.setOnClickListener {
            val intent = Intent(this, User_Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadBookingData(bookingId: String) {
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val slotName = doc.getString("slotName")
                    val selectedTime = doc.getString("selectedTime")
                    val vehicleNumber = doc.getString("vehicleNumber")
                    val price = doc.getDouble("price") ?: 0.0

                    val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
                    val formattedPrice = format.format(price)

                    slotInfo.text = """
                        Your Slot: $slotName
                        Duration: $selectedTime
                        Plate: $vehicleNumber
                        Total: $formattedPrice
                    """.trimIndent()

                    generateQRCode(bookingId, qrCodeView)
                } else {
                    slotInfo.text = "❌ Booking not found."
                }
            }
            .addOnFailureListener {
                slotInfo.text = "⚠ Failed to load booking data."
            }
    }

    private fun generateQRCode(data: String, imageView: ImageView) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 500, 500)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
