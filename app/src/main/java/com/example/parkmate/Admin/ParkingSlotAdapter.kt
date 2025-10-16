package com.example.parkmate.Admin

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.Admin.ParkingSlot
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class ParkingSlotAdapter(
    private val context: Context,
    private val slotList: MutableList<ParkingSlot>
) : RecyclerView.Adapter<ParkingSlotAdapter.SlotViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val slotName: TextView = itemView.findViewById(R.id.textSlotName)
        val slotStatus: TextView = itemView.findViewById(R.id.textSlotStatus)

        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slotList[position]

        // Display Slot ID and availability
        holder.slotName.text = slot.slotId   // or slot.slotName if you rename
        holder.slotStatus.text = slot.status

        // Delete functionality
        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Slot")
                .setMessage("Are you sure you want to delete ${slot.slotId}?")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("parking_slots").document(slot.slotId)
                        .delete()
                        .addOnSuccessListener {
                            slotList.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        .addOnFailureListener { e -> e.printStackTrace() }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = slotList.size
}
