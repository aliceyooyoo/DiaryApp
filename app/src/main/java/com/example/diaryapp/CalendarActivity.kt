package com.example.diaryapp

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPreviousMonth: Button
    private lateinit var btnNextMonth: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var postContainer: LinearLayout
    private lateinit var dbHelper: DiaryDbHelper

    // 현재 보여주는 달
    private val currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // activity_main.xml 연결
        setContentView(R.layout.activity_calendar)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.title = "캘린더"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // XML에 만든 화면 요소 연결
        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedDate.gravity = android.view.Gravity.START
        postContainer = findViewById(R.id.postContainer)
        dbHelper = DiaryDbHelper(this)

        // 처음 캘린더 표시
        updateCalendar()

        // 이전 달 버튼
        btnPreviousMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        // 다음 달 버튼
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

        // 기존 날짜 제거
        calendarGrid.removeAllViews()

        // 현재 연도와 월 가져오기
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        // 화면 위에 "2026년 7월" 표시
        val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)
        tvMonthYear.text = monthFormat.format(currentCalendar.time)

        // 해당 월의 첫 번째 날
        val firstDay = Calendar.getInstance()
        firstDay.set(year, month, 1)

        // 1일이 무슨 요일인지 확인
        // 일요일 = 1, 월요일 = 2, ... 토요일 = 7
        val firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK)

        // 해당 월의 마지막 날짜
        val lastDay = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 1일 전 빈칸 만들기
        for (i in 1 until firstDayOfWeek) {
            addEmptyDay()
        }

        // 실제 날짜 생성
        for (day in 1..lastDay) {
            addDay(day)
        }
    }

    // 빈 날짜 칸
    private fun addEmptyDay() {
        val textView = TextView(this)

        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 80
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }

        calendarGrid.addView(textView)
    }

    // 실제 날짜 칸
    private fun addDay(day: Int) {

        val textView = TextView(this)

        textView.text = day.toString()
        textView.textSize = 16f
        textView.gravity = android.view.Gravity.CENTER

        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 80
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        textView.setOnClickListener {

            val selectedDate =
                "${currentCalendar.get(Calendar.MONTH) + 1}월 ${day}일"


            tvSelectedDate.text = selectedDate


            postContainer.removeAllViews()


            val diaries = dbHelper.getDiariesByDate(selectedDate)


            if (diaries.isEmpty()) {

                val emptyText = TextView(this)

                emptyText.text = "작성한 일기가 없습니다."
                emptyText.textSize = 16f

                postContainer.addView(emptyText)

            } else {

                for (diary in diaries) {
                    val diarycard = LinearLayout(this)

                    val title = TextView(this)
                    title.text = "📖 ${diary.title}"
                    title.textSize = 20f


                    val content = TextView(this)
                    content.text = if (diary.content.length > 30) {
                        diary.content.substring(0, 30) + "..."
                    } else {
                        diary.content
                    }
                    content.textSize = 16f

                    val location = TextView(this)

                    location.text = "📍 ${diary.place}"
                    location.textSize = 14f


                    val sticker = TextView(this)

                    sticker.text = diary.sticker
                    sticker.textSize = 25f

                    val titleParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    titleParams.bottomMargin = 20


                    val contentParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    contentParams.bottomMargin = 20


                    val locationParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    locationParams.bottomMargin = 20


                    diarycard.addView(title)

                    val gap1 = TextView(this)
                    gap1.height = 20
                    diarycard.addView(gap1)

                    diarycard.addView(content)

                    val gap2 = TextView(this)
                    gap2.height = 20
                    diarycard.addView(gap2)

                    diarycard.addView(location)

                    val gap3 = TextView(this)
                    gap3.height = 20
                    diarycard.addView(gap3)

                    diarycard.addView(sticker)

                    postContainer.addView(diarycard)
                }
            }
        }
        calendarGrid.addView(textView)
    }
}