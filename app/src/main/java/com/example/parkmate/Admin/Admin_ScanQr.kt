package com.example.parkmate.Admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView

class Admin_ScanQr : AppCompatActivity() {

    private lateinit var barcodeView: CompoundBarcodeView
    private val db = FirebaseFirestore.getInstance()
    private var scanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_scanqr)

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.decodeContinuous(callback)
        barcodeView.resume()
    }

    private val callback = BarcodeCallback { result: BarcodeResult? ->
        if (result != null && !scanned) {
            scanned = true
            val scannedId = result.text
            verifyBooking(scannedId)
        }
    }

    private fun verifyBooking(bookingId: String) {
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val slotNumber = document.getString("slotNumber") ?: "Unknown"
                    val status = document.getString("status") ?: "Booked"

                    if (status == "Booked") {
                        document.reference.update("status", "Used")
                        showDialog("✅ Gate opened for Slot $slotNumber")
                    } else {
                        showDialog("⚠️ QR code already used!")
                    }
                } else {
                    showDialog("❌ Invalid QR code")
                }
            }
            .addOnFailureListener {
                showDialog("⚠️ Error verifying booking")
            }
    }

    private fun showDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Gate Access")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
