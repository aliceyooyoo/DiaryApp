package com.example.diaryapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApi {

    @GET("v2/local/search/keyword.json")
    fun searchPlace(
        @Header("Authorization") key: String,
        @Query("query") query: String
    ): Call<KakaoResponse>
}