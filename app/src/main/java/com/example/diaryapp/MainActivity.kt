package com.example.diaryapp

import DiaryDbHelper
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
        }

        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }
    override fun onResume() {
        super.onResume()

        val container = findViewById<LinearLayout>(R.id.recentDiaryContainer)
        container.removeAllViews()

        val dbHelper = DiaryDbHelper(this)
        val diaryList = dbHelper.getAllDiaries()

        for (diary in diaryList) {
            val tv = TextView(this)
            tv.text = "${diary.date}  ${diary.title}"
            tv.textSize = 16f
            tv.setPadding(16, 16, 16, 16)
            container.addView(tv)
        }
    }
}