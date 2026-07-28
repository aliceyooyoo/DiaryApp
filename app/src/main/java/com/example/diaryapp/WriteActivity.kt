package com.example.diaryapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteActivity : AppCompatActivity() {

    private val photoList = mutableListOf<Uri>()
    private lateinit var photoContainer: LinearLayout
    private lateinit var tvPlace: TextView
    private var representativeImageUri: Uri? = null

    // [고정형 스티커 변수] 선택된 스티커의 리소스 이름 (예: "sticker_smart_5")
    private var selectedStickerName: String = ""
    private lateinit var ivSelectedSticker: ImageView

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
                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                photoList.clear()
                photoList.addAll(uris)
                if (representativeImageUri == null || !photoList.contains(representativeImageUri)) {
                    representativeImageUri = photoList.firstOrNull()
                }
                updatePhotoListUI()
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

        // [수정] 제목 옆에 있는 스티커 ImageView 직접 연결
        ivSelectedSticker = findViewById(R.id.ivSelectedSticker)

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

            // 고정형 스티커 데이터 복원 (단일 스티커 이름 복원)
            val stickerStr = intent.getStringExtra("edit_sticker") ?: ""
            if (stickerStr.isNotEmpty()) {
                selectedStickerName = stickerStr.split(",")[0].split("@")[0]
                if (selectedStickerName.isNotEmpty()) {
                    val resId = resources.getIdentifier(selectedStickerName, "drawable", packageName)
                    if (resId != 0) {
                        ivSelectedSticker.setImageResource(resId)
                        ivSelectedSticker.visibility = View.VISIBLE
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

        // 스티커 버튼 클릭 시 다이얼로그 호출
        btnAddSticker.setOnClickListener {
            showSmartStickerDialog()
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

            // 고정된 스티커 이름 저장 (없으면 빈 문자열)
            val stickerString = selectedStickerName

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
                    selectedPlace
                )
                Toast.makeText(this, "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insertDiary(
                    date = today,
                    title = title,
                    content = content,
                    imageUri = imageUriString,
                    sticker = stickerString,
                    place = selectedPlace
                )
                Toast.makeText(this, "일기가 작성되었습니다.", Toast.LENGTH_SHORT).show()
            }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showSmartStickerDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val gridLayout = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1200
            )
            addView(gridLayout)
        }
        dialogView.addView(scrollView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("스티커를 선택하세요")
            .setView(dialogView)
            .create()

        for (i in 1..70) {
            val imageName = "sticker_smart_$i"
            val resId = resources.getIdentifier(imageName, "drawable", packageName)
            if (resId != 0) {
                val imgView = ImageView(this).apply {
                    setImageResource(resId)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 180
                        height = 180
                        setMargins(15, 15, 15, 15)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setOnClickListener {
                        // 스티커 선택 시 제목 옆 구석 슬롯에 즉시 반영
                        selectedStickerName = imageName
                        ivSelectedSticker.setImageResource(resId)
                        ivSelectedSticker.visibility = View.VISIBLE
                        dialog.dismiss()
                    }
                }
                gridLayout.addView(imgView)
            }
        }
        dialog.show()
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
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP

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