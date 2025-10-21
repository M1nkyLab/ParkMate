package com.example.parkmate.Admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class UserAdapter(
    private val context: Context,
    private val userList: MutableList<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        val txtRole: TextView = itemView.findViewById(R.id.txtRole)
        val txtCreatedAt: TextView = itemView.findViewById(R.id.txtCreatedAt)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_manage_itemuser, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.txtUsername.text = user.username
        holder.txtEmail.text = user.email
        holder.txtRole.text = "Role: ${user.role}"
        holder.txtCreatedAt.text = "Joined: ${user.createdAt}"

        // Highlight admins differently
        if (user.role == "admin") {
            holder.txtUsername.setTextColor(0xFFFFD700.toInt()) // Gold color
        } else {
            holder.txtUsername.setTextColor(0xFFFFFFFF.toInt())
        }

        // Delete button → confirm before deleting
        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete ${user.username}?")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("users").document(user.uid)
                        .delete()
                        .addOnSuccessListener {
                            userList.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        .addOnFailureListener { e -> e.printStackTrace() }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = userList.size
}
