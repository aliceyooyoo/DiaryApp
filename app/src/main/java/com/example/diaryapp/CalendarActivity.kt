package com.example.diaryapp

import android.content.Intent
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

    // 현재 보여주는 달
    private val currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_calendar)
        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)

        // XML에 만든 화면 요소 연결
        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedDate.gravity = android.view.Gravity.START
        postContainer = findViewById(R.id.postContainer)

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
        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
        }

        btnCalendar.setOnClickListener {
            // 현재 캘린더 화면이므로 아무 동작 안 함
        }
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
            tvSelectedDate.text = "${currentCalendar.get(Calendar.MONTH)+1}월 ${day}일"
            postContainer.removeAllViewsInLayout()
            // =========================
// 일기 카드 생성
// =========================

            // =========================
// 일기 카드 생성
// =========================

            val diaryCard = LinearLayout(this)
            diaryCard.orientation = LinearLayout.VERTICAL

// 카드 안쪽 여백
            diaryCard.setPadding(
                30,
                30,
                30,
                30
            )

// 카드 크기 + 카드 사이 간격
            val cardParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            cardParams.bottomMargin = 25
            diaryCard.layoutParams = cardParams


// =========================
// 카드 배경 (흰색)
// =========================

            val cardBackground = GradientDrawable()

            cardBackground.setColor(Color.WHITE)
            cardBackground.cornerRadius = 12f

            diaryCard.background = cardBackground


// =========================
// 왼쪽 사진 + 오른쪽 내용
// =========================

            val mainLayout = LinearLayout(this)
            mainLayout.orientation = LinearLayout.HORIZONTAL


// -------------------------
// 왼쪽 사진
// -------------------------

            val photo = TextView(this)

            photo.text = "사진"
            photo.textSize = 14f
            photo.gravity = Gravity.CENTER

            val photoParams = LinearLayout.LayoutParams(
                120,
                200
            )

            photo.layoutParams = photoParams


// -------------------------
// 오른쪽 내용
// -------------------------

            val textLayout = LinearLayout(this)

            textLayout.orientation = LinearLayout.VERTICAL

            val textParams = LinearLayout.LayoutParams(
                0,
                200
            )

            textParams.weight = 1f
            textParams.leftMargin = 25

            textLayout.layoutParams = textParams


// -------------------------
// 제목
// -------------------------

            val title = TextView(this)

            title.text = "오늘의 일기"
            title.textSize = 20f
            title.gravity = Gravity.START

            textLayout.addView(title)


// -------------------------
// 제목과 본문 사이 여백
// -------------------------

            val titleGap = TextView(this)

            titleGap.text = ""

            val titleGapParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                25
            )

            textLayout.addView(
                titleGap,
                titleGapParams
            )


// -------------------------
// 본문
// -------------------------

            val content = TextView(this)

            content.text = "오늘 하루는 정말 즐거웠다."
            content.textSize = 16f
            content.gravity = Gravity.START

            textLayout.addView(content)


// -------------------------
// 본문과 장소/날씨 사이 여백
// -------------------------

            val contentGap = TextView(this)

            contentGap.text = ""

            val contentGapParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )

            textLayout.addView(
                contentGap,
                contentGapParams
            )


// -------------------------
// 장소 + 날씨
// -------------------------

            val infoLayout = LinearLayout(this)

            infoLayout.orientation = LinearLayout.HORIZONTAL


// 장소
            val location = TextView(this)

            location.text = "◎ 홍대"
            location.textSize = 14f


// 날씨
            val weather = TextView(this)

            weather.text = "날씨 맑음"
            weather.textSize = 14f
            weather.gravity = Gravity.END


            val weatherParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            weatherParams.weight = 1f


// 장소 + 날씨 배치
            infoLayout.addView(location)

            infoLayout.addView(
                weather,
                weatherParams
            )


// 오른쪽 영역에 장소 + 날씨 추가
            textLayout.addView(infoLayout)


// -------------------------
// 사진 + 오른쪽 내용 합치기
// -------------------------

            mainLayout.addView(photo)
            mainLayout.addView(textLayout)


// -------------------------
// 카드에 추가
// -------------------------

            diaryCard.addView(mainLayout)


// -------------------------
// 화면에 카드 표시
// -------------------------

            postContainer.addView(diaryCard)
        }

        calendarGrid.addView(textView)
    }
}