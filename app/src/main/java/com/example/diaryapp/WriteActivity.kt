package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private val apiKey = "894efd0493fa47be9bd9c09d27182253"
    private var currentWeather: WeatherResult? = null

    private val photoList = mutableListOf<Uri>()
    private var representativeImageUri: Uri? = null
    private var selectedSticker: String = ""

    private val getImage = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            photoList.clear()
            photoList.addAll(uris)
            representativeImageUri = uris[0]
            updatePhotoListUI()
            Toast.makeText(this, "사진 ${uris.size}장이 첨부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)
        val btnAddPhoto = findViewById<TextView>(R.id.btnAddPhoto)
        val btnAddSticker = findViewById<TextView>(R.id.btnAddSticker)
        val tvSelectedSticker = findViewById<TextView>(R.id.tvSelectedSticker)
        val btnAcademicCalendar = findViewById<TextView>(R.id.btnAcademicCalendar)
        btnAcademicCalendar.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.swu.ac.kr/swu/927/subview.do"))
            startActivity(intent)
        }

        // TODO: 나중에 C가 만든 실제 GPS 좌표로 교체 (지금은 서울 좌표로 임시 고정)
        fetchWeather(37.5665, 126.9780) { result ->
            currentWeather = result
            findViewById<TextView>(R.id.tvWriteWeatherTemp).text = "${result.temp}°C"
            findViewById<TextView>(R.id.tvWriteWeatherDesc).text = result.description
            result.iconBitmap?.let {
                findViewById<ImageView>(R.id.ivWriteWeatherIcon).setImageBitmap(it)
            }
        }

        btnClose.setOnClickListener { finish() }

        btnAddPhoto.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddSticker.setOnClickListener {
            showEmojiCategoryDialog(tvSelectedSticker)
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())
            val imageUriString = representativeImageUri?.toString()

            val dbHelper = DiaryDbHelper(this)
            dbHelper.insertDiary(
                today, title, content,
                currentWeather?.icon,
                currentWeather?.let { "${it.temp}°C" },
                currentWeather?.description,
                imageUriString,
                selectedSticker
            )

            Toast.makeText(this, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun fetchWeather(lat: Double, lon: Double, onResult: (WeatherResult) -> Unit) {
        Thread {
            try {
                val urlStr = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=kr"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val main = json.getJSONObject("main")
                val weather = json.getJSONArray("weather").getJSONObject(0)

                val temp = main.getDouble("temp").toInt()
                val tempMax = main.getDouble("temp_max").toInt()
                val tempMin = main.getDouble("temp_min").toInt()
                val humidity = main.getInt("humidity")
                val description = weather.getString("description")
                val icon = weather.getString("icon")

                val iconUrl = URL("https://openweathermap.org/img/wn/$icon@2x.png")
                val iconBitmap = BitmapFactory.decodeStream(iconUrl.openStream())

                val result = WeatherResult(temp, tempMax, tempMin, humidity, description, icon, iconBitmap)
                runOnUiThread { onResult(result) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

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

                if (uri == representativeImageUri) {
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    setPadding(4, 4, 4, 4)
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(0, 0, 0, 0)
                }
            }

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
            .setTitle("스티커 카테고리 선택")
            .setItems(categories) { _, which ->
                showEmojiListDialog(which, tvSelectedSticker)
            }
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