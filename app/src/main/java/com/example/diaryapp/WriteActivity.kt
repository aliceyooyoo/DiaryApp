package com.example.diaryapp

import DiaryDbHelper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var selectedSticker: String = ""

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val imgSelected = findViewById<ImageView>(R.id.imgSelected)
            imgSelected.setImageURI(it)
            imgSelected.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)

        val btnAddPhoto = findViewById<Button>(R.id.btnAddPhoto)
        val btnAddSticker = findViewById<Button>(R.id.btnAddSticker)
        val tvSelectedSticker = findViewById<TextView>(R.id.tvSelectedSticker)

        btnClose.setOnClickListener {
            finish()
        }

        btnAddPhoto.setOnClickListener {
            getImage.launch("image/*")
        }

        // 스티커(이모지) 카테고리 선택 다이얼로그
        btnAddSticker.setOnClickListener {
            showEmojiCategoryDialog(tvSelectedSticker)
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())

            val dbHelper = DiaryDbHelper(this)
            dbHelper.insertDiary(today, title, content)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // 1단계: 카테고리 선택
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

    // 2단계: 카테고리별 세부 이모지 선택
    private fun showEmojiListDialog(categoryIndex: Int, tvSelectedSticker: TextView) {
        val emojiMap = listOf(
            // 0: 감정 / 표정
            arrayOf("😀", "😃", "😄", "😁", "😆", "🥹", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😋", "😛", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "😴", "🥱", "🤮"),
            // 1: 날씨 / 자연
            arrayOf("☀️", "🌤️", "⛅", "🌥️", "☁️", "🌧️", "⛈️", "🌩️", "❄️", "☃️", "💨", "🌊", "🌈", "⭐", "🌟", "✨", "🌙", "🔥", "🌸", "🌷", "🌹", "🌻", "🍀", "🍁", "🍂"),
            // 2: 일상 / 활동
            arrayOf("🎉", "🎊", "🎈", "🎁", "🛍️", "🎂", "🥳", "💻", "📱", "📚", "✏️", "📝", "🎨", "🎬", "🎤", "🎧", "🚗", "✈️", "🏖️", "🏕️", "🎮", "⚽", "🏀", "🏋️", "🏃"),
            // 3: 음식 / 카페
            arrayOf("☕", "🧋", "🍰", "🍩", "🍦", "🍕", "🍔", "🍟", "🌭", "🍿", "🥐", "🍞", "🥞", "🍣", "🍜", "🍲", "🍺", "🍷", "🧃"),
            // 4: 하트 / 기타
            arrayOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💯", "👍", "👎", "👏", "🙌", "🫶", "🙏")
        )

        val selectedList = emojiMap[categoryIndex]

        AlertDialog.Builder(this)
            .setTitle("스티커 선택")
            .setItems(selectedList) { _, which ->
                selectedSticker = selectedList[which]
                tvSelectedSticker.text = selectedSticker
            }
            .setNegativeButton("뒤로가기") { _, _ ->
                showEmojiCategoryDialog(tvSelectedSticker)
            }
            .show()
    }
}