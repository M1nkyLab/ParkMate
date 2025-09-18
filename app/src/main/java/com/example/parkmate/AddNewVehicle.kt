package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AddNewVehicle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addnewvehicle) // your layout

        // Navigate to activity vehicle when button is clicked
        val button = findViewById<Button>(R.id.saveVehicleButton)
        button.setOnClickListener {
            val intent = Intent(this, ActivityVehicle::class.java)
            startActivity(intent)
        }

    }
}
