package com.hdu.cetvocab.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 学习数据的导出与备份/恢复。
 *
 * 所有文件都写入应用外部私有目录（getExternalFilesDir），属于应用自己的存储空间，
 * 因此不需要申请任何存储权限；备份内容仅包含用户学习数据，不含内置只读词库。
 */
class BackupManager(
    private val context: Context,
    private val userDb: UserDb,
    private val wordDb: WordDb
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vocab_meta", Context.MODE_PRIVATE)

    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
    private val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun exportDir(): File = File(baseDir(), "导出").apply { mkdirs() }

    private fun backupDir(): File = File(baseDir(), "备份").apply { mkdirs() }

    /** 优先用应用外部私有目录；极端情况下（无外部存储）退回内部私有目录 */
    private fun baseDir(): File = context.getExternalFilesDir(null) ?: context.filesDir

    // ---------------- 导出 CSV ----------------

    /** 导出全部学习记录，一行一个单词状态 */
    fun exportStudyCsv(): File {
        val file = File(exportDir(), "学习记录_${fileStamp.format(Date())}.csv")
        val sb = StringBuilder()
        sb.append("word,level,wrong_count,fav,last_study,next_review\n")
        userDb.readableDatabase.rawQuery(
            "SELECT word_id,level,wrong_count,fav,last_ts,next_review_ts " +
                "FROM word_state ORDER BY last_ts DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                val word = wordDb.wordById(c.getInt(0))?.word ?: "#${c.getInt(0)}"
                sb.append(csvCell(word)).append(',')
                    .append(c.getInt(1)).append(',')
                    .append(c.getInt(2)).append(',')
                    .append(c.getInt(3)).append(',')
                    .append(csvCell(formatTs(c.getLong(4)))).append(',')
                    .append(csvCell(formatTs(c.getLong(5)))).append('\n')
            }
        }
        file.writeText(sb.toString())
        return file
    }

    /** 导出收藏的单词本，按字母序 */
    fun exportFavCsv(): File {
        val file = File(exportDir(), "单词本_${fileStamp.format(Date())}.csv")
        val favIds = ArrayList<Int>()
        userDb.readableDatabase.rawQuery(
            "SELECT word_id FROM word_state WHERE fav=1", null
        ).use { c -> while (c.moveToNext()) favIds.add(c.getInt(0)) }
        val words = wordDb.wordsByIds(favIds).sortedBy { it.word.lowercase(Locale.ROOT) }
        val sb = StringBuilder()
        sb.append("word,phonetic,translation,stage,unit\n")
        for (w in words) {
            sb.append(csvCell(w.word)).append(',')
                .append(csvCell(w.phonetic)).append(',')
                .append(csvCell(w.translation)).append(',')
                .append(csvCell(WordDb.stageLabel(w.stage))).append(',')
                .append(w.unit).append('\n')
        }
        file.writeText(sb.toString())
        return file
    }

    // ---------------- 备份 JSON ----------------

    /** 备份全部用户数据为单个 JSON 文件 */
    fun backupJson(): File {
        val file = File(backupDir(), "cetvocab_backup_${fileStamp.format(Date())}.json")
        val root = JSONObject()
        root.put("app", "过四斩六")
        root.put("db_version", UserDb.DB_VERSION)
        root.put("exported_at", System.currentTimeMillis())

        root.put(
            "word_state",
            queryArray(
                "SELECT word_id,level,wrong_count,fav,last_ts,next_review_ts," +
                    "ease,reps,interval_days FROM word_state"
            ) { c ->
                JSONObject().apply {
                    put("word_id", c.getInt(0))
                    put("level", c.getInt(1))
                    put("wrong_count", c.getInt(2))
                    put("fav", c.getInt(3))
                    put("last_ts", c.getLong(4))
                    put("next_review_ts", c.getLong(5))
                    put("ease", c.getDouble(6))
                    put("reps", c.getInt(7))
                    put("interval_days", c.getInt(8))
                }
            }
        )
        root.put(
            "study_log",
            queryArray("SELECT id,word_id,correct,day,ts FROM study_log") { c ->
                JSONObject().apply {
                    put("id", c.getLong(0))
                    put("word_id", c.getInt(1))
                    put("correct", c.getInt(2))
                    put("day", c.getString(3) ?: "")
                    put("ts", c.getLong(4))
                }
            }
        )
        root.put(
            "quiz_record",
            queryArray(
                "SELECT id,stage,unit,mode,score,total,correct,max_combo,duration_ms,ts " +
                    "FROM quiz_record"
            ) { c ->
                JSONObject().apply {
                    put("id", c.getLong(0))
                    put("stage", c.getInt(1))
                    put("unit", c.getInt(2))
                    put("mode", c.getString(3) ?: "")
                    put("score", c.getInt(4))
                    put("total", c.getInt(5))
                    put("correct", c.getInt(6))
                    put("max_combo", c.getInt(7))
                    put("duration_ms", c.getLong(8))
                    put("ts", c.getLong(9))
                }
            }
        )
        root.put(
            "achievement",
            queryArray("SELECT code,unlocked_ts FROM achievement") { c ->
                JSONObject().apply {
                    put("code", c.getString(0) ?: "")
                    put("unlocked_ts", c.getLong(1))
                }
            }
        )
        root.put(
            "app_meta",
            queryArray("SELECT k,v FROM app_meta") { c ->
                JSONObject().apply {
                    put("k", c.getString(0) ?: "")
                    put("v", c.getString(1) ?: "")
                }
            }
        )
        root.put(
            "prefs",
            JSONObject().apply {
                put("stage", prefs.getInt(KEY_STAGE, WordDb.STAGE_CET4))
                put("daily_goal", prefs.getInt(KEY_GOAL, 20))
                put("auto_speak", prefs.getBoolean(KEY_AUTOSPEAK, true))
                put("speech_rate", prefs.getFloat(KEY_RATE, 0.92f).toDouble())
                put("current_unit", prefs.getInt(KEY_UNIT, 0))
            }
        )
        file.writeText(root.toString(2))
        return file
    }

    // ---------------- 恢复 ----------------

    /** 列出备份目录下所有 JSON 备份，最新的在前 */
    fun listBackups(): List<File> =
        backupDir().listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /**
     * 用指定备份覆盖当前全部学习数据。
     * 会先清空相关数据表，再写回备份内容，最后恢复 SharedPreferences。
     */
    fun restore(file: File) {
        val root = JSONObject(file.readText())
        val db = userDb.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM word_state")
            db.execSQL("DELETE FROM study_log")
            db.execSQL("DELETE FROM quiz_record")
            db.execSQL("DELETE FROM achievement")
            db.execSQL("DELETE FROM app_meta")

            each(root.optJSONArray("word_state")) { o ->
                db.insertWithOnConflict(
                    "word_state", null,
                    ContentValues().apply {
                        put("word_id", o.getInt("word_id"))
                        put("level", o.getInt("level"))
                        put("wrong_count", o.getInt("wrong_count"))
                        put("fav", o.getInt("fav"))
                        put("last_ts", o.getLong("last_ts"))
                        put("next_review_ts", o.getLong("next_review_ts"))
                        put("ease", o.optDouble("ease", 2.5))
                        put("reps", o.optInt("reps", 0))
                        put("interval_days", o.optInt("interval_days", 0))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            each(root.optJSONArray("study_log")) { o ->
                db.insertWithOnConflict(
                    "study_log", null,
                    ContentValues().apply {
                        put("id", o.getLong("id"))
                        put("word_id", o.getInt("word_id"))
                        put("correct", o.getInt("correct"))
                        put("day", o.getString("day"))
                        put("ts", o.getLong("ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            each(root.optJSONArray("quiz_record")) { o ->
                db.insertWithOnConflict(
                    "quiz_record", null,
                    ContentValues().apply {
                        put("id", o.getLong("id"))
                        put("stage", o.getInt("stage"))
                        put("unit", o.getInt("unit"))
                        put("mode", o.getString("mode"))
                        put("score", o.getInt("score"))
                        put("total", o.getInt("total"))
                        put("correct", o.getInt("correct"))
                        put("max_combo", o.getInt("max_combo"))
                        put("duration_ms", o.getLong("duration_ms"))
                        put("ts", o.getLong("ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            each(root.optJSONArray("achievement")) { o ->
                db.insertWithOnConflict(
                    "achievement", null,
                    ContentValues().apply {
                        put("code", o.getString("code"))
                        put("unlocked_ts", o.getLong("unlocked_ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            each(root.optJSONArray("app_meta")) { o ->
                db.insertWithOnConflict(
                    "app_meta", null,
                    ContentValues().apply {
                        put("k", o.getString("k"))
                        put("v", o.getString("v"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        root.optJSONObject("prefs")?.let { p ->
            prefs.edit().apply {
                if (p.has(KEY_STAGE)) putInt(KEY_STAGE, p.optInt(KEY_STAGE, WordDb.STAGE_CET4))
                if (p.has(KEY_GOAL)) putInt(KEY_GOAL, p.optInt(KEY_GOAL, 20))
                if (p.has(KEY_AUTOSPEAK)) putBoolean(KEY_AUTOSPEAK, p.optBoolean(KEY_AUTOSPEAK, true))
                if (p.has(KEY_RATE)) putFloat(KEY_RATE, p.optDouble(KEY_RATE, 0.92).toFloat())
                if (p.has(KEY_UNIT)) putInt(KEY_UNIT, p.optInt(KEY_UNIT, 0))
            }.apply()
        }
    }

    // ---------------- 内部工具 ----------------

    private inline fun queryArray(sql: String, row: (Cursor) -> JSONObject): JSONArray {
        val arr = JSONArray()
        userDb.readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) arr.put(row(c))
        }
        return arr
    }

    private inline fun each(arr: JSONArray?, action: (JSONObject) -> Unit) {
        if (arr == null) return
        for (i in 0 until arr.length()) action(arr.getJSONObject(i))
    }

    private fun formatTs(ts: Long): String =
        if (ts <= 0L) "" else timeText.format(Date(ts))

    /** CSV 单元格转义：含逗号/引号/换行时用双引号包裹，内部引号翻倍 */
    private fun csvCell(value: String): String =
        if (value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        ) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    companion object {
        private const val KEY_STAGE = "stage"
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_AUTOSPEAK = "auto_speak"
        private const val KEY_RATE = "speech_rate"
        private const val KEY_UNIT = "current_unit"
    }
}
