package com.example.parkmate.Admin

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user",
    val createdAt: String = ""
)
