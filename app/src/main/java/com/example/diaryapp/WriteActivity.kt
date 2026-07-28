package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
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
import android.app.Activity.RESULT_OK
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.diaryapp.BuildConfig

class WriteActivity : AppCompatActivity() {

    private val apiKey = BuildConfig.WEATHER_API_KEY

    private val photoList = mutableListOf<Uri>()
    private lateinit var photoContainer: LinearLayout
    private lateinit var stickerOverlay: FrameLayout
    private lateinit var tvPlace: TextView
    private var representativeImageUri: Uri? = null

    // 여러 개 스티커 관리
    private val stickerViews = mutableListOf<TextView>()

    // 마지막으로 받아온 날씨 (저장 시 사용)
    private var lastWeatherTemp: String? = null
    private var lastWeatherDesc: String? = null

    private var isEditMode = false
    private var originalTitle = ""
    private var originalDate = ""
    private var selectedPlace = ""

    private var placeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val place = result.data?.getStringExtra("place")
                if (place != null) {
                    selectedPlace = place
                    tvPlace.text = "📍 $selectedPlace"
                }
            }
        }

    private val getImage =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                photoList.clear()
                photoList.addAll(uris)
                if (representativeImageUri == null || !photoList.contains(representativeImageUri)) {
                    representativeImageUri = photoList.firstOrNull()
                }
                updatePhotoListUI()
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            selectedPlace = data?.getStringExtra("place") ?: ""
            tvPlace.text = "📍 $selectedPlace"
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
        val btnLocation = findViewById<TextView>(R.id.btnLocation)

        stickerOverlay = findViewById(R.id.stickerOverlay)
        tvPlace = findViewById(R.id.tvPlace)
        photoContainer = findViewById(R.id.photoContainer)

        // 날씨 불러오기
        fetchWeather(37.5665, 126.9780) { result ->
            lastWeatherTemp = "${result.temp}°C"
            lastWeatherDesc = result.description
            findViewById<TextView>(R.id.tvTemp).text = lastWeatherTemp
            findViewById<TextView>(R.id.tvDescription).text = lastWeatherDesc
            result.iconBitmap?.let {
                findViewById<ImageView>(R.id.ivWeatherIcon).setImageBitmap(it)
            }
        }

        // 수정 모드로 들어온 경우 기존 데이터 채워넣기
        if (intent.hasExtra("edit_title")) {
            isEditMode = true
            originalTitle = intent.getStringExtra("edit_title") ?: ""
            originalDate = intent.getStringExtra("edit_date") ?: ""

            etTitle.setText(originalTitle)
            etContent.setText(intent.getStringExtra("edit_content") ?: "")

            val stickerStr = intent.getStringExtra("edit_sticker") ?: ""
            if (stickerStr.isNotEmpty()) {
                try {
                    val arr = org.json.JSONArray(stickerStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        addStickerToOverlay(
                            obj.getString("emoji"),
                            obj.getDouble("x").toFloat(),
                            obj.getDouble("y").toFloat()
                        )
                    }
                } catch (e: Exception) {
                    // 예전 방식(이모지만 저장된 데이터) 호환 처리
                    for (emoji in stickerStr) {
                        addStickerToOverlay(emoji.toString())
                    }
                }
            }

            val imageUriStr = intent.getStringExtra("edit_imageUri") ?: ""
            if (imageUriStr.isNotEmpty()) {
                val uris = imageUriStr.split(",").map { Uri.parse(it.trim()) }
                    .filter { it.toString().isNotEmpty() }
                photoList.addAll(uris)
                if (photoList.isNotEmpty()) {
                    representativeImageUri = photoList[0]
                }
                updatePhotoListUI()
            }
        }

        btnClose.setOnClickListener { finish() }

        btnAddPhoto.setOnClickListener { getImage.launch("image/*") }

        btnAddSticker.setOnClickListener { showEmojiCategoryDialog() }

        btnLocation.setOnClickListener {
            val intent = Intent(this, PlaceSearchActivity::class.java)
            placeLauncher.launch(intent)
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            val stickerJson = org.json.JSONArray().apply {
                for (sv in stickerViews) {
                    put(org.json.JSONObject().apply {
                        put("emoji", sv.text.toString())
                        put("x", sv.x)
                        put("y", sv.y)
                    })
                }
            }.toString()

            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "제목이나 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = if (isEditMode) originalDate else sdf.format(Date())

            val dbHelper = DiaryDbHelper(context = this)

            val finalUris = mutableListOf<Uri>()
            representativeImageUri?.let { rep ->
                if (photoList.contains(rep)) {
                    finalUris.add(rep)
                    for (u in photoList) {
                        if (u != rep) finalUris.add(u)
                    }
                }
            }
            if (finalUris.isEmpty()) {
                finalUris.addAll(photoList)
            }
            val imageUriString = finalUris.joinToString(",") { it.toString() }

            if (isEditMode) {
                dbHelper.updateDiary(
                    originalTitle,
                    originalDate,
                    title,
                    content,
                    imageUriString,
                    stickerJson
                )
                Toast.makeText(this, "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insertDiary(
                    date = today,
                    title = title,
                    content = content,
                    imageUri = imageUriString,
                    sticker = stickerJson,
                    place = selectedPlace,
                    weatherTemp = lastWeatherTemp,
                    weatherDesc = lastWeatherDesc
                )
                Toast.makeText(this, "일기가 작성되었습니다.", Toast.LENGTH_SHORT).show()
            }

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

    // 스티커 하나를 오버레이에 추가 (드래그 가능, 길게 누르면 삭제)
    private fun addStickerToOverlay(emoji: String, initX: Float? = null, initY: Float? = null) {
        val stickerView = TextView(this).apply {
            text = emoji
            textSize = 36f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            x = initX ?: (40f + (stickerViews.size * 30f))
            y = initY ?: (40f + (stickerViews.size * 30f))
        }

        stickerView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.tag = Pair(event.rawX - view.x, event.rawY - view.y)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val offset = view.tag as? Pair<Float, Float> ?: return@setOnTouchListener false
                    view.x = event.rawX - offset.first
                    view.y = event.rawY - offset.second
                    true
                }
                else -> false
            }
        }

        stickerView.setOnLongClickListener {
            stickerOverlay.removeView(stickerView)
            stickerViews.remove(stickerView)
            true
        }

        stickerOverlay.addView(stickerView)
        stickerViews.add(stickerView)
    }

    private fun showEmojiCategoryDialog() {
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
                        showEmojiPicker(emotions)
                    }
                    1 -> {
                        val weathers = arrayOf(
                            "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️",
                            "⛈️", "🌩️", "⚡", "❄️", "🌨️", "☃️", "⛄",
                            "🌬️", "💨", "🌪️", "🌫️", "☔", "💧", "💦", "🌈"
                        )
                        showEmojiPicker(weathers)
                    }
                    2 -> {
                        val realAnimals = arrayOf(
                            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
                            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
                            "🐧", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗",
                            "🐴", "🦄", "🐝", "🐛", "🦋", "🐞", "🐢", "🐙"
                        )
                        showEmojiPicker(realAnimals)
                    }
                    3 -> {
                        val realFoods = arrayOf(
                            "🍎", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🍍",
                            "🥝", "🍅", "🥑", "🍞", "🥐", "🥖", "🥨", "🧀",
                            "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕",
                            "🌮", "🌯", "🥙", "🥗", "🥪", "🥫", "🍝", "🍜",
                            "🍲", "🍛", "🍣", "🍱", "🥟", "🍚",
                            "🍙", "🍘", "🍡", "🍧", "🍨", "🍦",
                            "🎂", "🍮", "🍭", "🍬", "🍫", "🍩", "🍪",
                            "☕", "🍵", "🥤", "🧃", "🍺", "🍻", "🍷", "🍸", "🍹"
                        )
                        showEmojiPicker(realFoods)
                    }
                }
            }
            .show()
    }

    private fun showEmojiPicker(emojis: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("스티커를 선택하세요")
            .setItems(emojis) { _, which ->
                addStickerToOverlay(emojis[which])
            }
            .show()
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
}