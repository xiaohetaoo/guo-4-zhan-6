package com.hdu.cetvocab.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.hdu.cetvocab.util.LauncherIcons
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    // 用 DateTimeFormatter 而不是 SimpleDateFormat：前者不可变、线程安全，
    // 可以安全地作为字段复用。SimpleDateFormat 非线程安全，多线程并发导出时
    // 会输出错乱的时间甚至抛异常。
    // 文件名精确到秒：原来只到分钟，同一分钟内连点两次「备份数据」会互相覆盖，
    // 静默丢掉一份备份。
    private val fileStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val timeText = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun stamp(): String = fileStamp.format(LocalDateTime.now())

    /** 每个目录最多保留的历史文件数，超出的按时间从旧到新删除 */
    private val keepPerDir = 20

    private fun exportDir(): File = File(baseDir(), "导出").apply { mkdirs() }

    private fun backupDir(): File = File(baseDir(), "备份").apply { mkdirs() }

    /** 优先用应用外部私有目录；极端情况下（无外部存储）退回内部私有目录 */
    private fun baseDir(): File = context.getExternalFilesDir(null) ?: context.filesDir

    /**
     * 只保留最新的 [keepPerDir] 个文件，其余删除。
     * 导出与备份都是「每次新建一个带时间戳的文件」，不清理会无限增长。
     */
    private fun prune(dir: File, suffix: String) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(suffix, ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keepPerDir).forEach { runCatching { it.delete() } }
    }

    // ---------------- 导出 CSV ----------------

    /** 导出全部学习记录，一行一个单词状态 */
    fun exportStudyCsv(): File {
        val file = File(exportDir(), "学习记录_${stamp()}.csv")
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
        prune(exportDir(), ".csv")
        return file
    }

    /** 导出收藏的单词本，按字母序 */
    fun exportFavCsv(): File {
        val file = File(exportDir(), "单词本_${stamp()}.csv")
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
        prune(exportDir(), ".csv")
        return file
    }

    // ---------------- 备份 JSON ----------------

    /** 备份全部用户数据为单个 JSON 文件 */
    fun backupJson(): File {
        val file = File(backupDir(), "cetvocab_backup_${stamp()}.json")
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
                // 图标风格与明暗模式也要备份：设置页的备份说明写的是
                // 「完整备份学习进度、成绩、成就与设置」，少了这两项就名不副实 ——
                // 用户换机恢复后会发现主题和桌面图标回到了默认值。
                put("icon_style", prefs.getInt(KEY_ICON_STYLE, LauncherIcons.STYLE_DEFAULT))
                put("theme_mode", prefs.getInt(KEY_THEME, VocabRepository.THEME_SYSTEM))
            }
        )
        file.writeText(root.toString(2))
        prune(backupDir(), ".json")
        return file
    }

    // ---------------- 恢复 ----------------

    /** 列出备份目录下所有 JSON 备份，最新的在前 */
    fun listBackups(): List<File> =
        backupDir().listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** 恢复结果摘要：让界面能如实告诉用户「恢复了多少、跳过了多少失效记录」 */
    data class RestoreReport(
        val words: Int,
        val logs: Int,
        val quizzes: Int,
        /** 因 word_id 在当前词库中不存在而被丢弃的记录数 */
        val skipped: Int
    )

    /**
     * 用指定备份覆盖当前全部学习数据。
     *
     * 执行顺序刻意设计为「**先全部校验、再动数据**」：
     * 解析与校验阶段一旦失败就直接抛异常，此时一行数据都还没删，用户现有进度完好无损。
     * 历史实现是先 `readText()` 再进事务删表，虽然事务能回滚，
     * 但缺少版本与有效性校验，会把一批指向不存在单词的记录写进来。
     */
    fun restore(file: File): RestoreReport {
        val root = JSONObject(file.readText())

        // ---------- 1. 校验：不通过则直接抛出，此时尚未触碰任何现有数据 ----------
        val backupVersion = root.optInt("db_version", 0)
        require(backupVersion <= UserDb.DB_VERSION) {
            "备份来自更新的应用版本（备份 v$backupVersion，当前 v${UserDb.DB_VERSION}），请先升级应用再恢复"
        }

        // 词库随版本更新，旧备份里的 word_id 可能已不存在于当前词库。
        // 这类「孤儿记录」永远不会出现在任何列表里，却会占用 totalLearned 等统计口径，
        // 所以这里先取当前词库的全部合法 id，恢复时把无效条目剔除。
        val validIds = (
            wordDb.idsOfStage(WordDb.STAGE_CET4) + wordDb.idsOfStage(WordDb.STAGE_CET6)
            ).toHashSet()

        val allStates = root.optJSONArray("word_state").objects()
        val states = allStates.filter { it.optInt("word_id", -1) in validIds }
        val allLogs = root.optJSONArray("study_log").objects()
        val logs = allLogs.filter { it.optInt("word_id", -1) in validIds }
        val quizzes = root.optJSONArray("quiz_record").objects()
        val skipped = (allStates.size - states.size) + (allLogs.size - logs.size)

        // ---------- 2. 写回：整段包在一个事务里，中途失败会自动回滚 ----------
        val db = userDb.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM word_state")
            db.execSQL("DELETE FROM study_log")
            db.execSQL("DELETE FROM quiz_record")
            db.execSQL("DELETE FROM achievement")
            db.execSQL("DELETE FROM app_meta")

            states.forEach { o ->
                db.insertWithOnConflict(
                    "word_state", null,
                    ContentValues().apply {
                        put("word_id", o.optInt("word_id"))
                        put("level", o.optInt("level"))
                        put("wrong_count", o.optInt("wrong_count"))
                        put("fav", o.optInt("fav"))
                        put("last_ts", o.optLong("last_ts"))
                        put("next_review_ts", o.optLong("next_review_ts"))
                        put("ease", o.optDouble("ease", 2.5))
                        put("reps", o.optInt("reps", 0))
                        put("interval_days", o.optInt("interval_days", 0))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            logs.forEach { o ->
                db.insertWithOnConflict(
                    "study_log", null,
                    ContentValues().apply {
                        // 刻意不写 id：让 SQLite 重新分配自增主键。
                        // 备份里的 id 可能与当前库的自增序列冲突，回填会导致后续插入撞主键。
                        put("word_id", o.optInt("word_id"))
                        put("correct", o.optInt("correct"))
                        put("day", o.optString("day"))
                        put("ts", o.optLong("ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            quizzes.forEach { o ->
                db.insertWithOnConflict(
                    "quiz_record", null,
                    ContentValues().apply {
                        put("stage", o.optInt("stage"))
                        put("unit", o.optInt("unit"))
                        put("mode", o.optString("mode"))
                        put("score", o.optInt("score"))
                        put("total", o.optInt("total"))
                        put("correct", o.optInt("correct"))
                        put("max_combo", o.optInt("max_combo"))
                        put("duration_ms", o.optLong("duration_ms"))
                        put("ts", o.optLong("ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            root.optJSONArray("achievement").objects().forEach { o ->
                db.insertWithOnConflict(
                    "achievement", null,
                    ContentValues().apply {
                        put("code", o.optString("code"))
                        put("unlocked_ts", o.optLong("unlocked_ts"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            root.optJSONArray("app_meta").objects().forEach { o ->
                db.insertWithOnConflict(
                    "app_meta", null,
                    ContentValues().apply {
                        put("k", o.optString("k"))
                        put("v", o.optString("v"))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        // ---------- 3. 偏好 ----------
        // 偏好与数据库分属两个存储，没有跨存储事务，任何顺序都无法做到「原子」。
        // 这里刻意把偏好放在数据库之后，理由是两个方向的失败代价不对称：
        //   · 数据库已提交、偏好没写成功 → 用户只是保留了自己当前的设置（级别/目标/语速），
        //     数据是对的，行为无害；
        //   · 反过来先写偏好、数据库失败 → 设置被换成备份里的旧值，但数据还是原来的，
        //     用户会看到「设置变了、进度没变」，更容易误判为恢复失败。
        // 另外用 commit() 而不是 apply()：apply 是异步落盘，进程被杀时写入可能丢失，
        // 而这里需要「恢复完成」这个语义是确定的。
        root.optJSONObject("prefs")?.let { p ->
            prefs.edit().apply {
                if (p.has(KEY_STAGE)) putInt(KEY_STAGE, p.optInt(KEY_STAGE, WordDb.STAGE_CET4))
                if (p.has(KEY_GOAL)) putInt(KEY_GOAL, p.optInt(KEY_GOAL, 20))
                if (p.has(KEY_AUTOSPEAK)) putBoolean(KEY_AUTOSPEAK, p.optBoolean(KEY_AUTOSPEAK, true))
                if (p.has(KEY_RATE)) putFloat(KEY_RATE, p.optDouble(KEY_RATE, 0.92).toFloat())
                if (p.has(KEY_UNIT)) putInt(KEY_UNIT, p.optInt(KEY_UNIT, 0))
                if (p.has(KEY_ICON_STYLE)) {
                    putInt(KEY_ICON_STYLE, p.optInt(KEY_ICON_STYLE, LauncherIcons.STYLE_DEFAULT))
                }
                if (p.has(KEY_THEME)) {
                    putInt(KEY_THEME, p.optInt(KEY_THEME, VocabRepository.THEME_SYSTEM))
                }
            }.commit()
        }

        return RestoreReport(states.size, logs.size, quizzes.size, skipped)
    }

    // ---------------- 内部工具 ----------------

    private inline fun queryArray(sql: String, row: (Cursor) -> JSONObject): JSONArray {
        val arr = JSONArray()
        userDb.readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) arr.put(row(c))
        }
        return arr
    }

    /** JSONArray -> List<JSONObject>；null 或元素类型不符时返回空列表 */
    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        val list = ArrayList<JSONObject>(length())
        for (i in 0 until length()) optJSONObject(i)?.let { list.add(it) }
        return list
    }

    private fun formatTs(ts: Long): String =
        if (ts <= 0L) "" else timeText.format(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
        )

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
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_THEME = "theme_mode"
    }
}
