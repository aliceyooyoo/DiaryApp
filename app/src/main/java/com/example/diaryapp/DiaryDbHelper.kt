package com.example.diaryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DiaryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "diary.db", null, 6) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE diary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                title TEXT,
                content TEXT,
                imageUri TEXT,
                sticker TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS diary")
        onCreate(db)
    }

    fun insertDiary(date: String, title: String, content: String, imageUri: String, sticker: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("title", title)
            put("content", content)
            put("imageUri", imageUri)
            put("sticker", sticker)
        }
        db.insert("diary", null, values)
        db.close()
    }

    // 기존 일기 수정을 위한 함수 추가
    fun updateDiary(oldTitle: String, oldDate: String, newTitle: String, newContent: String, newImageUri: String, newSticker: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", newTitle)
            put("content", newContent)
            put("imageUri", newImageUri)
            put("sticker", newSticker)
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
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
            val stickerIndex = cursor.getColumnIndex("sticker")
            val sticker = if (stickerIndex != -1) cursor.getString(stickerIndex) ?: "" else ""

            list.add(Diary(date, title, content, imageUri, sticker))
        }
        cursor.close()
        db.close()
        return list
    }
}