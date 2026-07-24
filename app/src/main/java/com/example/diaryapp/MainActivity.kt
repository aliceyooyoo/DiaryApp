package com.example.diaryapp
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val apiKey = "894efd0493fa47be9bd9c09d27182253"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnWrite = findViewById<Button>(R.id.btnWrite)
        val btnCalendar = findViewById<Button>(R.id.btnCalendar)
        val btnMore = findViewById<TextView>(R.id.btnMore)

        btnWrite.setOnClickListener {
            startActivity(Intent(this, WriteActivity::class.java))
        }

        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        btnMore.setOnClickListener {
            startActivity(Intent(this, DiaryListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val sdf = SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
        val today = sdf.format(Date())
        tvGreeting.text = "$today\n오늘 하루는 어땠나요?"

        // TODO: 나중에 C가 만든 실제 GPS 좌표로 교체 (지금은 서울 좌표로 임시 고정)
        fetchWeather(37.5665, 126.9780) { result ->
            findViewById<TextView>(R.id.tvTemp).text = "${result.temp}°C"
            findViewById<TextView>(R.id.tvDescription).text = result.description
            findViewById<TextView>(R.id.tvTempMaxMin).text = "최고 ${result.tempMax}° / 최저 ${result.tempMin}°"
            findViewById<TextView>(R.id.tvHumidity).text = "${result.humidity}%"
            result.iconBitmap?.let {
                findViewById<ImageView>(R.id.ivWeatherIcon).setImageBitmap(it)
            }
        }

        val container = findViewById<LinearLayout>(R.id.recentDiaryContainer)
        container.removeAllViews()

        val dbHelper = DiaryDbHelper(this)
        val diaryList = dbHelper.getAllDiaries()
        val previewList = diaryList.take(3)
        for (diary in previewList) {
            container.addView(createDiaryCard(diary))
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

    private fun createDiaryCard(diary: Diary): LinearLayout {
        val dp = resources.displayMetrics.density

        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, (12 * dp).toInt())
        card.layoutParams = cardParams
        card.setBackgroundColor(Color.WHITE)

        val photoView = ImageView(this)
        val photoSize = (72 * dp).toInt()
        val photoParams = LinearLayout.LayoutParams(photoSize, photoSize)
        photoParams.setMargins(0, 0, (16 * dp).toInt(), 0)
        photoView.layoutParams = photoParams
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP

        if (!diary.imageUri.isNullOrEmpty()) {
            try {
                photoView.setImageURI(Uri.parse(diary.imageUri))
            } catch (e: Exception) {
                photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
            }
        } else {
            photoView.setBackgroundColor(Color.parseColor("#D9D9D9"))
        }

        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL

        val date = TextView(this)
        date.text = diary.date
        date.textSize = 12f
        date.setTextColor(Color.parseColor("#888888"))

        val title = TextView(this)
        title.text = diary.title
        title.textSize = 16f
        title.setTypeface(null, Typeface.BOLD)

        val content = TextView(this)
        content.text = diary.content
        content.textSize = 13f
        content.setTextColor(Color.parseColor("#555555"))
        content.maxLines = 2

        val weather = TextView(this)
        weather.text = if (!diary.weatherTemp.isNullOrEmpty()) "${diary.weatherTemp} ${diary.weatherDesc ?: ""}" else ""
        weather.textSize = 11f
        weather.setTextColor(Color.parseColor("#999999"))

        right.addView(date)
        right.addView(title)
        right.addView(content)
        right.addView(weather)

        card.addView(photoView)
        card.addView(right)

        return card
    }
}

data class WeatherResult(
    val temp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val humidity: Int,
    val description: String,
    val icon: String,
    val iconBitmap: Bitmap?
)