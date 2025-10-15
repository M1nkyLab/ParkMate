package com.example.parkmate.User

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class User_CompletePayment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_completepayment)

        val slotInfo = findViewById<TextView>(R.id.slotInfo)
        val qrCodeView = findViewById<ImageView>(R.id.qrCode)
        val backButton = findViewById<Button>(R.id.backToHomeBtn)

        // Get slot name from previous activity
        val slotName = intent.getStringExtra("slotName") ?: "Unknown"
        slotInfo.text = "Your Slot: $slotName"

        // Generate a QR code based on the slot name
        generateQRCode(slotName, qrCodeView)

        // Back to home button click
        backButton.setOnClickListener {
            val intent = Intent(this, User_Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
