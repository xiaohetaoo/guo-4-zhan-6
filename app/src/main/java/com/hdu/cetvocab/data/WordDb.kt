package com.hdu.cetvocab.data

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import java.io.File
import java.io.FileOutputStream

/**
 * 内置只读词库。
 * 词库以 SQLite 文件形式随安装包分发，首次启动时从 assets 复制到私有目录，
 * 之后所有查询都在本地完成，全程不需要网络。
 */
class WordDb(private val context: Context) {

    @Volatile
    private var db: SQLiteDatabase? = null

    /**
     * 词库可用性。之所以要把它做成一个状态而不是「抛出去就算了」：
     * 首页 / 词库 / 闯关都只读词库，坏了以后界面显示的是「词库尚未就绪」，
     * 用户会反复重启应用，而正确动作是重新安装 —— 这句话必须由某处说出来。
     */
    @Volatile
    var status: WordbookStatus = WordbookStatus.OK
        private set

    /** 词库副本损坏且重拷也失败：需要重新安装，不是「稍后再试」 */
    class WordbookUnreadableException(cause: Throwable?) :
        Exception("内置词库无法读取", cause)

    /**
     * 词库指纹：分级词数 + 一个廉价的内容摘要。
     *
     * 备份 JSON 里带上它，恢复时才能判断「这份备份是不是给当前这版词库做的」。
     * 摘要用几条聚合值而不是逐行哈希：词库 1.1 万行，备份是用户点了就要看到结果的动作，
     * 不该为了算指纹全表扫一遍；而「词数 + id 之和 + 词长之和 + 释义长度之和」
     * 已经能区分开任何一次实质性的词表变更（增删词、改释义都会动到至少一项）。
     */
    data class WordbookFingerprint(
        val cet4: Int,
        val cet6: Int,
        val total: Int,
        val digest: String
    ) {
        /** 与另一份指纹是否指向同一版词库；对方为 null（老备份没写）时按「未知」处理 */
        fun matches(other: WordbookFingerprint?): Boolean {
            if (other == null) return false
            return cet4 == other.cet4 && cet6 == other.cet6 &&
                total == other.total && digest == other.digest
        }

        fun describe(): String = "四级${cet4}词/六级${cet6}词"
    }

