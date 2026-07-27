package com.example.diaryapp

data class Diary(
    val id: Int = 0,
    val date: String,
    val title: String,
    val content: String,
    val imageUri: String,
    val sticker: String,
    val place: String,
    val weatherTemp: String? = null,
    val weatherDesc: String? = null
)