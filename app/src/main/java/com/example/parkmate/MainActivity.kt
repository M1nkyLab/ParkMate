package com.example.parkmate

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.parkmate.User.User_Home
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        val btnstart : Button = findViewById(R.id.btnstart)

        btnstart.setOnClickListener {
            val intent = Intent(this, User_Home::class.java)
            startActivity(intent)
        }

        // Get and log the FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log the token
            Log.d("FCM_TOKEN", token)
            
            // You can also display it in a Toast for quick access during debugging
            // Toast.makeText(baseContext, "FCM Token: $token", Toast.LENGTH_LONG).show()
        }
    }
}