    /**
     * 当前词库的指纹；词库读不出来时返回 null。
     *
     * 这里不抛异常：调用方是「导出备份」和「恢复备份」，它们跑在协程里，
     * 词库损坏时应该导出一份「没有指纹」的备份（恢复时退化成不做校验），
     * 而不是让用户连自己的学习记录都备份不出去。
     */
    fun fingerprintOrNull(): WordbookFingerprint? = reading(null) { d ->
        var cet4 = 0
        var cet6 = 0
        var total = 0
        var sumId = 0L
        var sumWord = 0L
        var sumTrans = 0L
        d.rawQuery(
            "SELECT COUNT(*), SUM(id), SUM(LENGTH(word)), SUM(LENGTH(translation)), " +
                "SUM(CASE WHEN stage=? THEN 1 ELSE 0 END), SUM(CASE WHEN stage=? THEN 1 ELSE 0 END) " +
                "FROM words",
            arrayOf(STAGE_CET4.toString(), STAGE_CET6.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                total = c.getInt(0); sumId = c.getLong(1); sumWord = c.getLong(2)
                sumTrans = c.getLong(3); cet4 = c.getInt(4); cet6 = c.getInt(5)
            }
        }
        // 混合成 8 位十六进制：只要各项都相同就认为同一版词库
        val mixed = (sumId * 31) xor (sumWord * 131) xor (sumTrans * 8191) xor total.toLong()
        WordbookFingerprint(cet4, cet6, total, String.format("%08x", mixed))
    }

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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val installed = prefs.getInt(PREFS_DB_VERSION_KEY, 0)
            if (!out.exists() || installed != DB_VERSION) installCopy(out, prefs)
            return try {
                SQLiteDatabase.openDatabase(out.path, null, SQLiteDatabase.OPEN_READONLY)
                    .also { db = it; status = WordbookStatus.OK }
            } catch (e: Exception) {
                /*
                 * 打不开时唯一值得单独处理的情况：私有目录里那份**副本**坏了。
                 * 原件在 assets 里、由安装包保证，副本删掉重拷就能恢复。
                 * 关键是必须连 [PREFS_DB_VERSION_KEY] 一起清掉 —— 只删文件的话，
                 * 标记仍写着「已装好」，下次冷启动判定不需要重拷，
                 * 于是每次开 App 都在同一个异常上崩，用户只能去系统设置里清数据。
                 */
                val recoverable = e is SQLiteDatabaseCorruptException ||
                    e is SQLiteDiskIOException ||
                    (e is SQLiteException &&
                        e.message?.contains("not a database", true) == true)
                if (!recoverable) throw WordbookUnreadableException(e)
                forceReinstall()
                val again = runCatching {
                    installCopy(out, prefs)
                    SQLiteDatabase.openDatabase(out.path, null, SQLiteDatabase.OPEN_READONLY)
                }
                // 重拷也失败（assets 原件本身有问题 / 存储写不进去）：
                // 这才是真正需要「重新安装」的场景，抛带类型让 UI 给准确文案
                again.getOrNull()?.let {
                    db = it
                    status = WordbookStatus.RECOVERED
                    return it
                }
                throw WordbookUnreadableException(again.exceptionOrNull() ?: e)
            }
        }
    }

    /**
     * 丢弃当前连接与私有目录里那份副本，并清掉安装标记，
     * 使下一次 [open] 必然走重拷。
     *
     * 必须在标记上也动手：只关连接不删标记的话，[open] 看到标记仍是当前版本，
     * 会直接把同一份坏文件再打开一次，损坏状态永远出不去。
     */
    private fun forceReinstall() {
        runCatching { db?.close() }
        db = null
        runCatching { File(context.filesDir, DB_NAME).delete() }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREFS_DB_VERSION_KEY).commit()
    }

    /**
     * 词库读取的统一入口：任何一次读失败都**降级返回 [fallback]**，
     * 同时把 [status] 标成 [WordbookStatus.CORRUPT]，由界面层负责说出来。
     *
     * 为什么不能直接往外抛：这些查询的调用方全部跑在 `viewModelScope` 里，
     * 而 viewModelScope 没配 CoroutineExceptionHandler —— 未捕获异常会直接
     * 结束协程并让应用崩溃（首页刷新路径上就是「一点图标就闪退」），
     * 用户既看不到原因也没有重试按钮。降级 + 状态位是把崩溃换成一句人话
     * （「请重新安装本应用」）的唯一办法，且学习记录在另一个库里并不会丢。
     *
     * 还有第二种损坏：连接能正常打开、读到一半才报 malformed。此时
     * [open] 里的 `db?.let` 会把这个坏连接一直复用下去，所以必须主动
     * [forceReinstall] 再试一次，而不是原样抛出。
     */
    private fun <T> reading(fallback: T, block: (SQLiteDatabase) -> T): T =
        try {
            block(open())
        } catch (e: WordbookUnreadableException) {
            status = WordbookStatus.CORRUPT
            fallback
        } catch (e: Exception) {
            val corrupt = e is SQLiteDatabaseCorruptException ||
                e is SQLiteDiskIOException ||
                (e is SQLiteException && e.message?.contains("malformed", true) == true)
            if (!corrupt) throw e
            forceReinstall()
            try {
                block(open())
            } catch (again: WordbookUnreadableException) {
                status = WordbookStatus.CORRUPT
                fallback
            }
        }

    /**
     * 把 assets 里的词库原子地装到私有目录。
     *
     * 拷贝必须是「写临时文件 → fsync → rename → 最后才写标记」。
     *
     * 旧写法是 `out.outputStream()` 直接覆写目标文件，写完立刻把标记置成
     * DB_VERSION 并 apply()。apply() 只是把数据排进磁盘队列，Android 的
     * 写回缓存意味着「标记已落盘、词库内容还没落盘」是真实可达的状态：
     * 首启拷到一半断电或被迫重启后，标记显示「已装好」，磁盘上却是半截
     * words.db，之后每次冷启动都判定不需要重装，首个查询就抛异常，
     * 而 viewModelScope 没有异常处理器 —— 结果是不可逆的无限闪退循环，
     * 用户只能去系统设置里清除数据。
     *
     * 现在：内容没 fsync 完就不 rename，没 rename 就不写标记。
     * 任何一步被打断，下次启动看到的仍是「标记 != 版本」，会自动重拷。
     */
    private fun installCopy(out: File, prefs: SharedPreferences) {
        val tmp = File(context.filesDir, "$DB_NAME.tmp")
        context.assets.open(DB_NAME).use { input ->
            FileOutputStream(tmp).use { output ->
                input.copyTo(output, 128 * 1024)
                output.flush()
                output.fd.sync()
            }
        }
        if (!tmp.renameTo(out)) {
            // rename 失败（极少数文件系统的跨设备情况）时退回原地重写，
            // 但仍以「先拷完再写标记」为不变式，不改变判定顺序
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(out).use { output ->
                    input.copyTo(output, 128 * 1024)
                    output.flush()
                    output.fd.sync()
                }
            }
        }
        tmp.delete()
        prefs.edit().putInt(PREFS_DB_VERSION_KEY, DB_VERSION).commit()
    }

    private fun queryWords(selection: String, args: Array<String>?, orderBy: String?): List<Word> =
        reading(emptyList()) { d ->
            val list = ArrayList<Word>()
            d.rawQuery(
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
            list
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

    /**
     * 词库某一级别的词数。读不出来时返回 0 并置 CORRUPT —— 返回 0 是可接受的降级
     * （首页与进度条会显示「本级 0 词」），抛出则整个刷新协程直接崩。
     */
    fun countOfStage(stage: Int): Int = reading(0) { d ->
        d.rawQuery("SELECT COUNT(*) FROM words WHERE stage=?", arrayOf(stage.toString()))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * 某一级别的全部词 id。
     * 用于按「级别」统计学习进度——学习记录在 user_data.db、词表在 words.db，
     * 是两个独立的 SQLite 库没法 JOIN，只能先取出本级 id 再与学习记录求交集。
     */
    fun idsOfStage(stage: Int): List<Int> = reading(emptyList()) { d ->
        d.rawQuery("SELECT id FROM words WHERE stage=?", arrayOf(stage.toString())).use { c ->
            val ids = ArrayList<Int>(c.count)
            while (c.moveToNext()) ids.add(c.getInt(0))
            ids
        }
    }

    fun unitCount(stage: Int): Int = reading(0) { d ->
        d.rawQuery(
            "SELECT COUNT(DISTINCT unit) FROM words WHERE stage=?", arrayOf(stage.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * 「同一个单词」的代表 id：同一 word 文本在词库里可能有两行
     * （四级 4750 词全部是六级 6248 词的子集），这里把每行都映射到该文本的最小 id。
     *
     * 存在的理由：学习记录表 word_state 以 word_id 为主键，一个词在四级学过、
     * 又在六级再学一次会留下两行。统计与成就如果直接数行数，「累计学习 200 词」
     * 其实只对应 100 个不同的单词，段位和 LEARN_* 成就都会被灌一倍水。
     *
     * 词库和用户数据是两个独立的 SQLite 文件，SQL 层 JOIN 不到一起，
     * 所以在内存里做一次性映射（10998 条，约 200 KB），之后按 id 查表。
     */
    fun canonicalId(id: Int): Int = canonicalMap()[id] ?: id

    /** 把一批 word_id 折算成「不同单词」的个数 */
    fun distinctWordCount(ids: Collection<Int>): Int {
        val map = canonicalMap()
        val seen = HashSet<Int>(ids.size)
        ids.forEach { seen.add(map[it] ?: it) }
        return seen.size
    }

    @Volatile
    private var canonicalIds: Map<Int, Int>? = null

    private fun canonicalMap(): Map<Int, Int> {
        canonicalIds?.let { return it }
        // 词库不可用时返回空表：distinctWordCount 于是退化成「按 id 计数」。
        // 这里不能缓存失败结果 —— 否则本轮会话后续都拿不到去重映射。
        val built = reading<Map<Int, Int>?>(null) { d ->
            val minIdOfWord = HashMap<String, Int>(7000)
            d.rawQuery("SELECT word, MIN(id) FROM words GROUP BY word", null).use { c ->
                while (c.moveToNext()) {
                    val w = c.getString(0) ?: continue
                    minIdOfWord[w] = c.getInt(1)
                }
            }
            val map = HashMap<Int, Int>(12000)
            d.rawQuery("SELECT id, word FROM words", null).use { c ->
                while (c.moveToNext()) {
                    val w = c.getString(1) ?: continue
                    map[c.getInt(0)] = minIdOfWord[w] ?: c.getInt(0)
                }
            }
            minIdOfWord.clear()
            map
        } ?: return emptyMap()
        synchronized(openLock) { canonicalIds = built }
        return built
    }

    /**
     * 按关键词搜索单词（英文或中文释义均可）。
     *
     * 必须带上 `stage`：六级词库完整地包含了四级的 4750 个词（同一 word 在
     * words.db 里有两行，只是 id / stage 不同），不过滤级别的话搜一个常见词
     * 会同时弹出两条一模一样的卡片。
     */
    fun search(keyword: String, stage: Int, limit: Int = SEARCH_LIMIT): List<Word> {
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
     * 题库是 v5 词库新增的表；若设备上仍是旧副本（表不存在），`rawQuery` 会抛
     * SQLiteException。这里按注释所述**真的把它转成空列表**（早期实现只写了注释
     * 没有 try/catch，一旦触发就是 IO 协程里的未捕获异常），
     * 由闯关页给出「题库暂不可用」的提示，而不是让整个 App 崩掉。
     */
    fun translationItems(stage: Int): List<TranslationItem> = reading(emptyList()) { d ->
        val list = ArrayList<TranslationItem>()
        val failure = runCatching {
            d.rawQuery(
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
        }.exceptionOrNull()
        if (failure != null) {
            // 只有「这张表在当前词库副本里不存在」才降级成空列表（旧版本副本的正常状态），
            // 其余异常（文件损坏、磁盘 IO）必须原样抛出去，让 UI 说出正确动作
            val missingTable = failure is SQLiteException &&
                failure.message?.contains("no such table", true) == true
            if (!missingTable) throw failure
        }
        list
    }

    companion object {
        const val DB_NAME = "words.db"

        /**
         * 关键词搜索单次返回的上限。词库有 1.1 万行，搜一个常见字母能命中上千条，
         * 全量加载会让列表明显掉帧。UI 必须在命中数等于这个上限时告诉用户「还有更多」，
         * 否则用户会以为整个级别只有前 60 个词（见 LearnScreen 的结果计数行）。
         */
        const val SEARCH_LIMIT = 60

        /**
         * 应用偏好文件名。**词库安装标记与用户设置共用这一个 prefs 文件**，
         * 所以任何 `edit().clear()` 都必须显式保住 [PREFS_DB_VERSION_KEY]，
         * 否则清完数据下次冷启动会把 7.98 MB 词库从 assets 重新拷一遍（见
         * [VocabRepository.resetAll]）。
         */
        const val PREFS_NAME = "vocab_meta"

        /** 已安装到私有目录的词库版本号标记（与 [DB_VERSION] 比对） */
        const val PREFS_DB_VERSION_KEY = "words_db_version"

        /**
         * v5：新增 translation_quiz（真题句子翻译题库）
         * v6：translation_quiz 扩充至 322 条（四级 161 / 六级 161）
         *
         * 这个号不是 SQLite 的 user_version，而是「已安装到私有目录的词库版本号」：
         * 与 [open] 里记录在 SharedPreferences 的值不一致时才重新从 assets 复制。
         * 因此只要内置词库内容有变（增删改题目），这里就必须 +1，
         * 否则老用户升级后仍用着设备上的旧副本，看不到新题。
         */
        const val DB_VERSION = 6
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

/** 词库可用性，见 [WordDb.status] */
enum class WordbookStatus {
    /** 正常 */
    OK,

    /** 私有目录里的副本损坏，已自动重拷并恢复成功（本轮仅用于诊断，不改变行为） */
    RECOVERED,

    /** 重拷也失败，需要重新安装应用 */
    CORRUPT
}
