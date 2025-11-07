package com.example.parkmate.User

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_View_Vehicle : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var addButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Store vehicle IDs + number plates
    private val vehicleList = mutableListOf<String>()
    private val vehicleIdList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_view_vehicles)

        listView = findViewById(R.id.vehicleListView)
        addButton = findViewById(R.id.addVehicleButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, vehicleList)
        listView.adapter = adapter

        loadVehicles()

        addButton.setOnClickListener {
            startActivity(Intent(this, User_AddNew_Vehicle::class.java))
        }

        // On item click → show edit/delete dialog
        listView.setOnItemClickListener { _, _, position, _ ->
            if (vehicleIdList.isEmpty() || position >= vehicleIdList.size) return@setOnItemClickListener

            val vehicleId = vehicleIdList[position]
            val currentPlate = vehicleList[position]

            showOptionsDialog(vehicleId, currentPlate)
        }
    }

    override fun onResume() {
        super.onResume()
        loadVehicles()
    }

    private fun loadVehicles() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("vehicles")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { documents ->
                vehicleList.clear()
                vehicleIdList.clear()

                for (doc in documents) {
                    val plate = doc.getString("numberPlate")
                    if (!plate.isNullOrEmpty()) {
                        vehicleList.add(plate)
                        vehicleIdList.add(doc.id)
                    }
                }

                if (vehicleList.isEmpty()) {
                    vehicleList.add("No vehicles found.")
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading vehicles: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showOptionsDialog(vehicleId: String, currentPlate: String) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle(currentPlate)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(vehicleId, currentPlate)
                    1 -> confirmDelete(vehicleId)
                }
            }
            .show()
    }

    private fun showEditDialog(vehicleId: String, currentPlate: String) {
        val input = EditText(this)
        input.hint = "Enter new number plate"
        input.setText(currentPlate)
        input.setSingleLine(true)
        input.setPadding(32, 24, 32, 24)

        AlertDialog.Builder(this)
            .setTitle("Edit Vehicle")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPlate = input.text.toString().trim().uppercase()
                if (newPlate.isNotEmpty()) {
                    updateVehicle(vehicleId, newPlate)
                } else {
                    Toast.makeText(this, "Number plate cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateVehicle(vehicleId: String, newPlate: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("vehicles").document(vehicleId)
            .update("numberPlate", newPlate)
            .addOnSuccessListener {
                Toast.makeText(this, "Vehicle updated", Toast.LENGTH_SHORT).show()
                loadVehicles()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(vehicleId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete this vehicle?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVehicle(vehicleId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVehicle(vehicleId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("vehicles").document(vehicleId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Vehicle deleted", Toast.LENGTH_SHORT).show()
                loadVehicles()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
