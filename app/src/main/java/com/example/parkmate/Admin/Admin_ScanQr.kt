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

    // The view that shows the live camera feed
    private lateinit var previewView: PreviewView
    // A service that runs the camera in the background
    private lateinit var cameraExecutor: ExecutorService
    // The connection to our Firestore database
    private val db = FirebaseFirestore.getInstance()
    // A flag to make sure we only process one QR code at a time
    private var isScanning = true

    // A unique number to identify our camera permission request
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    /**
     * This function runs when the screen is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the screen's layout to our admin_scanqr.xml file
        setContentView(R.layout.admin_scanqr)

        // Get the preview view from the layout
        previewView = findViewById(R.id.preview_view)
        // Create the background service for the camera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check if we already have permission to use the camera
        if (checkCameraPermission()) {
            // If yes, start the camera
            startCamera()
        } else {
            // If no, ask the user for permission
            requestCameraPermission()
        }
    }

    /**
     * A simple function to check if the CAMERA permission has been granted.
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Asks the user for permission to use the camera.
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /**
     * This function is called after the user responds to the permission request.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            // If permission was granted, start the camera
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                // If permission was denied, show a message and close the screen
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * This function sets up and starts the camera and the QR code analyzer.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            // Create the live preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Set up the analyzer that will look for QR codes in the camera feed
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                        // This is what happens when a QR code is found
                        if (isScanning) {
                            isScanning = false // Stop scanning to prevent multiple triggers
                            runOnUiThread {
                                val cleanBookingId = qrCode.trim() // Clean up the scanned text
                                verifyBooking(cleanBookingId) // Send the QR code text to our main logic function
                            }
                        }
                    })
                }

            // Use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Connect everything: the camera, the preview, and the analyzer
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

    /**
     * This is a helper class that does the actual work of finding a QR code in an image.
     */
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

                // Use Google's scanner to process the image
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // When a barcode is found, get the first one
                        if (barcodes.isNotEmpty()) {
                            barcodes[0].rawValue?.let { value ->
                                // Send the text value of the QR code back to our main logic
                                onQrCodeDetected(value)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        // Close the image to allow the next one to be processed
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    /**
     * This is the "brain" of the scanner. It takes the booking ID from the QR code
     * and decides what to do based on the booking's status in the database.
     */
    private fun verifyBooking(bookingId: String) {
        // Get the actual ID from the scanned text (in case there are extra characters)
        val idToSearch = bookingId.split(":").last().trim()
        // Create a reference to the specific booking document in Firestore
        val bookingRef = db.collection("bookings").document(idToSearch)

        // Get the document from the database
        bookingRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // If the booking is found, get its details
                    val slotName = document.getString("slotName") ?: "Unknown"
                    val status = document.getString("status") ?: "Booked"
                    val bookingType = document.getString("bookingType") ?: ""
                    val startTime = document.getTimestamp("startTime")

                    // --- Check for Advance Bookings that are too early ---
                    if (bookingType.equals("Reserve", ignoreCase = true) && startTime != null) {
                        val today = Calendar.getInstance()
                        val bookingDate = Calendar.getInstance().apply { time = startTime.toDate() }

                        // Check if the booking is for today's date
                        val isSameDay = today.get(Calendar.YEAR) == bookingDate.get(Calendar.YEAR) &&
                                      today.get(Calendar.DAY_OF_YEAR) == bookingDate.get(Calendar.DAY_OF_YEAR)

                        // If it's not today and the booking date is in the future...
                        if (!isSameDay && today.before(bookingDate)) {
                            val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                            val bookingDateStr = dateFormatter.format(bookingDate.time)
                            // ...show an error message and stop.
                            showDialog(
                                "⚠️ Access Denied",
                                "This booking is for a future date ($bookingDateStr). Please come back on the correct date."
                            )
                            return@addOnSuccessListener // Stop the function here
                        }
                    }
                    // --- End of the date check ---

                    // Decide what to do based on the booking's current status
                    when (status) {
                        // If the status is "Booked", it means the user is ENTERING for the first time.
                        "Booked" -> {
                            val durationHours = document.getLong("durationHours")?.toInt() ?: 0
                            val calendar = Calendar.getInstance()
                            val newStartTime = Timestamp(calendar.time) // The parking timer starts now
                            calendar.add(Calendar.HOUR_OF_DAY, durationHours)
                            val newEndTime = Timestamp(calendar.time) // Calculate when the parking will end

                            // Update the booking in the database
                            bookingRef.update(
                                mapOf(
                                    "status" to "Parked", // Change status to 'Parked'
                                    "gateAccess" to true,
                                    "startTime" to newStartTime, // Set the actual start time
                                    "endTime" to newEndTime      // Set the calculated end time
                                )
                            )

                            // Update the parking slot's status so no one else can book it
                            db.collection("parking_slots").document(slotName)
                                .update("status", "Occupied")

                            // Show a success message
                            showDialog(
                                "✅ Access Granted (Entry)",
                                "Gate opened for Slot $slotName. Status set to 'Parked'."
                            )
                        }

                        // If the status is "Parked", it means the user is already inside and is now EXITING.
                        "Parked" -> {
                            // Update the booking status
                            bookingRef.update("status", "Exited")
                            // Make the parking slot available again for others
                            db.collection("parking_slots").document(slotName)
                                .update("status", "Available")

                            // Show a success message
                            showDialog(
                                "✅ Exit Recorded",
                                "Slot $slotName is now available. Status updated to 'Exited'."
                            )
                        }

                        // If the booking is already finished or used.
                        "Exited", "Completed", "Expired" -> {
                            showDialog(
                                "⚠️ Access Denied",
                                "This QR code has already been used or the booking is completed."
                            )
                        }

                        // If the status is something unexpected.
                        else -> {
                            showDialog(
                                "❌ Invalid Booking",
                                "Unknown status: $status"
                            )
                        }
                    }
                } else {
                    // If the QR code does not match any booking in the database.
                    showDialog(
                        "❌ Access Denied",
                        "Invalid QR Code. Booking not found. Scanned data: $bookingId"
                    )
                }
            }
            .addOnFailureListener {
                // If there's a network error or other system problem.
                showDialog("⚠️ System Error", "Failed to verify booking: ${it.message}")
            }
    }

    /**
     * A simple helper function to show a pop-up message to the admin.
     * After the admin clicks "OK", it closes the scanner screen.
     */
    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish() // Close this screen
            }
            .setCancelable(false)
            .show()
    }

    /**
     * This function is called when the screen is closed, to make sure the camera is properly shut down.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
