package com.example.diaryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

class DiaryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "diary.db", null, 10) {

    private val appContext = context.applicationContext

    init {
        copyDatabaseFromAssetsIfNeeded(context)
        copyImagesFromAssetsIfNeeded(context)
    }

    private fun copyDatabaseFromAssetsIfNeeded(context: Context) {
        val dbFile = context.getDatabasePath("diary.db")
        if (dbFile.exists()) return

        try {
            dbFile.parentFile?.mkdirs()
            context.assets.open("diary.db").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyImagesFromAssetsIfNeeded(context: Context) {
        try {
            val assetFiles = context.assets.list("") ?: return
            val imageFiles = assetFiles.filter {
                it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png")
            }

            for (fileName in imageFiles) {
                val destFile = File(context.filesDir, fileName)
                if (destFile.exists()) continue

                context.assets.open(fileName).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 파일명만 저장된 옛날 데이터(image1.jpg 등)는 내부 저장소 절대경로로 변환
    // 이미 file:// 또는 content:// 형식이면 그대로 둠 (새로 작성한 일기)
    private fun resolveImageUri(rawUri: String): String {
        if (rawUri.isEmpty()) return ""
        if (rawUri.startsWith("file://") || rawUri.startsWith("content://")) {
            return rawUri
        }
        // 파일명만 있는 경우 (예: image1.jpg)
        val file = File(appContext.filesDir, rawUri)
        return "file://${file.absolutePath}"
    }

    private fun resolveImageUriList(rawImageUri: String?): String {
        if (rawImageUri.isNullOrEmpty()) return ""
        return rawImageUri.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(",") { resolveImageUri(it) }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE diary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                title TEXT,
                content TEXT,
                imageUri TEXT,
                sticker TEXT,
                place TEXT,
                weatherIcon TEXT,
                weatherTemp TEXT,
                weatherDesc TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS diary")
        onCreate(db)
    }

    fun insertDiary(
        date: String, title: String, content: String,
        imageUri: String, sticker: String, place: String,
        weatherIcon: String? = null, weatherTemp: String? = null, weatherDesc: String? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("title", title)
            put("content", content)
            put("imageUri", imageUri)
            put("sticker", sticker)
            put("place", place)
            put("weatherIcon", weatherIcon)
            put("weatherTemp", weatherTemp)
            put("weatherDesc", weatherDesc)
        }
        db.insert("diary", null, values)
        db.close()
    }

    fun updateDiary(
        oldTitle: String, oldDate: String,
        newTitle: String, newContent: String,
        newImageUri: String, newSticker: String, newPlace: String,
        weatherIcon: String? = null, weatherTemp: String? = null, weatherDesc: String? = null
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", newTitle)
            put("content", newContent)
            put("imageUri", newImageUri)
            put("sticker", newSticker)
            put("place", newPlace)
            if (weatherIcon != null) put("weatherIcon", weatherIcon)
            if (weatherTemp != null) put("weatherTemp", weatherTemp)
            if (weatherDesc != null) put("weatherDesc", weatherDesc)
        }
        db.update("diary", values, "title = ? AND date = ?", arrayOf(oldTitle, oldDate))
        db.close()
    }

    fun deleteDiary(title: String, date: String) {
        val db = writableDatabase
        db.delete("diary", "title = ? AND date = ?", arrayOf(title, date))
        db.close()
    }

    fun getAllDiaries(): List<Diary> {
        val list = mutableListOf<Diary>()
        val db = readableDatabase
        val cursor = db.query("diary", null, null, null, null, null, "id DESC")
        while (cursor.moveToNext()) {
            list.add(cursorToDiary(cursor))
        }
        cursor.close()
        db.close()
        return list
    }

    fun getDiariesByDate(date: String): List<Diary> {
        val list = mutableListOf<Diary>()
        val db = readableDatabase
        val cursor = db.query("diary", null, "date = ?", arrayOf(date), null, null, "id DESC")
        while (cursor.moveToNext()) {
            list.add(cursorToDiary(cursor))
        }
        cursor.close()
        db.close()
        return list
    }

    private fun cursorToDiary(cursor: android.database.Cursor): Diary {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        val date = cursor.getString(cursor.getColumnIndexOrThrow("date")) ?: ""
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: ""
        val content = cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: ""
        val rawImageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")) ?: ""
        val imageUri = resolveImageUriList(rawImageUri)
        val sticker = cursor.getString(cursor.getColumnIndexOrThrow("sticker")) ?: ""
        val place = cursor.getString(cursor.getColumnIndexOrThrow("place")) ?: ""
        val weatherIcon = cursor.getString(cursor.getColumnIndexOrThrow("weatherIcon"))
        val weatherTemp = cursor.getString(cursor.getColumnIndexOrThrow("weatherTemp"))
        val weatherDesc = cursor.getString(cursor.getColumnIndexOrThrow("weatherDesc"))
        return Diary(id, date, title, content, imageUri, sticker, place, weatherIcon, weatherTemp, weatherDesc)
    }
}