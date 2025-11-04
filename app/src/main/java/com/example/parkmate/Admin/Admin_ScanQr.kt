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
import com.google.firebase.Timestamp // 1. IMPORT TIMESTAMP
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.Calendar // 2. IMPORT CALENDAR
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

    // --- Camera Setup (No changes needed) ---

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
                                // 1. Clean the scanned string to remove whitespace
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
                    .addOnSuccessListener { barcodes ->
                        // Stop processing as soon as we find one valid barcode
                        if (barcodes.isNotEmpty()) {
                            barcodes[0].rawValue?.let { value ->
                                onQrCodeDetected(value)
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

    // --- END Camera Setup ---


    /**
     * This is the "brain" of your app.
     * It handles both ENTRY and EXIT scans.
     */
    private fun verifyBooking(bookingId: String) {

        // --- THIS IS THE FIX ---
        // The QR code contains "Booking ID: 1762237390440".
        // We split the string at the ":" and take the last part, then trim whitespace.
        val idToSearch = bookingId.split(":").last().trim()
        // idToSearch will now be "1762237390440"

        // We use the new idToSearch to find the document
        val bookingRef = db.collection("bookings").document(idToSearch)

        bookingRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Use "slotName" to match your booking data
                    val slotName = document.getString("slotName") ?: "Unknown"
                    val status = document.getString("status") ?: "Booked"

                    when (status) {
                        "Booked" -> {
                            // --- 1. ENTRY LOGIC (TIMER STARTS NOW) ---
                            val durationHours = document.getLong("durationHours")?.toInt() ?: 0
                            val calendar = Calendar.getInstance()
                            val startTime = Timestamp(calendar.time)
                            calendar.add(Calendar.HOUR_OF_DAY, durationHours)
                            val endTime = Timestamp(calendar.time)

                            bookingRef.update(
                                "status", "Active",
                                "gateAccess", true,
                                "startTime", startTime,
                                "endTime", endTime
                            )
                            db.collection("parking_slots").document(slotName)
                                .update("status", "Occupied")

                            showDialog("✅ Access Granted (Entry)", "Gate opened for Slot $slotName. Timer for $durationHours hours has started.")
                        }

                        "Active" -> {
                            // --- 2. EXIT LOGIC ---
                            bookingRef.update("status", "Completed")
                            db.collection("parking_slots").document(slotName)
                                .update("status", "Available")
                            showDialog("✅ Goodbye! (Exit)", "Slot $slotName is now available.")
                        }

                        "Completed", "Expired" -> {
                            // --- 3. ALREADY USED ---
                            showDialog("⚠️ Access Denied", "This QR code has already been used.")
                        }

                        else -> {
                            showDialog("❌ Invalid Booking", "Unknown status: $status")
                        }
                    }
                } else {
                    // This error will no longer show "Booking ID: ..."
                    showDialog("❌ Access Denied", "Invalid QR Code. Booking not found. Scanned data: $bookingId")
                }
            }
            .addOnFailureListener {
                showDialog("⚠️ System Error", "Failed to verify booking: ${it.message}")
            }
    }

    /**
     * Helper to show dialog and close activity
     */
    private fun showDialog(title: String, message: String) {
        // I am removing the "V2 DEBUG" from the title now that we found the bug
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish() // Close the scanner and go back to the admin main page
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

