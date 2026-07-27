package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.ListView
import retrofit2.Retrofit
import android.location.Geocoder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class PlaceSearchActivity : AppCompatActivity() {
    private lateinit var placeList: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var tvCurrentLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private fun searchPlace(keyword: String) {
        val api = RetrofitClient.kakaoApi

        api.searchPlace(
            "KakaoAK fc5afabd2d27a7cb6c05aba36ce8b028",
            keyword
        ).enqueue(object : Callback<KakaoResponse> {

            override fun onResponse(
                call: Call<KakaoResponse>,
                response: Response<KakaoResponse>
            ) {
                println("응답 받음")
                println("성공 여부 = ${response.isSuccessful}")
                println("응답 코드 = ${response.code()}")
                println("에러 내용 = ${response.errorBody()?.string()}")
                if (response.isSuccessful) {

                    val documents = response.body()?.documents

                    println("documents = $documents")
                    println("결과 개수 = ${documents?.size}")
                    placeList.clear()

                    documents?.forEach {
                        println("장소명 = ${it.place_name}")
                        placeList.add(it.place_name)
                    }
                    println("리스트 개수 = ${placeList.size}")
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(
                call: Call<KakaoResponse>,
                t: Throwable
            ) {

            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_place_search)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        placeList = ArrayList()

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            placeList
        )
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val listPlace = findViewById<ListView>(R.id.listPlace)
        val btnCurrentLocation = findViewById<Button>(R.id.btnCurrentLocation)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)

        btnCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }

        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        listPlace.adapter = adapter

        listPlace.setOnItemClickListener {_, _, position, _ ->
            val selectedPlace = placeList[position]

            val intent = Intent()
            intent.putExtra("place", selectedPlace)

            setResult(RESULT_OK, intent)
            finish()
        }

        btnConfirm.setOnClickListener {
            val place = etSearch.text.toString()
            tvCurrentLocation.text = place

        }

        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString()

            println("$keyword 검색 결과")
            searchPlace(keyword)

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    } private fun getCurrentLocation() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                100
            )

            return
        }


        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->

                if(location != null){

                    val geocoder = Geocoder(this, Locale.KOREA)

                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )

                    val place = if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0)
                    } else {
                        "현재 위치"
                    }

                    tvCurrentLocation.text = "\uD83D\uDCCD $place"

                    val intent = Intent()

                    intent.putExtra(
                        "place",
                        place
                    )

                    setResult(
                        RESULT_OK,
                        intent
                    )

                    finish()

                }


            }
    }

    }

