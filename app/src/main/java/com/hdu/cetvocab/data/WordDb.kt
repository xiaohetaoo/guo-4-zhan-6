package com.hdu.cetvocab.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * 内置只读词库。
 * 词库以 SQLite 文件形式随安装包分发，首次启动时从 assets 复制到私有目录，
 * 之后所有查询都在本地完成，全程不需要网络。
 */
class WordDb(private val context: Context) {

    @Volatile
    private var db: SQLiteDatabase? = null

    /** 首次打开的互斥锁，见 [open] 说明 */
    private val openLock = Any()

    /**
     * 打开（必要时先释放）内置词库。
     *
     * 这里必须是「加锁的双重检查」，不能只靠 `@Volatile`：
     * `@Volatile` 只保证可见性、不保证互斥。冷启动时 AppViewModel 会并发启动
     * refreshHome / refreshLearn / refreshProfile 三个协程，它们几乎同时首次访问词库，
     * 若不加锁就可能有两个线程同时通过 `db?.let` 检查 —— 结果是
     * ① 重复 openDatabase，先建的那个实例被直接丢弃且永不关闭（native 连接泄漏）；
     * ② 首次安装时并发写同一份 words.db 副本，可能把文件写坏。
     */
    private fun open(): SQLiteDatabase {
        db?.let { if (it.isOpen) return it }
        return synchronized(openLock) {
            db?.let { if (it.isOpen) return it }
            val out = File(context.filesDir, DB_NAME)
            val prefs = context.getSharedPreferences("vocab_meta", Context.MODE_PRIVATE)
            val installed = prefs.getInt("words_db_version", 0)
            if (!out.exists() || installed != DB_VERSION) {
                context.assets.open(DB_NAME).use { input ->
                    out.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
                }
                prefs.edit().putInt("words_db_version", DB_VERSION).apply()
            }
            SQLiteDatabase.openDatabase(out.path, null, SQLiteDatabase.OPEN_READONLY)
                .also { db = it }
        }
    }

