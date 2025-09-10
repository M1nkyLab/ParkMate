package com.example.parkmate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class LoginRegister : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loginregister)

        val gotologin : Button = findViewById(R.id.goToLogin)
        val gotoregister : Button = findViewById(R.id.goToRegister)

        gotologin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)

            gotoregister.setOnClickListener {
                val intent = Intent(this, Register::class.java)
                startActivity(intent)

            }
        }
    }
}
