package com.example.diaryapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object DiaryCardHelper {

    fun createCard(activity: Activity, diary: Diary): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val card = LinearLayout(activity)
        card.orientation = LinearLayout.HORIZONTAL
        card.setPadding(px(12), px(12), px(12), px(12))
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, px(12)) }

        card.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = px(14).toFloat()
        }
        card.elevation = px(2).toFloat()

        card.setOnClickListener {
            val intent = Intent(activity, DetailActivity::class.java).apply {
                putExtra("date", diary.date)
                putExtra("title", diary.title)
                putExtra("content", diary.content)
                putExtra("imageUri", diary.imageUri)
                putExtra("sticker", diary.sticker)
            }
            activity.startActivity(intent)
        }

        // 썸네일 (모서리 둥글게)
        val photoSize = px(72)
        val photoView = ImageView(activity)
        photoView.layoutParams = LinearLayout.LayoutParams(photoSize, photoSize).apply {
            setMargins(0, 0, px(14), 0)
        }
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP
        photoView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, px(10).toFloat())
            }
        }
        photoView.clipToOutline = true

        val firstUri = diary.imageUri?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
        if (!firstUri.isNullOrEmpty()) {
            try {
                photoView.setImageURI(Uri.parse(firstUri))
            } catch (e: Exception) {
                photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
            }
        } else {
            photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
        }

        // 오른쪽 텍스트 영역
        val right = LinearLayout(activity)
        right.orientation = LinearLayout.VERTICAL
        right.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val date = TextView(activity).apply {
            text = diary.date
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
        }

        val title = TextView(activity).apply {
            text = diary.title
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, px(2), 0, px(2))
        }

        val content = TextView(activity).apply {
            text = diary.content
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            maxLines = 2
        }

        // 하단: 장소(왼쪽) + 날씨(오른쪽)
        val bottomRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(6) }
        }

        val place = TextView(activity).apply {
            text = if (!diary.place.isNullOrEmpty()) "📍 ${diary.place}" else ""
            textSize = 11f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val weather = TextView(activity).apply {
            text = if (!diary.weatherTemp.isNullOrEmpty()) "${weatherEmoji(diary.weatherDesc)} ${diary.weatherTemp}" else ""
            textSize = 11f
            setTextColor(Color.parseColor("#999999"))
        }

        bottomRow.addView(place)
        bottomRow.addView(weather)

        right.addView(date)
        right.addView(title)
        right.addView(content)
        right.addView(bottomRow)

        card.addView(photoView)
        card.addView(right)

        return card
    }

    private fun weatherEmoji(desc: String?): String = when {
        desc == null -> "☁️"
        desc.contains("맑") -> "☀️"
        desc.contains("구름") -> "⛅"
        desc.contains("비") -> "🌧️"
        desc.contains("눈") -> "❄️"
        else -> "☁️"
    }
}