    private fun queryWords(selection: String, args: Array<String>?, orderBy: String?): List<Word> {
        val list = ArrayList<Word>()
        open().rawQuery(
            "SELECT id,word,phonetic,translation,trans_detail,definition,pos,stage,unit,rank," +
                "sentence,sentence_cn,roots,similar,mnemonic FROM words " +
                "WHERE $selection " + (orderBy?.let { "ORDER BY $it" } ?: ""),
            args
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Word(
                        id = c.getInt(0),
                        word = c.getString(1) ?: "",
                        phonetic = c.getString(2) ?: "",
                        translation = c.getString(3) ?: "",
                        transDetail = c.getString(4) ?: "",
                        definition = c.getString(5) ?: "",
                        pos = c.getString(6) ?: "",
                        stage = c.getInt(7),
                        unit = c.getInt(8),
                        rank = c.getInt(9),
                        sentence = c.getString(10) ?: "",
                        sentenceCn = c.getString(11) ?: "",
                        roots = c.getString(12) ?: "",
                        similar = c.getString(13) ?: "",
                        mnemonic = c.getString(14) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun wordsOfUnit(stage: Int, unit: Int): List<Word> =
        queryWords("stage=? AND unit=?", arrayOf(stage.toString(), unit.toString()), "\"rank\"")

    fun wordsOfStage(stage: Int): List<Word> =
        queryWords("stage=?", arrayOf(stage.toString()), "\"rank\"")

    /** 按首字母取词（字母不分大小写），用于「按首字母闯关」 */
    fun wordsByInitial(stage: Int, letter: Char): List<Word> =
        queryWords(
            "stage=? AND UPPER(substr(word,1,1))=?",
            arrayOf(stage.toString(), letter.uppercaseChar().toString()),
            "\"rank\""
        )

    fun wordById(id: Int): Word? =
        queryWords("id=?", arrayOf(id.toString()), null).firstOrNull()

    /**
     * 按单词原文精确取词，用于给「真题句子翻译」题挂上关键词词条
     * （拿到音标与发音能力）。查不到时返回 null，由调用方降级处理。
     */
    fun wordByText(text: String, stage: Int): Word? =
        queryWords(
            "stage=? AND word=?",
            arrayOf(stage.toString(), text),
            null
        ).firstOrNull()

    /**
     * 按 id 批量取词。
     *
     * 必须分批：`id IN (?,?,…)` 的占位符数量等于 id 个数，而 SQLite 的
     * `SQLITE_MAX_VARIABLE_NUMBER` 在较老的 Android 上默认只有 999。
     * 待复习词 / 收藏词是会随使用时间增长的，超过上限时 rawQuery 会抛
     * `SQLiteException: too many SQL variables`，而调用方是首页刷新，
     * 等于一进 App 就崩。这里按 500 一批切开再合并，长度不再受限。
     */
    fun wordsByIds(ids: List<Int>): List<Word> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(SQL_VAR_BATCH).flatMap { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            queryWords(
                "id IN ($placeholders)",
                chunk.map { it.toString() }.toTypedArray(),
                null
            )
        }
    }

    fun countOfStage(stage: Int): Int =
        open().rawQuery("SELECT COUNT(*) FROM words WHERE stage=?", arrayOf(stage.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /**
     * 某一级别的全部词 id。
     * 用于按「级别」统计学习进度——学习记录在 user_data.db、词表在 words.db，
     * 是两个独立的 SQLite 库没法 JOIN，只能先取出本级 id 再与学习记录求交集。
     */
    fun idsOfStage(stage: Int): List<Int> =
        open().rawQuery("SELECT id FROM words WHERE stage=?", arrayOf(stage.toString())).use { c ->
            val ids = ArrayList<Int>(c.count)
            while (c.moveToNext()) ids.add(c.getInt(0))
            ids
        }

    fun unitCount(stage: Int): Int =
        open().rawQuery(
            "SELECT COUNT(DISTINCT unit) FROM words WHERE stage=?", arrayOf(stage.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * 按关键词搜索单词（英文或中文释义均可）。
     *
     * 必须带上 `stage`：六级词库完整地包含了四级的 4750 个词（同一 word 在
     * words.db 里有两行，只是 id / stage 不同），不过滤级别的话搜一个常见词
     * 会同时弹出两条一模一样的卡片。
     */
    fun search(keyword: String, stage: Int, limit: Int = 60): List<Word> {
        // LIKE 里的 % 与 _ 是通配符，用户直接输入这两个字符会被当成模式匹配，
        // 这里用 ESCAPE 转义，让它们按字面量匹配。
        val escaped = keyword.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val k = "%$escaped%"
        return queryWords(
            "stage=? AND (word LIKE ? ESCAPE '\\' OR translation LIKE ? ESCAPE '\\')",
            arrayOf(stage.toString(), k, k),
            "word LIMIT $limit"
        )
    }

    /** 随机取词，用于生成干扰项 */
    fun randomWords(stage: Int, limit: Int): List<Word> =
        queryWords("stage=?", arrayOf(stage.toString()), "RANDOM() LIMIT $limit")

    /**
     * 取某一级别的「真题句子翻译」题库。
     *
     * 题库是 v5 词库新增的表；若用户设备上仍是旧副本（表不存在），
     * 这里会抛 SQLiteException，统一转成空列表，让闯关页给出「题库暂不可用」
     * 的提示，而不是让整个 App 崩在启动阶段。
     */
    fun translationItems(stage: Int): List<TranslationItem> {
        val list = ArrayList<TranslationItem>()
        open().rawQuery(
            "SELECT id,stage,keyword,chinese,opt_a,opt_b,opt_c,opt_d,correct,analysis " +
                "FROM translation_quiz WHERE stage=? ORDER BY id",
            arrayOf(stage.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val correct = (c.getString(8) ?: "").split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 0..3 }
                    .toSet()
                // 解析不出任何正确项的数据视为脏数据，直接丢弃，避免出一道无解题
                if (correct.isEmpty()) continue
                list.add(
                    TranslationItem(
                        id = c.getInt(0),
                        stage = c.getInt(1),
                        keyword = c.getString(2) ?: "",
                        chinese = c.getString(3) ?: "",
                        options = listOf(
                            c.getString(4) ?: "",
                            c.getString(5) ?: "",
                            c.getString(6) ?: "",
                            c.getString(7) ?: ""
                        ),
                        correct = correct,
                        analysis = c.getString(9) ?: ""
                    )
                )
            }
        }
        return list
    }

    companion object {
        const val DB_NAME = "words.db"
        /** v5：新增 translation_quiz（真题句子翻译题库） */
        const val DB_VERSION = 5
        const val STAGE_CET4 = 1
        const val STAGE_CET6 = 2

        /**
         * 单条 SQL 里最多绑定多少个 id。
         * 取 500 是为了在 SQLite 的 999 变量上限（老设备默认值）之下留足余量，
         * 同时避免分批过碎导致查询次数膨胀。
         */
        private const val SQL_VAR_BATCH = 500

        fun stageLabel(stage: Int) = if (stage == STAGE_CET6) "六级" else "四级"
    }
}
