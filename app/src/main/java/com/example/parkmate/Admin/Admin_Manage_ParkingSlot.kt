package com.example.parkmate.Admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.example.parkmate.Admin.ParkingSlot
import com.google.firebase.firestore.FirebaseFirestore

class Admin_Manage_ParkingSlot : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ParkingSlotAdapter
    private val slotList = mutableListOf<ParkingSlot>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_manage_parkingslot)

        recyclerView = findViewById(R.id.recyclerParkingSlots)
        val btnAddSlot = findViewById<Button>(R.id.btnAddSlot)

        adapter = ParkingSlotAdapter(this, slotList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAddSlot.setOnClickListener {
            val intent = Intent(this, Admin_Add_NewSlot::class.java)
            startActivity(intent)
        }

        loadParkingSlots()
    }

    private fun loadParkingSlots() {
        db.collection("parking_slots")
            .get()
            .addOnSuccessListener { documents ->
                slotList.clear()
                for (doc in documents) {
                    val slot = doc.toObject(ParkingSlot::class.java).copy(slotId = doc.id)
                    slotList.add(slot)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onResume() {
        super.onResume()
        loadParkingSlots() // refresh list after editing or adding
    }
}
