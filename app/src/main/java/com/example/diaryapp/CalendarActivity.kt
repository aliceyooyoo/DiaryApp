package com.example.diaryapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPreviousMonth: TextView
    private lateinit var btnNextMonth: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var postContainer: LinearLayout
    private lateinit var dbHelper: DiaryDbHelper

    private val currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "캘린더"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedDate.gravity = android.view.Gravity.START
        postContainer = findViewById(R.id.postContainer)
        dbHelper = DiaryDbHelper(this)

        updateCalendar()

        btnPreviousMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun extractEmojis(stickerRaw: String): String {
        if (stickerRaw.isEmpty()) return ""
        return try {
            val arr = JSONArray(stickerRaw)
            val sb = StringBuilder()
            for (i in 0 until arr.length()) {
                sb.append(arr.getJSONObject(i).getString("emoji"))
            }
            sb.toString()
        } catch (e: Exception) {
            stickerRaw
        }
    }

    private fun updateCalendar() {
        calendarGrid.removeAllViews()

        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)
        tvMonthYear.text = monthFormat.format(currentCalendar.time)

        val firstDay = Calendar.getInstance()
        firstDay.set(year, month, 1)
        val firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK)
        val lastDay = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            addEmptyDay()
        }
        for (day in 1..lastDay) {
            addDay(day)
        }
    }

    private fun addEmptyDay() {
        val dp = resources.displayMetrics.density
        val textView = TextView(this)
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = (64 * dp).toInt()
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        calendarGrid.addView(textView)
    }

    private fun addDay(day: Int) {
        val dp = resources.displayMetrics.density

        val dayContainer = LinearLayout(this)
        dayContainer.orientation = LinearLayout.VERTICAL
        dayContainer.gravity = android.view.Gravity.CENTER

        dayContainer.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = (64 * dp).toInt()
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }

        val textView = TextView(this)
        textView.text = day.toString()
        textView.textSize = 14f
        textView.gravity = android.view.Gravity.CENTER
        dayContainer.addView(textView)

        val dateStr = "${currentCalendar.get(Calendar.MONTH) + 1}월 ${day}일"
        val diariesOnDay = dbHelper.getDiariesByDate(dateStr)

        if (diariesOnDay.isNotEmpty()) {
            val firstDiary = diariesOnDay.last()
            val firstUri = firstDiary.imageUri
                ?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }

            if (!firstUri.isNullOrEmpty()) {
                val thumbSize = (32 * dp).toInt()   // 20 → 32로 확대
                val thumb = ImageView(this)
                thumb.layoutParams = LinearLayout.LayoutParams(thumbSize, thumbSize).apply {
                    topMargin = (2 * dp).toInt()
                }
                thumb.scaleType = ImageView.ScaleType.CENTER_CROP
                thumb.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                thumb.clipToOutline = true
                try {
                    thumb.setImageURI(Uri.parse(firstUri))
                } catch (e: Exception) {
                    thumb.setBackgroundColor(Color.parseColor("#D9D9D9"))
                }
                dayContainer.addView(thumb)
            }
        }

        dayContainer.setOnClickListener {
            tvSelectedDate.text = dateStr
            postContainer.removeAllViews()

            val diaries = dbHelper.getDiariesByDate(dateStr)

            if (diaries.isEmpty()) {
                val emptyText = TextView(this)
                emptyText.text = "작성한 일기가 없습니다."
                emptyText.textSize = 16f
                postContainer.addView(emptyText)
            } else {
                for (diary in diaries) {
                    postContainer.addView(DiaryCardHelper.createCard(this, diary))
                }
            }
        }

        calendarGrid.addView(dayContainer)
    }
}