package com.example.diaryapp

data class Diary(
    val date: String,
    val title: String,
    val content: String,
    val weatherIcon: String? = null,
    val weatherTemp: String? = null,
    val weatherDesc: String? = null,
    val imageUri: String? = null,
    val stickerData: String? = null
)