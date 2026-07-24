package com.example.diaryapp

data class Diary(
    val date: String,
    val title: String,
    val content: String,
    val weatherIcon: String? = null,
    val weatherTemp: String? = null,
    val weatherDesc: String? = null
)

data class WeatherResult(
    val temp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val humidity: Int,
    val description: String,
    val icon: String,
    val iconBitmap: android.graphics.Bitmap?
)