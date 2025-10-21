package com.example.parkmate.Admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkmate.R
import com.google.firebase.firestore.FirebaseFirestore

class Admin_ManageUser : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_manage_users)

        recyclerView = findViewById(R.id.recyclerUsers)
        adapter = UserAdapter(this, userList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (doc in documents) {
                    val user = doc.toObject(User::class.java).copy(uid = doc.id)
                    userList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUsers() // refresh list after changes
    }
}
