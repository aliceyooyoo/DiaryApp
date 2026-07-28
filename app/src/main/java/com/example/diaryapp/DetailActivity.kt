package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvDate = findViewById<TextView>(R.id.tvDetailDate)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvContent = findViewById<TextView>(R.id.tvDetailContent)
        val tvDetailPlace = findViewById<TextView>(R.id.tvDetailPlace)
        val detailStickerCanvas = findViewById<FrameLayout>(R.id.detailStickerCanvas)
        val photoContainer = findViewById<LinearLayout>(R.id.photoContainer)

        val btnEdit = findViewById<TextView>(R.id.btnEdit)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        val date = intent.getStringExtra("date") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val content = intent.getStringExtra("content") ?: ""
        val place = intent.getStringExtra("place") ?: ""
        val imageUriString = intent.getStringExtra("imageUri") ?: ""
        val sticker = intent.getStringExtra("sticker") ?: ""

        tvDate.text = date
        tvTitle.text = title
        tvContent.text = content

        if (place.isNotEmpty()) {
            tvDetailPlace.text = "📍 $place"
            tvDetailPlace.visibility = View.VISIBLE
        } else {
            tvDetailPlace.visibility = View.GONE
        }

        detailStickerCanvas.removeAllViews()
        if (sticker.isNotEmpty()) {
            sticker.split(",").forEach { entry ->
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
                    detailStickerCanvas.addView(stickerView)
                }
            }
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java).apply {
                putExtra("edit_title", title)
                putExtra("edit_date", date)
                putExtra("edit_content", content)
                putExtra("edit_sticker", sticker)
                putExtra("edit_imageUri", imageUriString)
                putExtra("edit_place", place)
            }
            startActivity(intent)
            finish()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("일기 삭제")
                .setMessage("정말 이 일기를 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    val dbHelper = DiaryDbHelper(this)
                    dbHelper.deleteDiary(title, date)
                    Toast.makeText(this, "일기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        photoContainer.removeAllViews()
        if (imageUriString.isNotEmpty()) {
            val uris = imageUriString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (uriStr in uris) {
                try {
                    val uri = Uri.parse(uriStr)

                    val frameLayout = FrameLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                            setMargins(0, 0, 16, 0)
                        }
                    }

                    val imageView = ImageView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }

                    frameLayout.addView(imageView)
                    photoContainer.addView(frameLayout)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

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
}