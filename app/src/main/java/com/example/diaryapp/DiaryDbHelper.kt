package com.example.diaryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.diaryapp.Diary

class DiaryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "diary.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE diary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                title TEXT,
                content TEXT,
                imageUri TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS diary")
        onCreate(db)
    }

    fun insertDiary(date: String, title: String, content: String, imageUri: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("title", title)
            put("content", content)
            put("imageUri", imageUri)
        }
        db.insert("diary", null, values)
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

            // imageUri까지 포함하여 Diary 객체 생성
            list.add(Diary(date, title, content, imageUri))
        }
        cursor.close()
        db.close()
        return list
    }
}