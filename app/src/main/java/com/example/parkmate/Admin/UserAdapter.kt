package com.example.parkmate.Admin

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class UserAdapter(
    private val context: Context,
    private val userList: MutableList<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * ViewHolder holds the views for a single item in the list.
     * It now includes a reference to the new btnEdit.
     */
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These IDs must match your admin_manage_itemuser.xml
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        val txtRole: TextView = itemView.findViewById(R.id.txtRole)
        val txtCreatedAt: TextView = itemView.findViewById(R.id.txtCreatedAt)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteUser)
        // --- THIS IS THE UPDATE ---
        val btnEdit: Button = itemView.findViewById(R.id.btnEditUser)
    }

    /**
     * Inflates the XML layout (admin_manage_itemuser.xml) for each row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_manage_item_user, parent, false)
        return UserViewHolder(view)
    }

    /**
     * Binds the data from the userList to the views in the ViewHolder.
     */
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        // Get the user data for this position
        val user = userList[position]

        // Set the text for each view
        holder.txtUsername.text = user.username
        holder.txtEmail.text = user.email
        holder.txtRole.text = "Role: ${user.role}"
        holder.txtCreatedAt.text = "Joined: ${user.createdAt}" // Assuming createdAt is a String

        // Highlight admins differently
        if (user.role.equals("admin", ignoreCase = true)) {
            holder.txtUsername.setTextColor(0xFFFFD700.toInt()) // Gold color
        } else {
            holder.txtUsername.setTextColor(0xFFFFFFFF.toInt()) // White color
        }

        // --- THIS IS THE NEW CLICK LISTENER FOR THE EDIT BUTTON ---
        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, Admin_Edit_User::class.java)
            // Pass the unique user ID (uid) to the new activity
            // The Admin_EditUser activity will use this ID to load the user's data
            intent.putExtra("USER_ID", user.uid)
            context.startActivity(intent)
        }

        // --- Set OnClickListener for the Delete button ---
        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete ${user.username}?")
                .setPositiveButton("Delete") { _, _ ->
                    // Delete the user from Firestore
                    db.collection("users").document(user.uid)
                        .delete()
                        .addOnSuccessListener {
                            // Remove the user from the list and notify the adapter
                            userList.removeAt(holder.adapterPosition) // More robust way to get position
                            notifyItemRemoved(holder.adapterPosition)
                            notifyItemRangeChanged(holder.adapterPosition, userList.size)
                            Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to delete user: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Returns the total number of items in the list.
     */
    override fun getItemCount(): Int = userList.size
}