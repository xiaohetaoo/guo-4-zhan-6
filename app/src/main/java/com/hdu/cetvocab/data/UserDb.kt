package com.hdu.cetvocab.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 「参与排名」的闯关记录过滤条件，供榜单 / 最高分 / 闯关次数三处统计复用。
 *
 * 拓展资源练习（[EXTENSION_QUIZ_SCOPES]）的记录照常入库（成就系统要按 unit 统计），
 * 但不算进排名口径，所以统一在这里拼一次 SQL 片段，避免三处各写一遍 `NOT IN` 写歪。
 * 这些 unit 值都是本文件内的常量整数，不存在注入风险。
 */
private val RANKED_ONLY: String =
    "unit NOT IN (" + EXTENSION_QUIZ_SCOPES.joinToString(",") { it.toString() } + ")"

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

    /**
     * 逐版本累加的迁移链。
     *
     * 注意：这里**绝不能**出现「版本跨度不认识就 DROP 全部表」的兜底分支。
     * 历史实现写的是 `if (oldVersion < 2) { 迁移 } else { DROP 五张表; onCreate() }`，
     * 当前 DB_VERSION=2 所以那段永远不会执行，但只要将来把版本提到 3，
     * `oldVersion == 2` 就会落进 else —— 用户全部学习进度、成绩、成就会被静默清空。
     *
     * 因此改成「一档一档往下走」的形式：新增版本时只在末尾续写一个 if，旧数据始终保留。
     * 判断统一用 `oldVersion < N`（而不是维护一个中间变量），
     * 因为迁移是按顺序累加的：oldVersion=1 时 v2、v3 两档会依次执行，语义等价。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v1 -> v2：word_state 增加 SM-2 间隔重复字段，用 ALTER 保留用户已有学习数据
            db.execSQL("ALTER TABLE word_state ADD COLUMN ease REAL NOT NULL DEFAULT 2.5")
            db.execSQL("ALTER TABLE word_state ADD COLUMN reps INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE word_state ADD COLUMN interval_days INTEGER NOT NULL DEFAULT 0")
        }

        // 后续版本在此续写，例如：
        // if (oldVersion < 3) { db.execSQL("ALTER TABLE ...") }
        // 迁移一律用 ALTER / UPDATE 增量完成，不做破坏性重建。
    }

    // ---------- 单词状态 ----------

    fun states(): Map<Int, WordState> {
        val map = HashMap<Int, WordState>()
        readableDatabase.rawQuery("SELECT $STATE_COLUMNS FROM word_state", null).use { c ->
            while (c.moveToNext()) map[c.getInt(0)] = c.toWordState()
        }
        return map
    }

    /**
     * 取单个单词的状态。
     *
     * 注意：**不要**写成 `states()[wordId]`。那样每取一个词都要把整张 word_state 表
     * 读进内存，而本方法在 markLearned / toggleFav / answerWord 里被逐词调用，
     * 学一组 50 个词就会退化成 O(N²) 的全表扫描。
     */
    fun stateOf(wordId: Int): WordState =
        readableDatabase.rawQuery(
            "SELECT $STATE_COLUMNS FROM word_state WHERE word_id=?",
            arrayOf(wordId.toString())
        ).use { c -> if (c.moveToFirst()) c.toWordState() else WordState(wordId) }

    /**
     * 记录一次作答结果。
     *
     * 「读旧值 → 算新值 → 整体 REPLACE」这三步必须放在同一个事务里：
     * 否则两个并发的作答（例如连点两下按钮）会各自读到同一个旧 wrong_count，
     * 后写入的那个把先写入的覆盖掉，导致答错次数丢失。
     */
    fun markLearned(
        wordId: Int, level: Int, nextReviewTs: Long,
        ease: Double, reps: Int, intervalDays: Int, correct: Boolean
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existing = stateOf(wordId)
            // 只有答错才累加错误次数。注意不能用 level 判断对错：首次答对时 level 同样是 1，
            // 若按 level <= 1 记错，会把所有新学且答对的词都误计入错词本。
            val wrong = if (correct) existing.wrongCount else existing.wrongCount + 1
            val cv = ContentValues().apply {
                put("word_id", wordId)
                put("level", level)
                put("wrong_count", wrong)
                put("fav", if (existing.fav) 1 else 0)
                put("last_ts", System.currentTimeMillis())
                put("next_review_ts", nextReviewTs)
                put("ease", ease)
                put("reps", reps)
                put("interval_days", intervalDays)
            }
            db.insertWithOnConflict("word_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 切换收藏，返回切换后的新值。
     *
     * 「读旧值 → 取反 → 写回」必须在同一个事务里完成。
     * 历史实现是 `setFav(id, !stateOf(id).fav)`：读与写是两次独立的数据库调用，
     * 中间存在窗口 —— 用户连点两下时，两次都会读到同一个旧值、算出同一个新值，
     * 结果只翻转了一次（用户以为取消了收藏，实际仍处于收藏状态）。
     *
     * 写入只更新 fav 这一列：早期实现会把整行读出来再原样写回，
     * 期间若有别的写入（例如同时答对一题）就会被这次回写覆盖掉。
     * 行不存在时才插入（其余列走建表默认值）。
     */
    fun toggleFav(wordId: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val next = !stateOf(wordId).fav
            val updated = db.update(
                "word_state",
                ContentValues().apply { put("fav", if (next) 1 else 0) },
                "word_id=?",
                arrayOf(wordId.toString())
            )
            if (updated == 0) {
                db.insertWithOnConflict(
                    "word_state", null,
                    ContentValues().apply {
                        put("word_id", wordId)
                        put("fav", if (next) 1 else 0)
                    },
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            db.setTransactionSuccessful()
            return next
        } finally {
            db.endTransaction()
        }
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
                "FROM quiz_record WHERE $RANKED_ONLY ORDER BY score DESC, ts DESC LIMIT $limit", null
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
                "FROM quiz_record WHERE $RANKED_ONLY ORDER BY ts DESC LIMIT $limit", null
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
        readableDatabase.rawQuery(
            "SELECT MAX(score) FROM quiz_record WHERE $RANKED_ONLY", null
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    fun quizPlayedCount(): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE $RANKED_ONLY", null
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /**
     * 排行榜页顶部三块统计（最高分 / 平均分 / 最高正确率 + 总次数），一次查询算完。
     *
     * 关键：在**全表**上聚合，而不是拿 `quizRecords(50)` 的结果现算 ——
     * 后者在闯关超过 50 次后会把统计范围悄悄缩到那 50 条里，
     * 出现「共 87 次闯关记录」却「平均分只按 50 条算」的自相矛盾。
     *
     * `IFNULL` 是为了让「一条记录都没有」时也返回一行 0，而不是 NULL。
     * `correct*100/total` 在 SQLite 里是整数除法，与 [QuizRecord.accuracy] 口径一致。
     */
    fun quizAggregate(): QuizAggregate =
        readableDatabase.rawQuery(
            "SELECT COUNT(*), IFNULL(MAX(score), 0), IFNULL(CAST(AVG(score) AS INTEGER), 0), " +
                "IFNULL(MAX(CASE WHEN total > 0 THEN correct * 100 / total ELSE 0 END), 0) " +
                "FROM quiz_record WHERE $RANKED_ONLY", null
        ).use { c ->
            if (c.moveToFirst()) {
                QuizAggregate(
                    count = c.getInt(0),
                    bestScore = c.getInt(1),
                    avgScore = c.getInt(2),
                    bestAccuracy = c.getInt(3)
                )
            } else {
                QuizAggregate()
            }
        }

    /** 某一专题关卡（按 unit 范围值）的完成次数 */
    fun quizCountOfUnit(unit: Int): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE unit=?",
            arrayOf(unit.toString())
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /**
     * 某一专题关卡「全对通关」的次数。
     *
     * `total > 0` 不能省：题库异常时可能写入 total=0 的记录，
     * 那样 correct=total=0 会被当成一次全对，成就白送。
     */
    fun quizPerfectCountOfUnit(unit: Int): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE unit=? AND total>0 AND correct=total",
            arrayOf(unit.toString())
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /** 打过哪些专题关卡（返回出现过的 unit 范围值集合） */
    fun quizPlayedUnits(units: Collection<Int>): Set<Int> {
        if (units.isEmpty()) return emptySet()
        val placeholders = units.joinToString(",") { "?" }
        val args = units.map { it.toString() }.toTypedArray()
        val found = HashSet<Int>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT unit FROM quiz_record WHERE unit IN ($placeholders)",
            args
        ).use { c ->
            while (c.moveToNext()) found.add(c.getInt(0))
        }
        return found
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

    /**
     * 解锁成就。
     *
     * 用单条 INSERT + CONFLICT_IGNORE 判断是否「本次真的解锁了」：
     * 插入成功返回行号，主键冲突被忽略时返回 -1。这样既避免了
     * 「先查是否存在、再插入」的竞态（并发时会重复上报解锁），
     * 也省掉了每次 unlock 都把整张 achievement 表读一遍的开销。
     */
    fun unlock(code: String): Boolean {
        val cv = ContentValues().apply {
            put("code", code)
            put("unlocked_ts", System.currentTimeMillis())
        }
        val rowId = writableDatabase.insertWithOnConflict(
            "achievement", null, cv, SQLiteDatabase.CONFLICT_IGNORE
        )
        return rowId != -1L
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

    /** 累加学习时长。读改写同样放在事务里，避免并发时后写覆盖先写。 */
    fun addStudySeconds(seconds: Long) {
        if (seconds <= 0) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cur = getMeta("study_seconds", "0").toLongOrNull() ?: 0L
            setMeta("study_seconds", (cur + seconds).toString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun studySeconds(): Long = getMeta("study_seconds", "0").toLongOrNull() ?: 0L

    companion object {
        const val DB_NAME = "user_data.db"
        const val DB_VERSION = 2

        /** word_state 的全部列，顺序与 [toWordState] 的下标严格对应 */
        private const val STATE_COLUMNS =
            "word_id,level,wrong_count,fav,last_ts,next_review_ts,ease,reps,interval_days"

        fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}

/** 把 word_state 的一行按 [UserDb.STATE_COLUMNS] 的顺序映射成 [WordState] */
private fun Cursor.toWordState(): WordState = WordState(
    wordId = getInt(0),
    level = getInt(1),
    wrongCount = getInt(2),
    fav = getInt(3) == 1,
    lastTs = getLong(4),
    nextReviewTs = getLong(5),
    ease = getDouble(6),
    reps = getInt(7),
    intervalDays = getInt(8)
)
