package com.example.diaryapp

data class KakaoResponse(
    val documents: List<Place>
)

data class Place(
    val place_name: String
)