package com.example.diaryapp

data class Diary(
    val date: String,
    val title: String,
    val content: String,
    val imageUri: String,
    val sticker: String,
    val place: String
)