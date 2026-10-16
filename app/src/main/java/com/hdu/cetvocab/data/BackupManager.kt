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
        context.getSharedPreferences(WordDb.PREFS_NAME, Context.MODE_PRIVATE)

    // 用 DateTimeFormatter 而不是 SimpleDateFormat：前者不可变、线程安全，
    // 可以安全地作为字段复用。SimpleDateFormat 非线程安全，多线程并发导出时
    // 会输出错乱的时间甚至抛异常。
    // 文件名精确到秒：原来只到分钟，同一分钟内连点两次「备份数据」会互相覆盖，
    // 静默丢掉一份备份。
    private val fileStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val timeText = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun stamp(): String = fileStamp.format(LocalDateTime.now())

    /**
     * 生成目录内不重名的文件。
     *
     * 时间戳只到秒，同一秒内的两次操作（快速连点、或脚本批量导出）会算出同一个路径，
     * 而 `writeText` 是先 truncate 再写、并非原子操作 —— 轻则静默覆盖一份备份，
     * 重则两个写线程交错出一个「半截 JSON」，而它还会出现在 listBackups() 里
     * 被当成可恢复的备份。这里遇到重名就追加序号，保证每次落盘都是独立文件。
     */
    private fun uniqueFile(dir: File, base: String, ext: String): File {
        var index = 1
        while (true) {
            val name = if (index == 1) "$base.$ext" else "$base($index).$ext"
            val file = File(dir, name)
            if (!file.exists()) return file
            index++
        }
    }

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
        val file = uniqueFile(exportDir(), "学习记录_${stamp()}", "csv")
        // 单趟把行读进内存（每行 6 个数值，最多数千行），再一次性批量取词名。
        // 早期实现是每行调一次 wordDb.wordById()，上万条状态时导出要走上万次
        // rawQuery（每次还映射 15 列），导出会明显卡住。
        data class Row(val wordId: Int, val level: Int, val wrong: Int, val fav: Int,
                       val lastTs: Long, val nextTs: Long)
        val rows = ArrayList<Row>()
        userDb.readableDatabase.rawQuery(
            "SELECT word_id,level,wrong_count,fav,last_ts,next_review_ts " +
                "FROM word_state ORDER BY last_ts DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                rows.add(Row(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3),
                    c.getLong(4), c.getLong(5)))
            }
        }
        val wordsById = HashMap<Int, String>()
        wordDb.wordsByIds(rows.map { it.wordId }).forEach { wordsById[it.id] = it.word }

        val sb = StringBuilder()
        sb.append("word,level,wrong_count,fav,last_study,next_review\n")
        rows.forEach { r ->
            sb.append(csvCell(wordsById[r.wordId] ?: "#${r.wordId}")).append(',')
                .append(r.level).append(',')
                .append(r.wrong).append(',')
                .append(r.fav).append(',')
                .append(csvCell(formatTs(r.lastTs))).append(',')
                .append(csvCell(formatTs(r.nextTs))).append('\n')
        }
        file.writeText(sb.toString())
        prune(exportDir(), ".csv")
        return file
    }

    /** 导出收藏的单词本，按字母序 */
    fun exportFavCsv(): File {
        val file = uniqueFile(exportDir(), "单词本_${stamp()}", "csv")
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
        val file = uniqueFile(backupDir(), "${BACKUP_PREFIX}${stamp()}", "json")
        val root = JSONObject()
        root.put(KEY_APP, APP_ID)
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
            "quiz_wrong",
            queryArray("SELECT stage,kind,word_id,wrong_count,last_ts FROM quiz_wrong") { c ->
                JSONObject().apply {
                    put("stage", c.getInt(0))
                    put("kind", c.getInt(1))
                    put("word_id", c.getInt(2))
                    put("wrong_count", c.getInt(3))
                    put("last_ts", c.getLong(4))
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
                put("animations_enabled", prefs.getBoolean(KEY_ANIM, true))
                put("sound_enabled", prefs.getBoolean(KEY_SOUND, true))
            }
        )
        file.writeText(root.toString(2))
        prune(backupDir(), ".json")
        return file
    }

    // ---------------- 恢复 ----------------

    /** 列出备份目录下所有 JSON 备份，最新的在前 */
    fun listBackups(): List<File> =
        backupDir().listFiles { f ->
            f.isFile && f.name.startsWith(BACKUP_PREFIX) && f.name.endsWith(".json", ignoreCase = true)
        }
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

        // ---------- 1. 校验：不通过则直接抛出，此时一行数据都还没删 ----------
        // 必须是「本产品导出的备份」，而不是目录下随便一个 JSON。
        // 只校验版本号是不够的：缺 db_version 字段的 JSON 会被 optInt 读成 0，
        // 照样通过 `0 <= DB_VERSION`，然后后面的 DELETE 就会把五张表清空、
        // 再插入 0 行 —— 用户进度、成绩、成就全灭，界面却提示「恢复成功」。
        require(root.optString(KEY_APP) == APP_ID) {
            "「${file.name}」不是「过四斩六」的备份文件，已中止恢复（现有数据未做任何改动）"
        }
        require(root.has("word_state") && root.has("study_log") && root.has("quiz_record")) {
            "「${file.name}」缺少必要的备份数据段，可能已损坏，已中止恢复（现有数据未做任何改动）"
        }
        /*
         * has() 只看键名，不看值。`"word_state": null`、`"word_state": {}`、
         * `"word_state": [1,2]` 全都能通过上面那道检查，而 optJSONArray 对它们
         * 一律返回 null，objects() 又把它变成空列表 —— 于是走到第 2 步无条件
         * DELETE 五张表、再插入 0 行，最后提示「已恢复学习数据」。
         * 手改过、被截断过、或者拿别的 App 同名键拼出来的 JSON，都会把用户进度清零。
         * 所以必须确认这三段**真的解析成了数组**才允许继续。
         */
        val stateArr = root.optJSONArray("word_state")
        val logArr = root.optJSONArray("study_log")
        val quizArr = root.optJSONArray("quiz_record")
        require(stateArr != null && logArr != null && quizArr != null) {
            "「${file.name}」的数据段格式不是预期的数组，已中止恢复（现有数据未做任何改动）"
        }
        // 空数组同样是「合法但会把数据清光」的输入：一份没有任何学习记录的备份
        // 与「损坏到记录段被抹掉」的备份在结构上无法区分，因此要求至少有一段非空。
        require(stateArr.length() + logArr.length() + quizArr.length() > 0) {
            "「${file.name}」里没有任何学习记录、闯关成绩或收藏，恢复它会清空现有数据，已中止。" +
                "如果确实要清空，请用「设置→清空学习数据」。"
        }
        /*
         * 错题集是**可选段**：v1.4.3 之前的备份里没有它，缺段必须照常恢复成功，
         * 不能按「缺少必要数据段」中止。但只要这个键真写了值，就必须是数组 ——
         * 写成 `"quiz_wrong": {}` 之类还照样往下走，就会落回上面那段注释描述的
         * 「DELETE 全部 + 插入 0 行」陷阱：错题集被静默清空却提示恢复成功。
         * 显式 null 与缺段同等对待（视为「没有待重做错题」），不会误伤老备份。
         */
        val wrongRaw = root.opt("quiz_wrong")
        require(wrongRaw == null || wrongRaw == JSONObject.NULL || wrongRaw is JSONArray) {
            "「${file.name}」的错题集数据段格式不是预期的数组，已中止恢复（现有数据未做任何改动）"
        }
        val wrongArr = wrongRaw as? JSONArray
        val backupVersion = root.optInt("db_version", 0)
        require(backupVersion <= UserDb.DB_VERSION) {
            "这份备份的数据格式版本（v${backupVersion}）比当前应用支持的（v${UserDb.DB_VERSION}）更新，" +
                "请先升级「过四斩六」再恢复"
        }

        // 词库随版本更新，旧备份里的 word_id 可能已不存在于当前词库。
        // 这类「孤儿记录」永远不会出现在任何列表里，却会占用 totalLearned 等统计口径，
        // 所以这里先取当前词库的全部合法 id，恢复时把无效条目剔除。
        val validIds = (
            wordDb.idsOfStage(WordDb.STAGE_CET4) + wordDb.idsOfStage(WordDb.STAGE_CET6)
            ).toHashSet()

        val allStates = stateArr.objects()
        val states = allStates.filter { it.optInt("word_id", -1) in validIds }
        val allLogs = logArr.objects()
        val logs = allLogs.filter { it.optInt("word_id", -1) in validIds }
        val quizzes = quizArr.objects()
        val skipped = (allStates.size - states.size) + (allLogs.size - logs.size)

        // ---------- 2. 写回：整段包在一个事务里，中途失败会自动回滚 ----------
        val db = userDb.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM word_state")
            db.execSQL("DELETE FROM study_log")
            db.execSQL("DELETE FROM quiz_record")
            // 错题集无条件清空：老备份没有这一段时，语义就是「恢复后没有待重做错题」，
            // 保留恢复前的残留会让用户以为老备份里也带着错题。
            db.execSQL("DELETE FROM quiz_wrong")
            db.execSQL("DELETE FROM achievement")
            db.execSQL("DELETE FROM app_meta")

            states.forEach { o ->
                db.insertWithOnConflict(
                    "word_state", null,
                    ContentValues().apply {
                        put("word_id", o.optInt("word_id"))
                        /*
                         * 数值必须夹回合法区间。optInt 只会「读不出来给默认值」，
                         * 不会拦下读得出来但非法的值：手改一个 level=7 之后，
                         * 这条记录既不满足复习查询的 `level BETWEEN 1 AND 5`，
                         * 也不满足新词查询的 `level = 0`，那个词就从复习和新词
                         * 两头同时消失，只能回词库重学 —— 而用户看不出任何异常。
                         */
                        put("level", clampInt(o.optInt("level"), 0, 5))
                        put("wrong_count", clampInt(o.optInt("wrong_count"), 0, Int.MAX_VALUE))
                        put("fav", if (clampInt(o.optInt("fav"), 0, 1) == 1) 1 else 0)
                        put("last_ts", o.optLong("last_ts").coerceAtLeast(0L))
                        put("next_review_ts", o.optLong("next_review_ts").coerceAtLeast(0L))
                        put("ease", clampDouble(o.optDouble("ease", 2.5), 1.3, 2.8))
                        put("reps", clampInt(o.optInt("reps"), 0, Int.MAX_VALUE))
                        put("interval_days", clampInt(o.optInt("interval_days"), 0, Int.MAX_VALUE))
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
                        put("correct", if (clampInt(o.optInt("correct"), 0, 1) == 1) 1 else 0)
                        put("day", o.optString("day"))
                        put("ts", o.optLong("ts").coerceAtLeast(0L))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            quizzes.forEach { o ->
                db.insertWithOnConflict(
                    "quiz_record", null,
                    ContentValues().apply {
                        // 闯关记录同样要夹：correct > total 会让「正确率」算出 130%，
                        // 三星判定和 QUIZ_PERFECT 都会被这种值骗到
                        val total = clampInt(o.optInt("total"), 0, Int.MAX_VALUE)
                        put("stage", clampInt(o.optInt("stage"), 1, 2))
                        /*
                         * 下界必须是 [QUIZ_SCOPE_WRONG]（-103），不能图省事写 -1。
                         * 早先这里写的是 clampInt(unit, -1, MAX)：拓展练习的记录 unit 是
                         * -101 / -102，一次备份恢复就把它们统统夹成 -1，
                         * 于是「易混词辨析做过 6 次」变成「全词挑战做过 6 次」并直接冒进排行榜
                         * ——而这些记录本来是被 RANKED_ONLY 排除在排名口径之外的。
                         */
                        put("unit", clampInt(o.optInt("unit"), QUIZ_SCOPE_WRONG, Int.MAX_VALUE))
                        put("mode", o.optString("mode"))
                        put("score", clampInt(o.optInt("score"), 0, Int.MAX_VALUE))
                        put("total", total)
                        put("correct", clampInt(o.optInt("correct"), 0, total))
                        put("max_combo", clampInt(o.optInt("max_combo"), 0, total))
                        put("duration_ms", o.optLong("duration_ms").coerceAtLeast(0L))
                        put("ts", o.optLong("ts").coerceAtLeast(0L))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            // 错题集回填：只收「当前词库里还在的词」和「重做功能真会出的两种题型」。
            // 题型限定在 MEANING / SPELLING：易混词与真题翻译从不进错题集，
            // 备份里冒出别的 kind 只能是被改过的值，收进来只会变成出不了题的死记录。
            wrongArr.objects()
                .filter { it.optInt("word_id", -1) in validIds }
                .filter { it.optInt("kind", -1) in 0..QuizKind.SPELLING.ordinal }
                .forEach { o ->
                    db.insertWithOnConflict(
                        "quiz_wrong", null,
                        ContentValues().apply {
                            put("stage", clampInt(o.optInt("stage"), 1, 2))
                            put("kind", o.optInt("kind"))
                            put("word_id", o.optInt("word_id"))
                            // 至少 1 次：夹成 0 会让「错得多的先练」的排序把这条排到最后，
                            // 而它其实一次都没错，属于被改坏的数据
                            put("wrong_count", clampInt(o.optInt("wrong_count"), 1, Int.MAX_VALUE))
                            put("last_ts", o.optLong("last_ts").coerceAtLeast(0L))
                        },
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            // 成就码必须过一遍当前版本的定义表再回填。
            // word_state / study_log 都做了有效性校验，成就却原样接收任意 code：
            // 恢复带回当前版本没有的成就码时，成就墙会显示「已解锁 100 / 81 个成就」，
            // 而进度条用的 fraction 大于 1 会直接触发 require 断言崩溃。
            val knownCodes = Achievements.ALL.mapTo(HashSet()) { it.code }
            root.optJSONArray("achievement").objects()
                .filter { it.optString("code") in knownCodes }
                .forEach { o ->
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
                if (p.has(KEY_ANIM)) {
                    putBoolean(KEY_ANIM, p.optBoolean(KEY_ANIM, true))
                }
                if (p.has(KEY_SOUND)) {
                    putBoolean(KEY_SOUND, p.optBoolean(KEY_SOUND, true))
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
    /** 读出来是整数但可能越界的字段统一夹一刀，NaN/负数/超大值都落回合法区间 */
    private fun clampInt(v: Int, min: Int, max: Int): Int = v.coerceIn(min, max)

    private fun clampDouble(v: Double, min: Double, max: Double): Double =
        if (v.isNaN() || v.isInfinite()) 2.5 else v.coerceIn(min, max)

    private fun JSONArray?.objects(): List<JSONObject> {        if (this == null) return emptyList()
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
        /** 备份文件的身份标识字段与取值：恢复时据此判断「这是不是本产品的备份」 */
        private const val KEY_APP = "app"
        private const val APP_ID = "过四斩六"

        /** 备份文件名前缀，[listBackups] 据此过滤，避免把目录里其它 .json 当成备份 */
        private const val BACKUP_PREFIX = "cetvocab_backup_"

        private const val KEY_STAGE = "stage"
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_AUTOSPEAK = "auto_speak"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_RATE = "speech_rate"
        private const val KEY_UNIT = "current_unit"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ANIM = "animations_enabled"
    }
}
