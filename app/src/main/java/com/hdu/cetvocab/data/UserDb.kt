package com.hdu.cetvocab.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 用户本地数据：学习进度、答题记录、闯关成绩、成就解锁。
 * 全部保存在设备私有目录，不上传任何服务器。
 */
class UserDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE word_state(" +
                "word_id INTEGER PRIMARY KEY," +
                "level INTEGER NOT NULL DEFAULT 0," +
                "wrong_count INTEGER NOT NULL DEFAULT 0," +
                "fav INTEGER NOT NULL DEFAULT 0," +
                "last_ts INTEGER NOT NULL DEFAULT 0," +
                "next_review_ts INTEGER NOT NULL DEFAULT 0," +
                "ease REAL NOT NULL DEFAULT 2.5," +
                "reps INTEGER NOT NULL DEFAULT 0," +
                "interval_days INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE study_log(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "word_id INTEGER NOT NULL," +
                "correct INTEGER NOT NULL," +
                "day TEXT NOT NULL," +
                "ts INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_log_day ON study_log(day)")
        db.execSQL(
            "CREATE TABLE quiz_record(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "stage INTEGER NOT NULL," +
                "unit INTEGER NOT NULL," +
                "mode TEXT NOT NULL," +
                "score INTEGER NOT NULL," +
                "total INTEGER NOT NULL," +
                "correct INTEGER NOT NULL," +
                "max_combo INTEGER NOT NULL," +
                "duration_ms INTEGER NOT NULL," +
                "ts INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE achievement(code TEXT PRIMARY KEY, unlocked_ts INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE app_meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v1 -> v2：word_state 增加 SM-2 间隔重复字段，用 ALTER 保留用户已有学习数据
            db.execSQL("ALTER TABLE word_state ADD COLUMN ease REAL NOT NULL DEFAULT 2.5")
            db.execSQL("ALTER TABLE word_state ADD COLUMN reps INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE word_state ADD COLUMN interval_days INTEGER NOT NULL DEFAULT 0")
        } else {
            // 其它版本跨度无法逐列迁移，回退为重建空表
            db.execSQL("DROP TABLE IF EXISTS word_state")
            db.execSQL("DROP TABLE IF EXISTS study_log")
            db.execSQL("DROP TABLE IF EXISTS quiz_record")
            db.execSQL("DROP TABLE IF EXISTS achievement")
            db.execSQL("DROP TABLE IF EXISTS app_meta")
            onCreate(db)
        }
    }

    // ---------- 单词状态 ----------

    fun states(): Map<Int, WordState> {
        val map = HashMap<Int, WordState>()
        readableDatabase.rawQuery(
            "SELECT word_id,level,wrong_count,fav,last_ts,next_review_ts," +
                "ease,reps,interval_days FROM word_state", null
        ).use { c ->
            while (c.moveToNext()) {
                map[c.getInt(0)] = WordState(
                    wordId = c.getInt(0),
                    level = c.getInt(1),
                    wrongCount = c.getInt(2),
                    fav = c.getInt(3) == 1,
                    lastTs = c.getLong(4),
                    nextReviewTs = c.getLong(5),
                    ease = c.getDouble(6),
                    reps = c.getInt(7),
                    intervalDays = c.getInt(8)
                )
            }
        }
        return map
    }

    fun stateOf(wordId: Int): WordState =
        states()[wordId] ?: WordState(wordId)

    fun markLearned(
        wordId: Int, level: Int, nextReviewTs: Long,
        ease: Double, reps: Int, intervalDays: Int, correct: Boolean
    ) {
        val now = System.currentTimeMillis()
        val existing = stateOf(wordId)
        // 只有答错才累加错误次数。注意不能用 level 判断对错：首次答对时 level 同样是 1，
        // 若按 level <= 1 记错，会把所有新学且答对的词都误计入错词本。
        val wrong = if (correct) existing.wrongCount else existing.wrongCount + 1
        val cv = ContentValues().apply {
            put("word_id", wordId)
            put("level", level)
            put("wrong_count", wrong)
            put("fav", if (existing.fav) 1 else 0)
            put("last_ts", now)
            put("next_review_ts", nextReviewTs)
            put("ease", ease)
            put("reps", reps)
            put("interval_days", intervalDays)
        }
        writableDatabase.insertWithOnConflict(
            "word_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun setFav(wordId: Int, fav: Boolean) {
        val existing = stateOf(wordId)
        val cv = ContentValues().apply {
            put("word_id", wordId)
            put("level", existing.level)
            put("wrong_count", existing.wrongCount)
            put("fav", if (fav) 1 else 0)
            put("last_ts", existing.lastTs)
            put("next_review_ts", existing.nextReviewTs)
            put("ease", existing.ease)
            put("reps", existing.reps)
            put("interval_days", existing.intervalDays)
        }
        writableDatabase.insertWithOnConflict(
            "word_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ---------- 学习日志 ----------

    fun logStudy(wordId: Int, correct: Boolean) {
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("word_id", wordId)
            put("correct", if (correct) 1 else 0)
            put("day", today())
            put("ts", now)
        }
        writableDatabase.insert("study_log", null, cv)
    }

    fun todayLearnedCount(): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(DISTINCT word_id) FROM study_log WHERE day=?", arrayOf(today())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun distinctWordCount(): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(DISTINCT word_id) FROM study_log", null
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun correctWrongTotals(): Pair<Int, Int> =
        readableDatabase.rawQuery(
            "SELECT SUM(correct), COUNT(*)-SUM(correct) FROM study_log", null
        ).use {
            if (it.moveToFirst()) (it.getInt(0)) to (it.getInt(1)) else 0 to 0
        }

    fun studyDays(): List<String> {
        val days = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT day FROM study_log ORDER BY day DESC", null
        ).use { c -> while (c.moveToNext()) days.add(c.getString(0)) }
        return days
    }

    fun recentDays(n: Int): List<DayStat> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val from = today.minusDays((n - 1).toLong()).format(fmt)
        val raw = HashMap<String, DayStat>()
        readableDatabase.rawQuery(
            "SELECT day, COUNT(DISTINCT word_id), SUM(correct), COUNT(*)-SUM(correct) " +
                "FROM study_log WHERE day>=? GROUP BY day", arrayOf(from)
        ).use { c ->
            while (c.moveToNext()) {
                raw[c.getString(0)] = DayStat(c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3))
            }
        }
        return (0 until n).map { i ->
            val d = today.minusDays((n - 1 - i).toLong()).format(fmt)
            raw[d] ?: DayStat(d, 0, 0, 0)
        }
    }

    // ---------- 闯关成绩 ----------

    fun addQuizRecord(record: QuizRecord): Long {
        val cv = ContentValues().apply {
            put("stage", record.stage)
            put("unit", record.unit)
            put("mode", record.mode)
            put("score", record.score)
            put("total", record.total)
            put("correct", record.correct)
            put("max_combo", record.maxCombo)
            put("duration_ms", record.durationMs)
            put("ts", record.ts)
        }
        return writableDatabase.insert("quiz_record", null, cv)
    }

    fun quizRecords(limit: Int = 100): List<QuizRecord> {
        val list = ArrayList<QuizRecord>()
        readableDatabase.rawQuery(
            "SELECT id,stage,unit,mode,score,total,correct,max_combo,duration_ms,ts " +
                "FROM quiz_record ORDER BY score DESC, ts DESC LIMIT $limit", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    QuizRecord(
                        c.getLong(0), c.getInt(1), c.getInt(2), c.getString(3) ?: "",
                        c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getLong(8), c.getLong(9)
                    )
                )
            }
        }
        return list
    }

    fun recentQuizRecords(limit: Int = 20): List<QuizRecord> {
        val list = ArrayList<QuizRecord>()
        readableDatabase.rawQuery(
            "SELECT id,stage,unit,mode,score,total,correct,max_combo,duration_ms,ts " +
                "FROM quiz_record ORDER BY ts DESC LIMIT $limit", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    QuizRecord(
                        c.getLong(0), c.getInt(1), c.getInt(2), c.getString(3) ?: "",
                        c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getLong(8), c.getLong(9)
                    )
                )
            }
        }
        return list
    }

    fun bestScore(): Int =
        readableDatabase.rawQuery("SELECT MAX(score) FROM quiz_record", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    fun quizPlayedCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM quiz_record", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    fun quizTotalMs(): Long =
        readableDatabase.rawQuery("SELECT SUM(duration_ms) FROM quiz_record", null).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }

    // ---------- 成就 ----------

    fun achievements(): Map<String, Long> {
        val map = HashMap<String, Long>()
        readableDatabase.rawQuery("SELECT code,unlocked_ts FROM achievement", null).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        return map
    }

    fun unlock(code: String): Boolean {
        if (achievements().containsKey(code)) return false
        val cv = ContentValues().apply {
            put("code", code)
            put("unlocked_ts", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "achievement", null, cv, SQLiteDatabase.CONFLICT_IGNORE
        )
        return true
    }

    // ---------- 通用 meta ----------

    fun getMeta(key: String, def: String = "0"): String =
        readableDatabase.rawQuery("SELECT v FROM app_meta WHERE k=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) ?: def else def
        }

    fun setMeta(key: String, value: String) {
        val cv = ContentValues().apply {
            put("k", key)
            put("v", value)
        }
        writableDatabase.insertWithOnConflict(
            "app_meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun addStudySeconds(seconds: Long) {
        if (seconds <= 0) return
        val cur = getMeta("study_seconds", "0").toLongOrNull() ?: 0L
        setMeta("study_seconds", (cur + seconds).toString())
    }

    fun studySeconds(): Long = getMeta("study_seconds", "0").toLongOrNull() ?: 0L

    companion object {
        const val DB_NAME = "user_data.db"
        const val DB_VERSION = 2

        fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}
