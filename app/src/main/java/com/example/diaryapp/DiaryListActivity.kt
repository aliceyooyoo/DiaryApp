package com.example.diaryapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DiaryListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        val container = findViewById<LinearLayout>(R.id.diaryContainer)

        val dbHelper = DiaryDbHelper(this)
        val diaryList = dbHelper.getAllDiaries()

        for (diary in diaryList) {
            container.addView(createDiaryCard(diary))
        }

        // 하단 탭바 버튼 클릭 리스너 연결
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnWrite = findViewById<LinearLayout>(R.id.btnWrite)
        val btnCalendar = findViewById<LinearLayout>(R.id.btnCalendar)

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
            finish()
        }

        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            finish()
        }
    }

    private fun createDiaryCard(diary: Diary): LinearLayout {
        val dp = resources.displayMetrics.density

        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 0, 0, (12 * dp).toInt())
        card.layoutParams = cardParams
        card.setBackgroundColor(Color.WHITE)

        // 카드 클릭 시 상세 화면(DetailActivity)으로 이동
        card.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("date", diary.date)
                putExtra("title", diary.title)
                putExtra("content", diary.content)
                putExtra("imageUri", diary.imageUri)
                putExtra("sticker", diary.sticker)
                putExtra("place", diary.place)
            }
            startActivity(intent)
        }

        // 사진 뷰 설정
        val photoView = ImageView(this)
        val photoSize = (72 * dp).toInt()
        val photoParams = LinearLayout.LayoutParams(photoSize, photoSize)
        photoParams.setMargins(0, 0, (16 * dp).toInt(), 0)
        photoView.layoutParams = photoParams
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP

        if (!diary.imageUri.isNullOrEmpty()) {
            try {
                val firstUriStr = diary.imageUri.split(",").map { it.trim() }.firstOrNull { it.isNotEmpty() }
                if (!firstUriStr.isNullOrEmpty()) {
                    photoView.setImageURI(Uri.parse(firstUriStr))
                } else {
                    photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
                }
            } catch (e: Exception) {
                photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
            }
        } else {
            photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
        }

        // 우측 텍스트 영역
        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL
        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        right.layoutParams = rightParams

        val date = TextView(this)
        date.text = diary.date
        date.textSize = 12f
        date.setTextColor(Color.parseColor("#888888"))

        val title = TextView(this)
        title.text = diary.title
        title.textSize = 16f
        title.setTextColor(Color.parseColor("#000000"))
        title.setPadding(0, (2 * dp).toInt(), 0, (2 * dp).toInt())

        val content = TextView(this)
        content.text = diary.content
        content.textSize = 13f
        content.setTextColor(Color.parseColor("#333333"))
        content.maxLines = 2

        right.addView(date)
        right.addView(title)
        right.addView(content)

        // 하단 장소 레이아웃 (날씨 제거)
        val tvPlace = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * dp).toInt()
            }
            textSize = 11f
            setTextColor(Color.parseColor("#666666"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        if (!diary.place.isNullOrEmpty()) {
            tvPlace.text = "📍 ${diary.place}"
            tvPlace.visibility = View.VISIBLE
            right.addView(tvPlace)
        } else {
            tvPlace.visibility = View.GONE
        }

        card.addView(photoView)
        card.addView(right)

        return card
    }
}