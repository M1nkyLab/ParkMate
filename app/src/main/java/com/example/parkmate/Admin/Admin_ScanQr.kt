package com.example.parkmate.Admin

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Admin_ScanQr : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private val db = FirebaseFirestore.getInstance()
    private var isScanning = true // Use this to prevent multiple scans of the same code

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_scanqr)

        previewView = findViewById(R.id.preview_view)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                        if (isScanning) {
                            isScanning = false // Stop scanning
                            runOnUiThread {
                                val cleanBookingId = qrCode.trim()
                                verifyBooking(cleanBookingId)
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                showDialog("Error", "Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class QrCodeAnalyzer(
        private val onQrCodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    // ... inside the analyze method
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            barcodes[0].rawValue?.let { value ->
                                onQrCodeDetected(value) // Correct: Call the lambda function
                            }
                        }
                    }

                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun verifyBooking(bookingId: String) {
        val idToSearch = bookingId.split(":").last().trim()
        val bookingRef = db.collection("bookings").document(idToSearch)

        bookingRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val slotName = document.getString("slotName") ?: "Unknown"
                    val status = document.getString("status") ?: "Booked"
                    val bookingType = document.getString("bookingType") ?: ""
                    val startTime = document.getTimestamp("startTime")

                    // --- NEW: Advance Booking Date Check ---
                    if (bookingType.equals("Reserve", ignoreCase = true) && startTime != null) {
                        val today = Calendar.getInstance()
                        val bookingDate = Calendar.getInstance().apply { time = startTime.toDate() }

                        // Compare day, month, and year. Ignore the time.
                        val isSameDay = today.get(Calendar.YEAR) == bookingDate.get(Calendar.YEAR) &&
                                      today.get(Calendar.DAY_OF_YEAR) == bookingDate.get(Calendar.DAY_OF_YEAR)

                        if (!isSameDay && today.before(bookingDate)) {
                            val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                            val bookingDateStr = dateFormatter.format(bookingDate.time)
                            showDialog(
                                "⚠️ Access Denied",
                                "This booking is for a future date ($bookingDateStr). Please come back on the correct date."
                            )
                            return@addOnSuccessListener // Stop processing
                        }
                    }
                    // --- END: Advance Booking Date Check ---

                    when (status) {
                        "Booked" -> {
                            // --- ENTRY LOGIC ---
                            val durationHours = document.getLong("durationHours")?.toInt() ?: 0
                            val calendar = Calendar.getInstance()
                            val newStartTime = Timestamp(calendar.time)
                            calendar.add(Calendar.HOUR_OF_DAY, durationHours)
                            val newEndTime = Timestamp(calendar.time)

                            bookingRef.update(
                                mapOf(
                                    "status" to "Parked",
                                    "gateAccess" to true,
                                    "startTime" to newStartTime,
                                    "endTime" to newEndTime
                                )
                            )

                            db.collection("parking_slots").document(slotName)
                                .update("status", "Occupied")

                            showDialog(
                                "✅ Access Granted (Entry)",
                                "Gate opened for Slot $slotName. Status set to 'Parked'."
                            )
                        }

                        "Parked" -> {
                            // --- EXIT LOGIC ---
                            bookingRef.update("status", "Exited")
                            db.collection("parking_slots").document(slotName)
                                .update("status", "Available")

                            showDialog(
                                "✅ Exit Recorded",
                                "Slot $slotName is now available. Status updated to 'Exited'."
                            )
                        }

                        "Exited", "Completed", "Expired" -> {
                            showDialog(
                                "⚠️ Access Denied",
                                "This QR code has already been used or the booking is completed."
                            )
                        }

                        else -> {
                            showDialog(
                                "❌ Invalid Booking",
                                "Unknown status: $status"
                            )
                        }
                    }
                } else {
                    showDialog(
                        "❌ Access Denied",
                        "Invalid QR Code. Booking not found. Scanned data: $bookingId"
                    )
                }
            }
            .addOnFailureListener {
                showDialog("⚠️ System Error", "Failed to verify booking: ${it.message}")
            }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
