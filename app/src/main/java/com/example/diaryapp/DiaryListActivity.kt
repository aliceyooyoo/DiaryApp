package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
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

        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)

        btnHome.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        btnWrite.setOnClickListener { startActivity(Intent(this, WriteActivity::class.java)) }
        btnCalendar.setOnClickListener { startActivity(Intent(this, CalendarActivity::class.java)) }
    }

    private fun loadImageSafely(imageView: ImageView, uriString: String?) {
        if (uriString.isNullOrEmpty()) {
            imageView.setImageDrawable(null)
            imageView.setBackgroundColor(Color.parseColor("#D9D9D9"))
            return
        }
        try {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setBackgroundColor(Color.parseColor("#D9D9D9"))
            }
        } catch (e: Exception) {
            imageView.setImageDrawable(null)
            imageView.setBackgroundColor(Color.parseColor("#D9D9D9"))
        }
    }

    private fun createDiaryCard(diary: Diary): LinearLayout {
        val dp = resources.displayMetrics.density

        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, (12 * dp).toInt())
        card.layoutParams = cardParams
        card.setBackgroundColor(Color.WHITE)

        card.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("diaryId", diary.id)
            startActivity(intent)
        }

        val photoView = ImageView(this)
        val photoSize = (72 * dp).toInt()
        val photoParams = LinearLayout.LayoutParams(photoSize, photoSize)
        photoParams.setMargins(0, 0, (16 * dp).toInt(), 0)
        photoView.layoutParams = photoParams
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP
        loadImageSafely(photoView, diary.imageUri)

        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL

        val date = TextView(this)
        date.text = diary.date
        date.textSize = 12f
        date.setTextColor(Color.parseColor("#888888"))

        val title = TextView(this)
        title.text = diary.title
        title.textSize = 16f

        val content = TextView(this)
        content.text = diary.content
        content.textSize = 13f
        content.maxLines = 2

        val weather = TextView(this)
        weather.text = if (!diary.weatherTemp.isNullOrEmpty()) "${diary.weatherTemp} ${diary.weatherDesc ?: ""}" else ""
        weather.textSize = 11f
        weather.setTextColor(Color.parseColor("#999999"))

        right.addView(date)
        right.addView(title)
        right.addView(content)
        right.addView(weather)

        card.addView(photoView)
        card.addView(right)

        return card
    }
}