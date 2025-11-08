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

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        val txtRole: TextView = itemView.findViewById(R.id.txtRole)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteUser)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_manage_item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.txtUsername.text = user.username
        holder.txtEmail.text = user.email
        holder.txtRole.text = "Role: ${user.role}"

        if (user.role.equals("admin", ignoreCase = true)) {
            holder.txtUsername.setTextColor(0xFFFFD700.toInt()) // Gold color
        } else {
            holder.txtUsername.setTextColor(0xFFFFFFFF.toInt()) // White color
        }

        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, Admin_Edit_User::class.java)
            intent.putExtra("USER_ID", user.uid)
            context.startActivity(intent)
        }

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete ${user.username}?")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("users").document(user.uid)
                        .delete()
                        .addOnSuccessListener {
                            userList.removeAt(holder.adapterPosition)
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

    override fun getItemCount(): Int = userList.size
}