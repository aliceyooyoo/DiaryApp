package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvDate = findViewById<TextView>(R.id.tvDetailDate)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvContent = findViewById<TextView>(R.id.tvDetailContent)
        val ivRepresentative = findViewById<ImageView>(R.id.ivRepresentativePhoto)
        val subPhotoContainer = findViewById<LinearLayout>(R.id.subPhotoContainer)
        val stickerOverlay = findViewById<FrameLayout>(R.id.stickerOverlay)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnEdit = findViewById<TextView>(R.id.btnEdit)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        val date = intent.getStringExtra("date") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val content = intent.getStringExtra("content") ?: ""
        val imageUriString = intent.getStringExtra("imageUri") ?: ""
        val sticker = intent.getStringExtra("sticker") ?: ""

        tvDate.text = date
        tvTitle.text = title
        tvContent.text = content

        btnBack.setOnClickListener { finish() }

        // 스티커를 저장된 위치 그대로 오버레이에 복원
        if (sticker.isNotEmpty()) {
            try {
                val arr = JSONArray(sticker)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val stickerView = TextView(this).apply {
                        text = obj.getString("emoji")
                        textSize = 32f
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        x = obj.getDouble("x").toFloat()
                        y = obj.getDouble("y").toFloat()
                    }
                    stickerOverlay.addView(stickerView)
                }
            } catch (e: Exception) {
                // 예전 방식(이모지 문자열만 저장된 데이터) 호환: 왼쪽 위부터 순서대로 표시
                var offset = 0
                for (emoji in sticker) {
                    val stickerView = TextView(this).apply {
                        text = emoji.toString()
                        textSize = 32f
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        x = (40 + offset * 60).toFloat()
                        y = 8f
                    }
                    stickerOverlay.addView(stickerView)
                    offset++
                }
            }
        }

        // 진짜 수정 버튼 연결 (데이터를 들고 WriteActivity로 이동)
        btnEdit.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java).apply {
                putExtra("edit_title", title)
                putExtra("edit_date", date)
                putExtra("edit_content", content)
                putExtra("edit_sticker", sticker)
                putExtra("edit_imageUri", imageUriString)
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

        subPhotoContainer.removeAllViews()

        if (imageUriString.isNotEmpty()) {
            val uris = imageUriString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (uris.isNotEmpty()) {
                try {
                    val repUri = Uri.parse(uris[0])
                    val inputStream = contentResolver.openInputStream(repUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        ivRepresentative.setImageBitmap(bitmap)
                        ivRepresentative.visibility = View.VISIBLE
                    } else {
                        ivRepresentative.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ivRepresentative.visibility = View.GONE
                }

                if (uris.size > 1) {
                    ivRepresentative.post {
                        val totalWidth = ivRepresentative.width
                        val subUris = uris.subList(1, uris.size)
                        val spacing = (8 * resources.displayMetrics.density).toInt()
                        val totalSpacing = spacing * (subUris.size - 1)
                        val eachWidth = (totalWidth - totalSpacing) / subUris.size
                        val eachHeight = (80 * resources.displayMetrics.density).toInt()

                        subPhotoContainer.removeAllViews()
                        for (i in subUris.indices) {
                            try {
                                val subUri = Uri.parse(subUris[i])
                                val subStream = contentResolver.openInputStream(subUri)
                                val subBitmap = BitmapFactory.decodeStream(subStream)
                                subStream?.close()

                                if (subBitmap != null) {
                                    val subIv = ImageView(this).apply {
                                        layoutParams = LinearLayout.LayoutParams(eachWidth, eachHeight).apply {
                                            setMargins(if (i > 0) spacing else 0, 0, 0, 0)
                                        }
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        setImageBitmap(subBitmap)
                                    }
                                    subPhotoContainer.addView(subIv)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } else {
                ivRepresentative.visibility = View.GONE
            }
        } else {
            ivRepresentative.visibility = View.GONE
        }

        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)

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