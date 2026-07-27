package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class DiaryListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        val container = findViewById<LinearLayout>(R.id.diaryContainer)

        val dbHelper = DiaryDbHelper(this)
        val diaryList = dbHelper.getAllDiaries()

        for (diary in diaryList) {
            container.addView(DiaryCardHelper.createCard(this, diary))
        }

        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)

        btnHome.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        btnWrite.setOnClickListener { startActivity(Intent(this, WriteActivity::class.java)) }
        btnCalendar.setOnClickListener { startActivity(Intent(this, CalendarActivity::class.java)) }
    }
}