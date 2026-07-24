package com.example.diaryapp

import com.example.diaryapp.DiaryDbHelper
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
    private lateinit var photoContainer: LinearLayout
    private var representativeImageUri: Uri? = null

    private val getImage = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            photoList.clear()
            photoList.addAll(uris)
            if (representativeImageUri == null || !photoList.contains(representativeImageUri)) {
                representativeImageUri = photoList.firstOrNull()
            }
            updatePhotoListUI()
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

        photoContainer = findViewById(R.id.photoContainer)

        btnClose.setOnClickListener {
            finish()
        }

        btnAddPhoto.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddSticker.setOnClickListener {
            showEmojiCategoryDialog(tvSelectedSticker)
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "제목이나 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())

            val dbHelper = DiaryDbHelper(context = this)
            val imageUriString = representativeImageUri?.toString() ?: ""

            dbHelper.insertDiary(date = today, title = title, content = content, imageUri = imageUriString)

            Toast.makeText(this, "일기가 작성되었습니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updatePhotoListUI() {
        photoContainer.removeAllViews()

        for (uri in photoList) {
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
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP

                if (uri == representativeImageUri) {
                    setBackgroundColor(Color.RED)
                    setPadding(4, 4, 4, 4)
                }

                setOnClickListener {
                    representativeImageUri = uri
                    updatePhotoListUI()
                }
            }

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
                        representativeImageUri = photoList.firstOrNull()
                    }
                    updatePhotoListUI()
                }
            }

            frameLayout.addView(imageView)
            frameLayout.addView(deleteBtn)
            photoContainer.addView(frameLayout)
        }
    }

    private fun showEmojiCategoryDialog(tvSelectedSticker: TextView) {
        val categories = arrayOf("감정", "날씨", "동물", "음식")
        AlertDialog.Builder(this)
            .setTitle("스티커 카테고리를 선택하세요")
            .setItems(categories) { _, which ->
                when (which) {
                    0 -> {
                        val emotions = arrayOf(
                            "😊", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                            "🙂", "🙃", "😉", "😇", "🥰", "😍", "🤩", "😘",
                            "😋", "😛", "😜", "🤪", "😎", "🥳", "😏",
                            "😞", "😔", "😢", "😭", "😤", "😠", "😡",
                            "👍", "👎", "👏", "🙌", "👋", "🤝", "❤️", "🧡",
                            "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💕", "🔥"
                        )
                        showEmojiPicker(tvSelectedSticker, emotions)
                    }
                    1 -> {
                        val weathers = arrayOf(
                            "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️",
                            "⛈️", "🌩️", "⚡", "❄️", "🌨️", "☃️", "⛄",
                            "🌬️", "💨", "🌪️", "🌫️", "☔", "💧", "💦", "🌈"
                        )
                        showEmojiPicker(tvSelectedSticker, weathers)
                    }
                    2 -> {
                        val realAnimals = arrayOf(
                            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
                            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
                            "🐧", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗",
                            "🐴", "🦄", "🐝", "🐛", "🦋", "🐞", "🐢", "🐙"
                        )
                        showEmojiPicker(tvSelectedSticker, realAnimals)
                    }
                    3 -> {
                        val realFoods = arrayOf(
                            "🍎", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🍍",
                            "🥝", "🍅", "🥑", "🍞", "🥐", "🥖", "🥨", "🧀",
                            "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕", "핫도그",
                            "🌮", "🌯", "🥙", "🥗", "🥪", "🥫", "🍝", "🍜",
                            "🍲", "🍛", "🍣", "🍱", "🥟", "오뎅", "🍚",
                            "🍙", "🍘", "🍡", "🍧", "🍨", "🍦", "케이크",
                            "🎂", "🍮", "🍭", "🍬", "🍫", "팝콘", "🍩", "🍪",
                            "☕", "🍵", "🥤", "🧃", "🍺", "🍻", "🍷", "🍸", "🍹"
                        )
                        showEmojiPicker(tvSelectedSticker, realFoods)
                    }
                }
            }
            .show()
    }

    private fun showEmojiPicker(tvSelectedSticker: TextView, emojis: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("스티커를 선택하세요")
            .setItems(emojis) { _, which ->
                val selectedEmoji = emojis[which]
                tvSelectedSticker.text = selectedEmoji
                tvSelectedSticker.visibility = View.VISIBLE
            }
            .show()
    }
}