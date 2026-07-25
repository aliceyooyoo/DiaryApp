package com.example.diaryapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.URL

class DetailActivity : AppCompatActivity() {

    private var diaryId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        diaryId = intent.getIntExtra("diaryId", -1)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)
        val btnEdit = findViewById<TextView>(R.id.btnEdit)

        btnBack.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("일기 삭제")
                .setMessage("이 일기를 삭제할까요?")
                .setPositiveButton("삭제") { _, _ ->
                    DiaryDbHelper(this).deleteDiary(diaryId)
                    finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("diaryId", diaryId)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDiary()
    }

    private fun loadDiary() {
        val diary = DiaryDbHelper(this).getDiaryById(diaryId) ?: return

        findViewById<TextView>(R.id.tvDetailDate).text = diary.date
        findViewById<TextView>(R.id.tvDetailTitle).text = diary.title
        findViewById<TextView>(R.id.tvDetailContent).text = diary.content

        val tvWeather = findViewById<TextView>(R.id.tvDetailWeather)
        if (!diary.weatherTemp.isNullOrEmpty()) {
            tvWeather.text = "${diary.weatherTemp} ${diary.weatherDesc ?: ""}"
        } else {
            tvWeather.text = "날씨 정보 없음"
        }

        val ivWeatherIcon = findViewById<ImageView>(R.id.ivDetailWeatherIcon)
        if (!diary.weatherIcon.isNullOrEmpty()) {
            fetchWeatherIconBitmap(diary.weatherIcon) { bitmap ->
                ivWeatherIcon.setImageBitmap(bitmap)
            }
        }

        val ivPhoto = findViewById<ImageView>(R.id.ivDetailPhoto)
        if (!diary.imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(diary.imageUri)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap)
                    ivPhoto.visibility = View.VISIBLE
                } else {
                    ivPhoto.visibility = View.GONE
                }
            } catch (e: Exception) {
                ivPhoto.visibility = View.GONE
            }
        } else {
            ivPhoto.visibility = View.GONE
        }

        val stickerCanvas = findViewById<FrameLayout>(R.id.detailStickerCanvas)
        stickerCanvas.removeAllViews()

        if (!diary.stickerData.isNullOrEmpty()) {
            diary.stickerData.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 3) {
                    val emoji = parts[0]
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f

                    val stickerView = TextView(this).apply {
                        text = emoji
                        textSize = 32f
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        this.x = x
                        this.y = y
                    }
                    stickerCanvas.addView(stickerView)
                }
            }
        }
    }

    private fun fetchWeatherIconBitmap(iconCode: String, onResult: (android.graphics.Bitmap) -> Unit) {
        Thread {
            try {
                val url = URL("https://openweathermap.org/img/wn/$iconCode@2x.png")
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                runOnUiThread { onResult(bitmap) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}