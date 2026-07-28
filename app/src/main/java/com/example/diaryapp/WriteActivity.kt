package com.example.diaryapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import android.view.Gravity

class WriteActivity : AppCompatActivity() {

    private val photoList = mutableListOf<Uri>()
    private lateinit var photoContainer: LinearLayout
    private lateinit var tvPlace: TextView
    private lateinit var stickerCanvasContainer: FrameLayout
    private var representativeImageUri: Uri? = null

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

        // 🚨 수정된 부분: XML 레이아웃의 실제 타입인 LinearLayout으로 변경
        val btnAddPhoto = findViewById<LinearLayout>(R.id.btnAddPhoto)
        val btnAddSticker = findViewById<LinearLayout>(R.id.btnAddSticker)
        val btnLocation = findViewById<LinearLayout>(R.id.btnLocation)

        tvPlace = findViewById(R.id.tvPlace)
        photoContainer = findViewById(R.id.photoContainer)
        stickerCanvasContainer = findViewById(R.id.stickerCanvasContainer)

        // 수정 모드 진입 시 기존 데이터 채우기
        if (intent.hasExtra("edit_title")) {
            isEditMode = true
            originalTitle = intent.getStringExtra("edit_title") ?: ""
            originalDate = intent.getStringExtra("edit_date") ?: ""

            etTitle.setText(originalTitle)
            etContent.setText(intent.getStringExtra("edit_content") ?: "")

            // [추가] 장소 복원
            val editPlace = intent.getStringExtra("edit_place") ?: ""
            if (editPlace.isNotEmpty()) {
                selectedPlace = editPlace
                tvPlace.text = "📍 $selectedPlace"
            }

            // 스마트 스티커 데이터 복원 (형식: 이름@X@Y@Scale@Rotation)
            val stickerStr = intent.getStringExtra("edit_sticker") ?: ""
            if (stickerStr.isNotEmpty()) {
                val stickers = stickerStr.split(",")
                for (stk in stickers) {
                    val parts = stk.split("@")
                    if (parts.size >= 5) {
                        val imageName = parts[0]
                        val x = parts[1].toFloatOrNull() ?: 200f
                        val y = parts[2].toFloatOrNull() ?: 200f
                        val scale = parts[3].toFloatOrNull() ?: 1f
                        val rot = parts[4].toFloatOrNull() ?: 0f

                        val resId = resources.getIdentifier(imageName, "drawable", packageName)
                        if (resId != 0) {
                            addStickerToCanvas(imageName, resId, x, y, scale, rot)
                        }
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

        // 스티커 버튼 클릭 시 1~70번 스마트 스티커 다이얼로그 호출
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

            // 화면에 배치된 모든 스티커들의 위치, 크기, 회전값 직렬화
            val stickerDataList = mutableListOf<String>()
            for (i in 0 until stickerCanvasContainer.childCount) {
                val child = stickerCanvasContainer.getChildAt(i)
                if (child is StickerView) {
                    val data = "${child.imageName}@${child.x}@${child.y}@${child.scaleX}@${child.rotation}"
                    stickerDataList.add(data)
                }
            }
            val stickerString = stickerDataList.joinToString(",")

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
                    selectedPlace // [추가]
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

        // sticker_smart_1 ~ sticker_smart_70 동적 로드
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
                        addStickerToCanvas(imageName, resId)
                        dialog.dismiss()
                    }
                }
                gridLayout.addView(imgView)
            }
        }
        dialog.show()
    }

    private fun addStickerToCanvas(
        imageName: String,
        resId: Int,
        initialX: Float = 200f,
        initialY: Float = 200f,
        scale: Float = 1f,
        rot: Float = 0f
    ) {
        val stickerView = StickerView(this, imageName, resId).apply {
            x = initialX
            y = initialY
            scaleX = scale
            scaleY = scale
            rotation = rot
        }

        stickerCanvasContainer.addView(stickerView)
        stickerView.invalidate()
        stickerView.requestLayout()
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