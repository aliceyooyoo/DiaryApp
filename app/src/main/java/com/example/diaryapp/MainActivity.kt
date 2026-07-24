package com.example.diaryapp

import DiaryDbHelper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)
        val btnAcademicCalendar = findViewById<TextView>(R.id.btnAcademicCalendar)

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
        }

        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

         //학사일정 이동
        btnAcademicCalendar?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.swu.ac.kr/swu/927/subview.do"))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        val container = findViewById<LinearLayout>(R.id.recentDiaryContainer)
        container.removeAllViews()

        val dbHelper = DiaryDbHelper(this)
        val diaryList = dbHelper.getAllDiaries()

        for (diary in diaryList) {
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 24, 32, 24)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

           //대표사진 표시 영역
            val imgView = ImageView(this).apply {
                val imgParams = LinearLayout.LayoutParams(180, 180)
                imgParams.setMargins(0, 0, 24, 0)
                layoutParams = imgParams
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFE0E0E0.toInt())
            }


            try {
                val imageUriField = diary::class.java.getDeclaredField("imageUri")
                imageUriField.isAccessible = true
                val uriStr = imageUriField.get(diary) as? String
                if (!uriStr.isNullOrEmpty()) {
                    imgView.setImageURI(Uri.parse(uriStr))
                }
            } catch (e: Throwable) {
                imgView.setBackgroundColor(0xFFE0E0E0.toInt())
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val tvDate = TextView(this).apply {
                text = diary.date
                textSize = 12f
                setTextColor(0xFF888888.toInt())
            }

            val tvTitle = TextView(this).apply {
                text = diary.title
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 4, 0, 4)
            }

            val tvContent = TextView(this).apply {
                text = diary.content
                textSize = 13f
                setTextColor(0xFF555555.toInt())
                maxLines = 2
            }

            textLayout.addView(tvDate)
            textLayout.addView(tvTitle)
            textLayout.addView(tvContent)

            cardView.addView(imgView)
            cardView.addView(textLayout)

            container.addView(cardView)
        }
    }
}