package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ActivityVehicle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityvehicle) // your layout

        // Navigate to AddNewVehicleActivity when button is clicked
        val button = findViewById<Button>(R.id.addVehicleButton)
        button.setOnClickListener {
            val intent = Intent(this, AddNewVehicle::class.java)
            startActivity(intent)
        }
    }
}
