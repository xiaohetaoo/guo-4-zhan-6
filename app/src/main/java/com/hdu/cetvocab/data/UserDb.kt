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

    /**
     * 学习记录库的可用性。
     *
     * 与 [WordDb.status] 是同一套思路，但方向恰好相反、代价也更大：
     * 词库损坏可以从 assets 重拷（可再生），学习记录损坏**不可再生** ——
     * 用户唯一的出路是从备份恢复。所以这里也必须把「读不出来」变成
     * 「一句人话 + 一个可继续操作的界面」，而不是让异常穿过 viewModelScope 崩掉应用。
     *
     * 为什么必须做：本类的读方法（[states]、[achievements]、[quizRecords]…）
     * 遍布首页 / 词库 / 我的三条刷新链路，而这三条链路又都在
     * `AppRoot` 的 `LaunchedEffect(Unit) { refreshAll() }` 里 —— 也就是说
     * 库一旦在读取中途损坏（闪存故障、异常掉电），会退化成「每次冷启动都崩，
     * 用户只能去系统设置里清数据（连带丢掉全部进度）」。
     * `WordDb.kt` 的注释点名过这个「无限闪退循环」，只是当时只保护了词库那一侧。
     */
    @Volatile
    var status: UserDbStatus = UserDbStatus.OK
        private set

    /** 记录一次读取失败：损坏与其它不可用分开，界面据此决定说哪一句 */
    internal fun noteFailure(t: Throwable) {
        status = if (isCorruption(t)) UserDbStatus.CORRUPT else UserDbStatus.UNAVAILABLE
    }

    private fun isCorruption(t: Throwable): Boolean =
        t is android.database.sqlite.SQLiteDatabaseCorruptException ||
            t is android.database.sqlite.SQLiteDiskIOException ||
            (t is android.database.sqlite.SQLiteException &&
                (t.message?.contains("malformed", true) == true ||
                    t.message?.contains("not a database", true) == true))

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
        createQuizWrong(db)
    }

    /**
     * 闯关错题集。
     *
     * 与 word_state.wrong_count（学习卡片「答错」计数，喂给错词本）是两套东西，互不合并：
     * 卡片答错说明这个词没记住，闯关答错说明「这道题」没做对，
     * 用户要的是「把闯关错的题再练一遍」，而不是把闯关变成第二本错词本。
     *
     * 主键取 (stage, kind, word_id)：同一个词在四级错选义、在六级错拼写，
     * 是三条独立的待订正记录；只按 word_id 建键会让后一次答错覆盖前一次，
     * 重做时就只补得回一种题型。
     */
    private fun createQuizWrong(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS quiz_wrong(" +
                "stage INTEGER NOT NULL," +
                "kind INTEGER NOT NULL," +
                "word_id INTEGER NOT NULL," +
                "wrong_count INTEGER NOT NULL DEFAULT 1," +
                "last_ts INTEGER NOT NULL DEFAULT 0," +
                "PRIMARY KEY(stage, kind, word_id))"
        )
        // 重做队列按「错得多 → 错得早」排序。这个索引服务的是 `WHERE stage=?` 这一步过滤
        // （本表行数很少，取题时的两列排序走不到索引，也没有必要走）。
        // 不要指望它省掉排序：ORDER BY 是 wrong_count DESC + last_ts ASC 的混向排序，
        // 索引只能整体正扫或整体倒扫，混向顺序永远用不上索引排序。
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_wrong_stage ON quiz_wrong(stage, wrong_count, last_ts)")
    }

    /**
     * 逐版本累加的迁移链。
     *
     * 注意：这里**绝不能**出现「版本跨度不认识就 DROP 全部表」的兜底分支。
     * 历史实现写的是 `if (oldVersion < 2) { 迁移 } else { DROP 五张表; onCreate() }`，
     * 当时 DB_VERSION=2，所以那段永远不会执行，但只要把版本提到 3，
     * `oldVersion == 2` 就会落进 else —— 用户全部学习进度、成绩、成就会被静默清空。
     * 现在 DB_VERSION=3，那条 else 已经是「会执行」的状态，绝对不能写回来。
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

        if (oldVersion < 3) {
            // v2 -> v3：新增闯关错题集表。纯增表，不动任何既有行。
            createQuizWrong(db)
        }

        // 后续版本在此续写，例如：
        // if (oldVersion < 4) { db.execSQL("ALTER TABLE ...") }
        // 迁移一律用 ALTER / UPDATE 增量完成，不做破坏性重建。
    }

    /**
     * 降级（同一台设备上装了比现有库更旧的 APK）。
     *
     * 默认实现是直接抛 `SQLiteException: A downgrade ... cannot be performed`，
     * 而它发生在 `getWritableDatabase()` 里 —— 本类的读方法遍布首页 / 词库 / 我的，
     * 结果是老版本 APK 一装上去就**每次启动都崩**，用户除了卸载（连带丢掉全部进度）
     * 没有别的出路。这是侧载场景（我们的分发方式正是发 APK），不是理论问题。
     *
     * 这里选择空实现而不是「删库重建」：本项目的迁移全是加列 / 加表
     * （v1→v2 三个 SM-2 列、v2→v3 一张 quiz_wrong），旧版代码读新版库时
     * 那些多出来的列和表它根本不查询，留着不影响任何读写路径；
     * 反过来一旦删库，用户进度就真没了 —— 两个代价明显不对称。
     *
     * **前提**：以后若出现「改列语义 / 删列 / 重算数据」这类非增量迁移，
     * 这个空实现就不再安全，必须在这里显式处理（例如把不兼容的表清掉），
     * 并把这条写进迁移注释里。
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---------- 单词状态 ----------

    /**
     * 读操作的统一入口：任何一次读失败都**降级返回 [fallback]**，
     * 同时把 [status] 标成损坏/不可用，由界面层负责说出来。
     *
     * 与 [WordDb.reading] 同构。之所以不能直接往外抛：这些查询的调用方
     * 全部跑在 `viewModelScope` 里，而 viewModelScope 没配 CoroutineExceptionHandler
     * —— 未捕获异常会直接结束协程并让应用崩溃。降级 + 状态位是把崩溃换成一句人话
     * 的唯一办法，且**学习记录本身还在库里**，用户可以从备份恢复。
     *
     * 注意这里不删库、不重建：用户数据的损坏是「读不出来」，
     * 而不是「需要重装」—— 唯一正确的动作是提示用户从备份恢复，
     * 任何自动清库都会把还能救的数据真的抹掉。
     */
    private fun <T> reading(fallback: T, block: (SQLiteDatabase) -> T): T =
        try {
            block(readableDatabase)
        } catch (e: Exception) {
            noteFailure(e)
            fallback
        }

    fun states(): Map<Int, WordState> = reading(emptyMap()) { db ->
        val map = HashMap<Int, WordState>()
        db.rawQuery("SELECT $STATE_COLUMNS FROM word_state", null).use { c ->
            while (c.moveToNext()) map[c.getInt(0)] = c.toWordState()
        }
        map
    }

    /**
     * 取单个单词的状态。
     *
     * 注意：**不要**写成 `states()[wordId]`。那样每取一个词都要把整张 word_state 表
     * 读进内存，而本方法在 markLearned / toggleFav / answerWord 里被逐词调用，
     * 学一组 50 个词就会退化成 O(N²) 的全表扫描。
     */
    fun stateOf(wordId: Int): WordState = reading(WordState(wordId)) { db ->
        db.rawQuery(
            "SELECT $STATE_COLUMNS FROM word_state WHERE word_id=?",
            arrayOf(wordId.toString())
        ).use { c -> if (c.moveToFirst()) c.toWordState() else WordState(wordId) }
    }

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
     * 撤销一次作答：把 [wordId] 的状态整体写回 [prev]。
     *
     * 供学习页「返回上一个词」使用（见 `AppViewModel.stepBack`）。
     * 为什么必须真的回写而不是只把界面索引退一格：`answerCurrent` 已经
     * 改过 word_state 并往 study_log 插了一行，只退界面的话，
     * 同一个词会被再答一次 —— 累计学习、正确率、连续打卡全部虚高，
     * 而错词本里的 wrong_count 也会被扣两次或加两次。
     *
     * 这里**整体 REPLACE 回旧快照**（含 ease / reps / interval_days），
     * 而不是「反向执行一遍 SM-2」：SM-2 不可逆（ease 的更新依赖 q，
     * 且 interval 是乘性递推），反向推算必然累积误差。
     * 快照是调用方在作答**之前**取的 [stateOf]，逐字段还原是精确的。
     *
     * @param prev 作答前的状态快照
     */
    fun restoreState(prev: WordState) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("word_id", prev.wordId)
                put("level", prev.level)
                put("wrong_count", prev.wrongCount)
                put("fav", if (prev.fav) 1 else 0)
                put("last_ts", prev.lastTs)
                put("next_review_ts", prev.nextReviewTs)
                put("ease", prev.ease)
                put("reps", prev.reps)
                put("interval_days", prev.intervalDays)
            }
            db.insertWithOnConflict("word_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 删除某个词最近一条学习日志，返回是否真的删掉了。
     *
     * 与 [restoreState] 配对使用：只还原 word_state 而留着日志，
     * 「今日已学」「累计学习」「连续打卡」这些按 study_log 聚合的统计
     * 仍会把那次作废的作答算进去。
     *
     * 删的是**该词 ts 最大的那一条**而不是「今天那一条」：用户可能
     * 隔天才回头改判（学习页不保存跨天会话，但 BACK 撤销发生在同一会话内），
     * 按 ts 取最新一条与作答顺序严格对应。
     */
    fun deleteLatestLog(wordId: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val rowId = reading<Long?>(null) { d ->
                d.rawQuery(
                    "SELECT id FROM study_log WHERE word_id=? ORDER BY ts DESC, id DESC LIMIT 1",
                    arrayOf(wordId.toString())
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
            }
            val deleted = rowId != null && db.delete("study_log", "id=?", arrayOf(rowId.toString())) > 0
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 批量把一批词标记为「已学会」，返回实际写入的行数。
     *
     * 设置页「一键把高中词汇标为已学会」用这个入口。语义刻意定成
     * 「**已学会** = level 3（熟悉）」，而不是 5（精通）：
     *  · level ≥ 3 才算进「熟悉以上」这个统计口径（见 [WordState.level] 注释）；
     *  · 标成 5 会让错词本把这些词直接移出、也不再进复习队列，
     *    等于替用户断言「这些词你已经完全掌握」—— 一键操作不该有这种效力；
     *  · level 3 让它们仍会在到期时进入复习队列，用户真想彻底压掉，
     *    可以在词库页逐个降级。
     *
     * nextReviewTs 给「现在 + 一个较长间隔」而不是 0：0 表示「立刻到期」，
     * 2604 个词会当场全部涌进今日队列，把每日目标冲垮。
     * reps=0、intervalDays=0 保持中性，后续答对时按 SM-2 正常从 1 天起算。
     *
     * 已经在 level ≥ 3 的词不重复写（避免把 last_ts 刷成今天、
     * 让「上次学习」这个显示变得没意义）。整批在一个事务里，
     * 中途失败不会留下「标了一半」的状态。
     *
     * @return 真正从「未学会」变成「已学会」的行数
     */
    fun markBatchLearned(wordIds: Collection<Int>): Int {
        if (wordIds.isEmpty()) return 0
        val db = writableDatabase
        val now = System.currentTimeMillis()
        // 与 markLearned 同一口径：3 = 熟悉，进入「熟悉以上」统计
        val level = 3
        val nextReviewTs = now + 30L * 86_400_000L
        var changed = 0
        db.beginTransaction()
        try {
            // 先查已有状态，只对 level < 3 的下手；分批查避免 IN 变量数超 999
            val existing = HashMap<Int, WordState>(wordIds.size)
            wordIds.chunked(500).forEach { chunk ->
                db.rawQuery(
                    "SELECT $STATE_COLUMNS FROM word_state WHERE word_id IN (" +
                        chunk.joinToString(",") + ")",
                    null
                ).use { c -> while (c.moveToNext()) c.toWordState().let { existing[it.wordId] = it } }
            }
            wordIds.chunked(500).forEach { chunk ->
                chunk.forEach { id ->
                    val old = existing[id]
                    if (old != null && old.level >= level) return@forEach
                    val cv = ContentValues().apply {
                        put("word_id", id)
                        put("level", level)
                        // 保留既有的错词次数与收藏，一键标记不改这两项
                        put("wrong_count", old?.wrongCount ?: 0)
                        put("fav", if (old?.fav == true) 1 else 0)
                        put("last_ts", now)
                        put("next_review_ts", nextReviewTs)
                        put("ease", old?.ease ?: 2.5)
                        put("reps", old?.reps ?: 0)
                        put("interval_days", old?.intervalDays ?: 0)
                    }
                    db.insertWithOnConflict("word_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    changed++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return changed
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

    fun todayLearnedCount(): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(DISTINCT word_id) FROM study_log WHERE day=?", arrayOf(today())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun distinctWordCount(): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(DISTINCT word_id) FROM study_log", null
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun correctWrongTotals(): Pair<Int, Int> = reading(0 to 0) { db ->
        db.rawQuery(
            "SELECT SUM(correct), COUNT(*)-SUM(correct) FROM study_log", null
        ).use {
            if (it.moveToFirst()) (it.getInt(0)) to (it.getInt(1)) else 0 to 0
        }
    }

    fun studyDays(): List<String> = reading(emptyList()) { db ->
        val days = ArrayList<String>()
        db.rawQuery(
            "SELECT DISTINCT day FROM study_log ORDER BY day DESC", null
        ).use { c -> while (c.moveToNext()) days.add(c.getString(0)) }
        days
    }

    fun recentDays(n: Int): List<DayStat> = reading(emptyList()) { db ->
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val from = today.minusDays((n - 1).toLong()).format(fmt)
        val raw = HashMap<String, DayStat>()
        db.rawQuery(
            "SELECT day, COUNT(DISTINCT word_id), SUM(correct), COUNT(*)-SUM(correct) " +
                "FROM study_log WHERE day>=? GROUP BY day", arrayOf(from)
        ).use { c ->
            while (c.moveToNext()) {
                raw[c.getString(0)] = DayStat(c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3))
            }
        }
        (0 until n).map { i ->
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

    // ---------- 闯关错题集 ----------

    /**
     * 记一道闯关错题（已存在则错误次数 +1）。
     *
     * 用「insertWithOnConflict(IGNORE) 的返回值判断有没有真插进去」，
     * 而不是先 SELECT 再决定 INSERT/UPDATE：那是一次往返而不是两次，
     * 也不会重演 markLearned 修过的那类读改写竞态。
     * IGNORE 命中冲突时返回 -1，据此再走 UPDATE 累加，两条路径互斥、不会重复计数。
     */
    fun addQuizWrong(stage: Int, kind: Int, wordId: Int) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("stage", stage)
                put("kind", kind)
                put("word_id", wordId)
                put("wrong_count", 1)
                put("last_ts", now)
            }
            val rowId = db.insertWithOnConflict(
                "quiz_wrong", null, cv, SQLiteDatabase.CONFLICT_IGNORE
            )
            if (rowId < 0) {
                db.execSQL(
                    "UPDATE quiz_wrong SET wrong_count=wrong_count+1, last_ts=? " +
                        "WHERE stage=? AND kind=? AND word_id=?",
                    arrayOf(now, stage, kind, wordId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** 答对即移出：错题集始终等于「最近一次作答仍是错的题」，不做已订正标记。 */
    fun takeQuizWrong(stage: Int, kind: Int, wordId: Int) {
        writableDatabase.execSQL(
            "DELETE FROM quiz_wrong WHERE stage=? AND kind=? AND word_id=?",
            arrayOf(stage, kind, wordId)
        )
    }

    /** 当前级别的待重做错题数，供排行榜汇总卡显示。 */
    fun quizWrongCount(stage: Int): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(*) FROM quiz_wrong WHERE stage=?", arrayOf(stage.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * 重做队列取题：错得多的优先，同样多的先错的优先。
     * 排序键写死在 SQL 里，与 idx_wrong_stage 的顺序一致，避免全表排序。
     */
    fun quizWrongEntries(stage: Int, limit: Int): List<QuizWrong> = reading(emptyList()) { db ->
        val list = ArrayList<QuizWrong>()
        db.rawQuery(
            "SELECT stage,kind,word_id,wrong_count,last_ts FROM quiz_wrong " +
                "WHERE stage=? ORDER BY wrong_count DESC, last_ts ASC LIMIT $limit",
            arrayOf(stage.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(QuizWrong(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getLong(4)))
            }
        }
        list
    }

    fun allQuizWrong(): List<QuizWrong> = reading(emptyList()) { db ->
        val list = ArrayList<QuizWrong>()
        db.rawQuery(
            "SELECT stage,kind,word_id,wrong_count,last_ts FROM quiz_wrong " +
                "ORDER BY stage,kind,word_id", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(QuizWrong(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getLong(4)))
            }
        }
        list
    }

    /** 备份恢复用：整表替换（先清后插），与其余各段的「清空再写回」口径一致。 */
    fun replaceQuizWrong(rows: List<QuizWrong>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM quiz_wrong")
            for (r in rows) {
                db.execSQL(
                    "INSERT OR REPLACE INTO quiz_wrong(stage,kind,word_id,wrong_count,last_ts) VALUES(?,?,?,?,?)",
                    arrayOf(r.stage, r.kind, r.wordId, r.wrongCount, r.lastTs)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearQuizWrong() {
        writableDatabase.execSQL("DELETE FROM quiz_wrong")
    }

    fun quizRecords(limit: Int = 100): List<QuizRecord> = reading(emptyList()) { db ->
        val list = ArrayList<QuizRecord>()
        db.rawQuery(
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
        list
    }

    fun recentQuizRecords(limit: Int = 20): List<QuizRecord> = reading(emptyList()) { db ->
        val list = ArrayList<QuizRecord>()
        db.rawQuery(
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
        list
    }

    fun bestScore(): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT MAX(score) FROM quiz_record WHERE $RANKED_ONLY", null
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun quizPlayedCount(): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE $RANKED_ONLY", null
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * 全部闯关次数，**不**排除辨析 / 翻译两类拓展练习。
     *
     * 排行榜与最高分用 [quizPlayedCount]（拓展练习不计入排名，是有意设计），
     * 但成就不能用它：成就墙上「明辨秋毫 = 完成一次易混词辨析闯关」和
     * 「初次闯关 = 完成第一次单词闯关」说的是同一件事，用两套口径就会出现
     * 「已解锁明辨秋毫、却显示还没初次闯关」的自相矛盾。
     * 凡是对用户表述为「累计闯关 N 次」的判定，一律走这个全量口径。
     */
    fun quizPlayedCountAll(): Int = reading(0) { db ->
        db.rawQuery("SELECT COUNT(*) FROM quiz_record", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * 排行榜页顶部三块统计（最高分 / 平均分 / 最高正确率 + 总次数），一次查询算完。
     *
     * 关键：在**全表**上聚合，而不是拿 `quizRecords(50)` 的结果现算 ——
     * 后者在闯关超过 50 次后会把统计范围悄悄缩到那 50 条里，
     * 出现「共 87 次闯关记录」却「平均分只按 50 条算」的自相矛盾。
     *
     * `IFNULL` 是为了让「一条记录都没有」时也返回一行 0，而不是 NULL。
     * 正确率必须四舍五入（`ROUND(correct * 100.0 / total)`），与 [percentOf] 同一口径。
     * 早先这里写的是整数除法，与 Kotlin 侧一致但两边一起错：
     * 99.5% 在排行榜显示 99、在首页进度显示 100。
     */
    fun quizAggregate(): QuizAggregate = reading(QuizAggregate()) { db ->
        db.rawQuery(
            "SELECT COUNT(*), IFNULL(MAX(score), 0), IFNULL(CAST(AVG(score) AS INTEGER), 0), " +
                "IFNULL(MAX(CASE WHEN total > 0 THEN CAST(ROUND(correct * 100.0 / total) AS INTEGER) ELSE 0 END), 0) " +
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
    }

    /** 某一专题关卡（按 unit 范围值）的完成次数 */
    fun quizCountOfUnit(unit: Int): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE unit=?",
            arrayOf(unit.toString())
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * 某一专题关卡「全对通关」的次数。
     *
     * `total > 0` 不能省：题库异常时可能写入 total=0 的记录，
     * 那样 correct=total=0 会被当成一次全对，成就白送。
     */
    fun quizPerfectCountOfUnit(unit: Int): Int = reading(0) { db ->
        db.rawQuery(
            "SELECT COUNT(*) FROM quiz_record WHERE unit=? AND total>0 AND correct=total",
            arrayOf(unit.toString())
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /** 打过哪些专题关卡（返回出现过的 unit 范围值集合） */
    fun quizPlayedUnits(units: Collection<Int>): Set<Int> {
        if (units.isEmpty()) return emptySet()
        val placeholders = units.joinToString(",") { "?" }
        val args = units.map { it.toString() }.toTypedArray()
        return reading(emptySet()) { db ->
            val found = HashSet<Int>()
            db.rawQuery(
                "SELECT DISTINCT unit FROM quiz_record WHERE unit IN ($placeholders)",
                args
            ).use { c ->
                while (c.moveToNext()) found.add(c.getInt(0))
            }
            found
        }
    }

    fun quizTotalMs(): Long = reading(0L) { db ->
        db.rawQuery("SELECT SUM(duration_ms) FROM quiz_record", null).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
    }

    // ---------- 成就 ----------

    fun achievements(): Map<String, Long> = reading(emptyMap()) { db ->
        val map = HashMap<String, Long>()
        db.rawQuery("SELECT code,unlocked_ts FROM achievement", null).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        map
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

    fun getMeta(key: String, def: String = "0"): String = reading(def) { db ->
        db.rawQuery("SELECT v FROM app_meta WHERE k=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) ?: def else def
        }
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
            val cur = getMeta(KEY_STUDY_SECONDS, "0").toLongOrNull() ?: 0L
            setMeta(KEY_STUDY_SECONDS, (cur + seconds).toString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun studySeconds(): Long = getMeta(KEY_STUDY_SECONDS, "0").toLongOrNull() ?: 0L

    companion object {
        const val DB_NAME = "user_data.db"
        const val DB_VERSION = 3

        /**
         * app_meta 里唯一在用的键：累计学习秒数。
         * 写成常量而不是散落的字面量，是因为恢复侧也要按这个键判断「值必须是数字」，
         * 两边各写一遍字符串迟早会漂。
         */
        const val KEY_STUDY_SECONDS = "study_seconds"

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

/**
 * 学习记录库的可用性，见 [UserDb.status]。
 *
 * 与 [WordbookStatus] 的差别：词库可以重拷，学习记录不可以 ——
 * 所以这里**没有** RECOVERED 这一档，也不做任何自动修复。
 */
enum class UserDbStatus {
    /** 正常 */
    OK,

    /** 库文件损坏（malformed / not a database / 磁盘 IO 失败）：唯一出路是从备份恢复 */
    CORRUPT,

    /** 其它读失败（权限、被占用等）：可以稍后再试 */
    UNAVAILABLE
}
