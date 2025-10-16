package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class Admin_Add_NewSlot : AppCompatActivity() {

    private lateinit var inputSlotId: EditText
    private lateinit var btnSaveSlot: Button
    private val db = FirebaseFirestore.getInstance() // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_add_newslot)

        inputSlotId = findViewById(R.id.inputSlotId)
        btnSaveSlot = findViewById(R.id.btnSaveSlot)

        btnSaveSlot.setOnClickListener {
            saveSlot()
        }
    }

    private fun saveSlot() {
        val slotId = inputSlotId.text.toString().trim()

        if (slotId.isEmpty()) {
            Toast.makeText(this, "Please enter a Slot ID", Toast.LENGTH_SHORT).show()
            return
        }

        val slot = hashMapOf(
            "slotId" to slotId,
            "status" to "Available"
        )

        db.collection("parking_slots")
            .document(slotId) // using slotId as document ID
            .set(slot)
            .addOnSuccessListener {
                Toast.makeText(this, "Slot added successfully", Toast.LENGTH_SHORT).show()
                finish() // go back to Manage Parking page
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add slot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
