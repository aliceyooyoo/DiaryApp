package com.example.diaryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DiaryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "diary.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE diary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                title TEXT,
                content TEXT,
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

    fun insertDiary(date: String, title: String, content: String, weatherIcon: String?, weatherTemp: String?, weatherDesc: String?) {
        val db = writableDatabase
        val values = ContentValues()
        values.put("date", date)
        values.put("title", title)
        values.put("content", content)
        values.put("weatherIcon", weatherIcon)
        values.put("weatherTemp", weatherTemp)
        values.put("weatherDesc", weatherDesc)
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
            val weatherIcon = cursor.getString(cursor.getColumnIndexOrThrow("weatherIcon"))
            val weatherTemp = cursor.getString(cursor.getColumnIndexOrThrow("weatherTemp"))
            val weatherDesc = cursor.getString(cursor.getColumnIndexOrThrow("weatherDesc"))
            list.add(Diary(date, title, content, weatherIcon, weatherTemp, weatherDesc))
        }
        cursor.close()
        db.close()
        return list
    }
}