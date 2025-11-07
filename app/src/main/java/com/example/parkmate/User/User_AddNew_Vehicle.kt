package com.example.parkmate.User

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_AddNew_Vehicle : AppCompatActivity() {

    private lateinit var vehicleInput: EditText
    private lateinit var saveButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_add_newvehicle)

        vehicleInput = findViewById(R.id.vehicleInput)
        saveButton = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            val plate = vehicleInput.text.toString().trim()
            val userId = auth.currentUser?.uid

            if (plate.isEmpty()) {
                Toast.makeText(this, "Please enter a number plate", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId != null) {
                val vehicleData = hashMapOf(
                    "numberPlate" to plate,
                    "createdAt" to System.currentTimeMillis()
                )

                // Add vehicle under the user's subcollection
                db.collection("users").document(userId)
                    .collection("vehicles")
                    .add(vehicleData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vehicle added successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Return to the list page
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
