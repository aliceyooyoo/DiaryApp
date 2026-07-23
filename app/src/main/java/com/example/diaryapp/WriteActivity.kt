package com.example.diaryapp

import DiaryDbHelper
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)

        btnClose.setOnClickListener {
            finish()
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())

            val dbHelper = DiaryDbHelper(this)
            dbHelper.insertDiary(today, title, content)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}