package com.example.parkmate.User

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.text.NumberFormat
import java.util.Locale

class User_CompletePayment : AppCompatActivity() {

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

        // --- Get data from previous page ---
        val slotName = intent.getStringExtra("slotName") ?: "Unknown Slot"
        val selectedTime = intent.getStringExtra("selectedTime") ?: "Unknown Duration"
        val price = intent.getDoubleExtra("price", 0.0)

        // Format price
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        val formattedPrice = format.format(price)

        // --- Display slot info ---
        slotInfo.text = "Your Slot: $slotName\nDuration: $selectedTime\nTotal: $formattedPrice"

        // --- Update Firestore slot status to Booked ---
        updateSlotStatus(slotName)

        // --- Generate QR Code ---
        val qrData = "Slot: $slotName\nDuration: $selectedTime\nTotal: $formattedPrice"
        generateQRCode(qrData, qrCodeView)

        // --- Back to home button ---
        backButton.setOnClickListener {
            val intent = Intent(this, User_Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateSlotStatus(slotId: String) {
        val slotRef = db.collection("parking_slots").document(slotId)
        slotRef.update("status", "Booked")
            .addOnSuccessListener {
                // optional: toast or log
            }
            .addOnFailureListener { e ->
                slotInfo.append("\n⚠ Failed to update status: ${e.message}")
            }
    }

    private fun generateQRCode(data: String, imageView: ImageView) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 400, 400)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
