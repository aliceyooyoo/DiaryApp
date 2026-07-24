package com.example.diaryapp
import com.example.diaryapp.DiaryDbHelper
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

        val photoBox = View(this)
        val photoSize = (72 * dp).toInt()
        val photoParams = LinearLayout.LayoutParams(photoSize, photoSize)
        photoParams.setMargins(0, 0, (16 * dp).toInt(), 0)
        photoBox.layoutParams = photoParams
        photoBox.setBackgroundColor(Color.parseColor("#D9D9D9"))

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

        right.addView(date)
        right.addView(title)
        right.addView(content)

        card.addView(photoBox)
        card.addView(right)

        return card
    }
}