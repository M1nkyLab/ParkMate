package com.example.parkmate.User

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_Show_Vehicle : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var addButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val vehicleList = mutableListOf<String>()
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
    }

    override fun onResume() {
        super.onResume()
        loadVehicles() // Refresh the list after returning from Add Vehicle
    }

    private fun loadVehicles() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("vehicles")
            .orderBy("createdAt") // optional: newest first
            .get()
            .addOnSuccessListener { documents ->
                vehicleList.clear()
                for (doc in documents) {
                    val plate = doc.getString("numberPlate")
                    if (!plate.isNullOrEmpty()) {
                        vehicleList.add(plate)
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
}
