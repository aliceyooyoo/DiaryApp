package com.example.diaryapp

import DiaryDbHelper
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private val photoList = mutableListOf<Uri>()
    private var representativeImageUri: Uri? = null
    private var selectedSticker: String = ""

    private val getImage = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            photoList.clear()
            photoList.addAll(uris)
            representativeImageUri = uris[0] // 첫 번째 사진을 기본 대표사진으로 지정
            updatePhotoListUI()
            Toast.makeText(this, "사진 ${uris.size}장이 첨부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val btnClose = findViewById<TextView>(R.id.btnClose)

        val btnAddPhoto = findViewById<TextView>(R.id.btnAddPhoto)
        val btnAddSticker = findViewById<TextView>(R.id.btnAddSticker)
        val tvSelectedSticker = findViewById<TextView>(R.id.tvSelectedSticker)

        // 사진 추가 버튼 클릭
        btnAddPhoto.setOnClickListener {
            getImage.launch("image/*")
        }

        // 스티커 선택 버튼 클릭
        btnAddSticker.setOnClickListener {
            showEmojiCategoryDialog(tvSelectedSticker)
        }

        // 닫기 및 완료 버튼
        btnClose.setOnClickListener { finish() }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())

            val dbHelper = DiaryDbHelper(this)
            val imageUriString = representativeImageUri?.toString() ?: ""

            dbHelper.insertDiary(today, title, content, imageUriString)

            Toast.makeText(this, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // 사진 가로 나열 및 대표사진 테두리 / 삭제 버튼 반영 UI 갱신
    private fun updatePhotoListUI() {
        val photoContainer = findViewById<LinearLayout>(R.id.photoContainer)
        photoContainer.removeAllViews()

        for (uri in photoList) {
            val frameLayout = FrameLayout(this).apply {
                val params = LinearLayout.LayoutParams(200, 200)
                params.setMargins(0, 0, 16, 0)
                layoutParams = params
            }

            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)

                // 대표사진인 경우 녹색 테두리 강조
                if (uri == representativeImageUri) {
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    setPadding(4, 4, 4, 4)
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(0, 0, 0, 0)
                }
            }

            // 대표사진 뱃지 표시
            if (uri == representativeImageUri) {
                val badgeView = TextView(this).apply {
                    text = "대표"
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#80000000"))
                    setPadding(8, 2, 8, 2)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(6, 6, 0, 0)
                    }
                }
                frameLayout.addView(badgeView)
            }

            // 우측 상단 ✕ 삭제 버튼
            val deleteBtn = TextView(this).apply {
                text = "✕"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#99000000"))
                setPadding(10, 2, 10, 2)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                }

                setOnClickListener {
                    photoList.remove(uri)
                    if (representativeImageUri == uri) {
                        representativeImageUri = if (photoList.isNotEmpty()) photoList[0] else null
                    }
                    updatePhotoListUI()
                    Toast.makeText(this@WriteActivity, "사진이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            // 이미지 클릭 시 대표사진으로 변경
            imageView.setOnClickListener {
                representativeImageUri = uri
                updatePhotoListUI()
                Toast.makeText(this, "대표사진으로 지정되었습니다.", Toast.LENGTH_SHORT).show()
            }

            frameLayout.addView(imageView)
            frameLayout.addView(deleteBtn)
            photoContainer.addView(frameLayout)
        }
    }

    private fun showEmojiCategoryDialog(tvSelectedSticker: TextView) {
        val categories = arrayOf("😀 감정 / 표정", "☀️ 날씨 / 자연", "🎉 일상 / 활동", "🍔 음식 / 카페", "❤️ 하트 / 기타")

        AlertDialog.Builder(this)
            .setTitle("스티커 카테고리")
            .setItems(categories) { _, which ->
                showEmojiListDialog(which, tvSelectedSticker)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEmojiListDialog(categoryIndex: Int, tvSelectedSticker: TextView) {
        val emojiMap = listOf(
            arrayOf("😀", "😃", "😄", "😁", "😆", "🥹", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😋", "😛", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭"),
            arrayOf("☀️", "🌤️", "⛅", "🌥️", "☁️", "🌧️", "⛈️", "🌩️", "❄️", "☃️", "💨", "🌊", "🌈", "⭐", "🌟", "✨", "🌙", "🔥", "🌸", "🌷", "🌹", "🌻", "🍀"),
            arrayOf("🎉", "🎊", "🎈", "🎁", "🛍️", "🎂", "🥳", "💻", "📱", "📚", "✏️", "📝", "🎨", "🎬", "🎤", "🎧", "🚗", "✈️", "🏖️", "🏕️", "🎮", "⚽", "🏀"),
            arrayOf("☕", "🧋", "🍰", "🍩", "🍦", "🍕", "🍔", "🍟", "🍿", "🥐", "🍞", "🥞", "🍣", "🍜", "🍲", "🍺", "🍷", "🧃"),
            arrayOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💯", "👍", "👎", "👏", "🙌", "🫶", "🙏")
        )

        val selectedList = emojiMap[categoryIndex]

        AlertDialog.Builder(this)
            .setTitle("스티커 선택")
            .setItems(selectedList) { _, which ->
                selectedSticker = selectedList[which]
                tvSelectedSticker.text = selectedSticker
                tvSelectedSticker.visibility = View.VISIBLE
            }
            .setNegativeButton("뒤로가기") { _, _ ->
                showEmojiCategoryDialog(tvSelectedSticker)
            }
            .show()
    }
}