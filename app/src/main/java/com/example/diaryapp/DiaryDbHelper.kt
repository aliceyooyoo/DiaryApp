import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.diaryapp.Diary

class DiaryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "diary.db", null, 2) { // DB 버전을 2로 변경하여 imageUri 컬럼 적용

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

    // 일기 저장 (imageUri 매개변수 추가, 기본값 ""으로 사진이 없어도 저장 가능)
    fun insertDiary(date: String, title: String, content: String, imageUri: String = "") {
        val db = writableDatabase
        val values = ContentValues()
        values.put("date", date)
        values.put("title", title)
        values.put("content", content)
        values.put("imageUri", imageUri)
        db.insert("diary", null, values)
        db.close()
    }

    // 전체 일기 조회
    fun getAllDiaries(): List<Diary> {
        val list = mutableListOf<Diary>()
        val db = readableDatabase
        val cursor = db.query("diary", null, null, null, null, null, "id DESC")
        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))

            // imageUri 컬럼 읽기
            val imageUriIndex = cursor.getColumnIndex("imageUri")
            val imageUri = if (imageUriIndex != -1) cursor.getString(imageUriIndex) ?: "" else ""

            list.add(Diary(date, title, content, imageUri))
        }
        cursor.close()
        db.close()
        return list
    }
}