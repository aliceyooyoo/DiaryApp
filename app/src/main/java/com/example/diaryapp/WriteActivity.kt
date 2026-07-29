package com.example.diaryapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlacedSticker(val emoji: String, var x: Float, var y: Float)

class WriteActivity : AppCompatActivity() {

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var currentWeather: WeatherResult? = null

    private val photoList = mutableListOf<Uri>()
    private lateinit var photoContainer: LinearLayout
    private lateinit var tvPlace: TextView
    private var representativeImageUri: Uri? = null

    private lateinit var stickerCanvas: FrameLayout
    private val placedStickers = mutableListOf<PlacedSticker>()

    private var isEditMode = false
    private var originalTitle = ""
    private var originalDate = ""
    private var selectedPlace = ""

    private val placeLauncher =
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
                val copiedUris = uris.mapNotNull { copyImageToInternalStorage(it) }

                photoList.clear()
                photoList.addAll(copiedUris)
                if (representativeImageUri == null || !photoList.contains(representativeImageUri)) {
                    representativeImageUri = photoList.firstOrNull()
                }
                updatePhotoListUI()
            }
        }

    private fun copyImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "diary_img_${System.currentTimeMillis()}_${(0..999).random()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val btnClose = findViewById<TextView>(R.id.btnClose)

        val btnAddPhoto = findViewById<LinearLayout>(R.id.btnAddPhoto)
        val btnAddSticker = findViewById<LinearLayout>(R.id.btnAddSticker)
        val btnLocation = findViewById<LinearLayout>(R.id.btnLocation)

        tvPlace = findViewById(R.id.tvPlace)
        photoContainer = findViewById(R.id.photoContainer)
        stickerCanvas = findViewById(R.id.stickerCanvas)

        // 날씨 조회
        // TODO: 나중에 C가 만든 실제 GPS 좌표로 교체 (지금은 서울 좌표로 임시 고정)
        fetchWeather(37.5665, 126.9780) { result ->
            currentWeather = result
            findViewById<TextView>(R.id.tvWriteWeatherTemp).text = "${result.temp}°C"
            findViewById<TextView>(R.id.tvWriteWeatherDesc).text = result.description
            result.iconBitmap?.let {
                findViewById<ImageView>(R.id.ivWriteWeatherIcon).setImageBitmap(it)
            }
        }

        // 수정 모드 진입 시 기존 데이터 채우기
        if (intent.hasExtra("edit_title")) {
            isEditMode = true
            originalTitle = intent.getStringExtra("edit_title") ?: ""
            originalDate = intent.getStringExtra("edit_date") ?: ""

            etTitle.setText(originalTitle)
            etContent.setText(intent.getStringExtra("edit_content") ?: "")

            val editPlace = intent.getStringExtra("edit_place") ?: ""
            if (editPlace.isNotEmpty()) {
                selectedPlace = editPlace
                tvPlace.text = "📍 $selectedPlace"
            }

            // 좌표 기반 스티커 복원: "resName:x:y,resName:x:y"
            val stickerStr = intent.getStringExtra("edit_sticker") ?: ""
            if (stickerStr.isNotEmpty()) {
                stickerStr.split(",").forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 3) {
                        val emoji = parts[0]
                        val x = parts[1].toFloatOrNull() ?: 100f
                        val y = parts[2].toFloatOrNull() ?: 100f
                        val placed = PlacedSticker(emoji, x, y)
                        placedStickers.add(placed)
                        drawStickerView(placed)
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

        btnClose.setOnClickListener {
            finish()
        }

        btnAddPhoto.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddSticker.setOnClickListener {
            showEmojiCategoryDialog()
        }

        btnLocation.setOnClickListener {
            val intent = Intent(this, PlaceSearchActivity::class.java)
            placeLauncher.launch(intent)
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "제목이나 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 좌표 기반 스티커 문자열로 변환
            val stickerString = placedStickers.joinToString(",") { "${it.emoji}:${it.x}:${it.y}" }

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
                    stickerString,
                    selectedPlace,
                    currentWeather?.icon,
                    currentWeather?.let { "${it.temp}°C" },
                    currentWeather?.description
                )
                Toast.makeText(this, "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insertDiary(
                    date = today,
                    title = title,
                    content = content,
                    imageUri = imageUriString,
                    sticker = stickerString,
                    place = selectedPlace,
                    weatherIcon = currentWeather?.icon,
                    weatherTemp = currentWeather?.let { "${it.temp}°C" },
                    weatherDesc = currentWeather?.description
                )
                Toast.makeText(this, "일기가 작성되었습니다.", Toast.LENGTH_SHORT).show()
            }

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

    // 스티커 다이얼로그에서 선택하면 캔버스에 새로 추가
    private fun addStickerToCanvas(emoji: String) {
        val placed = PlacedSticker(emoji, 100f, 100f)
        placedStickers.add(placed)
        drawStickerView(placed)
    }

    // 스티커 하나를 캔버스에 그리고 드래그/삭제 리스너 연결
    private fun drawStickerView(placed: PlacedSticker) {
        val stickerView = TextView(this).apply {
            text = placed.emoji
            textSize = 32f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            x = placed.x
            y = placed.y
        }

        var dX = 0f
        var dY = 0f

        stickerView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX + dX
                    view.y = event.rawY + dY
                    placed.x = view.x
                    placed.y = view.y
                    true
                }
                else -> false
            }
        }

        stickerView.setOnLongClickListener {
            placedStickers.remove(placed)
            stickerCanvas.removeView(stickerView)
            Toast.makeText(this, "스티커가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            true
        }

        stickerCanvas.addView(stickerView)
    }

    private fun showEmojiCategoryDialog() {
        val categories = arrayOf("😀 감정 / 표정", "☀️ 날씨 / 자연", "🎉 일상 / 활동", "🍔 음식 / 카페", "❤️ 하트 / 기타")

        AlertDialog.Builder(this)
            .setTitle("스티커 카테고리 선택")
            .setItems(categories) { _, which ->
                showEmojiListDialog(which)
            }
            .show()
    }

    private fun showEmojiListDialog(categoryIndex: Int) {
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
                addStickerToCanvas(selectedList[which])
            }
            .setNegativeButton("뒤로가기") { _, _ ->
                showEmojiCategoryDialog()
            }
            .show()
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
                scaleType = ImageView.ScaleType.CENTER_CROP

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
                    gravity = Gravity.TOP or Gravity.END
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
}