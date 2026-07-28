package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.widget.Toolbar

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPreviousMonth: TextView
    private lateinit var btnNextMonth: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var postContainer: LinearLayout
    private lateinit var dbHelper: DiaryDbHelper

    private lateinit var btnHomeTab: LinearLayout
    private lateinit var btnWriteTab: LinearLayout
    private lateinit var btnCalendarTab: LinearLayout

    private val currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
        supportActionBar?.title = "캘린더"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        postContainer = findViewById(R.id.postContainer)
        dbHelper = DiaryDbHelper(this)

        btnHomeTab = findViewById(R.id.btnHomeTab)
        btnWriteTab = findViewById(R.id.btnWriteTab)
        btnCalendarTab = findViewById(R.id.btnCalendarTab)

        btnHomeTab.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnWriteTab.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
            finish()
        }

        btnCalendarTab.setOnClickListener {
            // 현재 화면
        }

        updateCalendar()
        loadDiariesForDate(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1, currentCalendar.get(Calendar.DAY_OF_MONTH))

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
            addDay(year, month + 1, day)
        }
    }

    private fun addEmptyDay() {
        val textView = TextView(this)
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 80
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        calendarGrid.addView(textView)
    }

    private fun addDay(year: Int, month: Int, day: Int) {
        val textView = TextView(this)
        textView.text = day.toString()
        textView.textSize = 16f
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.BLACK)

        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 80
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }

        textView.setOnClickListener {
            loadDiariesForDate(year, month, day)
        }
        calendarGrid.addView(textView)
    }

    private fun loadDiariesForDate(year: Int, month: Int, day: Int) {
        val selectedDateStr = "${month}월 ${day}일"
        tvSelectedDate.text = selectedDateStr
        postContainer.removeAllViews()

        val diaries = dbHelper.getDiariesByDate(selectedDateStr)

        if (diaries.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "작성한 일기가 없습니다."
            emptyText.textSize = 16f
            emptyText.setTextColor(Color.GRAY)
            emptyText.setPadding(10, 10, 10, 10)
            postContainer.addView(emptyText)
        } else {
            for (diary in diaries) {
                val diarycard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(30, 30, 30, 30)
                    setBackgroundColor(Color.parseColor("#FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 20)
                    }

                    setOnClickListener {
                        val intent = Intent(this@CalendarActivity, DetailActivity::class.java).apply {
                            putExtra("title", diary.title)
                            putExtra("date", diary.date)
                            putExtra("content", diary.content)
                            putExtra("imageUri", diary.imageUri)
                            putExtra("sticker", diary.sticker)
                            putExtra("place", diary.place)
                        }
                        startActivity(intent)
                    }
                }

                // [수정] 대표 사진(첫 번째 사진) 딱 1장만 추출해서 표시
                if (!diary.imageUri.isNullOrEmpty()) {
                    val uris = diary.imageUri!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (uris.isNotEmpty()) {
                        try {
                            val uri = Uri.parse(uris[0])
                            val imageView = ImageView(this).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    500
                                ).apply {
                                    setMargins(0, 0, 0, 16)
                                }
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }

                            val inputStream = contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                            }

                            diarycard.addView(imageView)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val title = TextView(this).apply {
                    text = "📖 ${diary.title}"
                    textSize = 18f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                }

                val content = TextView(this).apply {
                    text = if (diary.content.length > 30) {
                        diary.content.substring(0, 30) + "..."
                    } else {
                        diary.content
                    }
                    textSize = 15f
                    setTextColor(Color.DKGRAY)
                }

                val location = TextView(this).apply {
                    text = "📍 ${diary.place}"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                }

                // 스마트 스티커 처리
                val stickerName = if (!diary.sticker.isNullOrEmpty()) {
                    diary.sticker!!.split(",")[0].split("@")[0]
                } else {
                    ""
                }

                val stickerImageView = if (stickerName.isNotEmpty() && stickerName.startsWith("sticker_smart")) {
                    val resId = resources.getIdentifier(stickerName, "drawable", packageName)
                    if (resId != 0) {
                        ImageView(this).apply {
                            setImageResource(resId)
                            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                                setMargins(0, 10, 0, 0)
                            }
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    } else null
                } else null

                diarycard.addView(title)
                diarycard.addView(TextView(this).apply { height = 10 })
                diarycard.addView(content)
                diarycard.addView(TextView(this).apply { height = 10 })

                if (!diary.place.isNullOrEmpty()) {
                    diarycard.addView(location)
                    diarycard.addView(TextView(this).apply { height = 10 })
                }

                if (stickerImageView != null) {
                    diarycard.addView(stickerImageView)
                }

                postContainer.addView(diarycard)
            }
        }
    }
}