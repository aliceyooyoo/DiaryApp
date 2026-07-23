package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CalendarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnWrite = findViewById<Button>(R.id.btnWrite)

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
        }
    }
}