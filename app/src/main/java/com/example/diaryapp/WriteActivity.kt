package com.example.diaryapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private val apiKey = "894efd0493fa47be9bd9c09d27182253"
    private var currentWeather: WeatherResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val btnDone = findViewById<TextView>(R.id.btnDone)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)

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

        btnDone.setOnClickListener {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()
            val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
            val today = sdf.format(Date())

            val dbHelper = DiaryDbHelper(this)
            dbHelper.insertDiary(
                today, title, content,
                currentWeather?.icon,
                currentWeather?.let { "${it.temp}°C" },
                currentWeather?.description
            )

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
}