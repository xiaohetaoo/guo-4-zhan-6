package com.hdu.cetvocab.data

/** 单词（来自内置只读词库） */
data class Word(
    val id: Int,
    val word: String,
    val phonetic: String,
    val translation: String,
    /** 完整中文释义（多义项，按行分隔） */
    val transDetail: String,
    val definition: String,
    val pos: String,
    /** 1 = 四级大纲，2 = 六级大纲 */
    val stage: Int,
    /** 阶段内单元号（每 50 词一单元）。分组依据见 [rank]：按字母序切块，不是官方词表顺序 */
    val unit: Int,
    /**
     * 阶段内序号，从 1 开始。
     *
     * **按单词字母序**编号，不是官方词表顺序、也不是词频：
     * `tools/build_words_db.py` 里 `ordered = sorted(words)` 之后 `rank = i + 1`。
     * 界面（单词详情页的「字母序第 N 位」）与出题排序都按这个口径写，
     * 别把它读成「第 N 高频词」—— 排序规则一改，rank 的含义就跟着变，
     * 而闯关重放依赖「同一 seed + 同一 rank 顺序 → 同一题序」，注释与实际必须一致。
     */
    val rank: Int,
    val sentence: String,
    val sentenceCn: String,
    /** 词根词缀拆解 */
    val roots: String,
    /** 形近词，格式：word|释义;;word|释义 */
    val similar: String,
    /** 记忆提示（谐音 / 联想） */
    val mnemonic: String
) {
    val similarList: List<Pair<String, String>>
        get() = similar.split(";;")
            .mapNotNull { item ->
                if (item.isBlank()) return@mapNotNull null
                val parts = item.split("|")
                if (parts.size >= 2) parts[0] to parts[1] else null
            }
}

/** 用户对单词的熟练度状态 */
data class WordState(
    val wordId: Int,
    /**
     * 熟练度等级，共 6 档，文案见
     * [com.hdu.cetvocab.data.VocabRepository.levelLabel]：
     * 0 未学 / 1 初识 / 2 认识 / 3 熟悉 / 4 掌握 / 5 精通。
     * 统计位说的「熟悉以上」按 `level >= 3` 起算，错词本在 `level == 5` 时把词移出。
     */
    val level: Int = 0,
    val wrongCount: Int = 0,
    val fav: Boolean = false,
    val lastTs: Long = 0L,
    val nextReviewTs: Long = 0L,
    // ---- SM-2 间隔重复参数 ----
    val ease: Double = 2.5,       // 难度因子，越小表示越难记
    val reps: Int = 0,            // 连续答对的次数
    val intervalDays: Int = 0     // 当前复习间隔（天）
)

/** 一次闯关成绩 */
data class QuizRecord(
    val id: Long,
    val stage: Int,
    val unit: Int,
    val mode: String,
    val score: Int,
    val total: Int,
    val correct: Int,
    val maxCombo: Int,
    val durationMs: Long,
    val ts: Long
) {
    val accuracy: Int get() = percentOf(correct, total)
}

/**
 * 闯关成绩的聚合口径（排行榜页顶部三块统计用）。
 *
 * 必须由 SQL 在**全表**上算，不能拿榜单列表（最多 50 条）现算：
 * 超过 50 次之后，「平均分」与「最高正确率」就只在最近/最高的那 50 条里统计，
 * 数字会与「共 N 次闯关记录」自相矛盾。同样只统计「参与排名」的记录。
 */
data class QuizAggregate(
    /** 参与排名的闯关次数 */
    val count: Int = 0,
    /** 历史最高分 */
    val bestScore: Int = 0,
    /** 平均分（整数，向下取整） */
    val avgScore: Int = 0,
    /** 最高正确率（百分比整数） */
    val bestAccuracy: Int = 0
)

/**
 * 闯关范围编码（`QuizUiState.unit` 与 `QuizRecord.unit` 共用同一套约定）：
 * - `>= 1`    → 单元挑战，值即单元号
 * - `-1`      → 全阶段全词挑战
 * - `-2..-27` → 首字母挑战，字母索引 = `-unit - 2`（0 = A，25 = Z）
 * - `-101`    → 易混词辨析
 * - `-102`    → 真题句子翻译
 * - `-103`    → 错题重做
 *
 * 两个专题关卡刻意取 -101 / -102：首字母最多只用到 -27，
 * 中间留出大段空档，后续再加专题也不会和已有编码撞车。
 */
const val QUIZ_SCOPE_ALL = -1
const val QUIZ_SCOPE_CONFUSABLE = -101
const val QUIZ_SCOPE_TRANSLATION = -102

/**
 * 错题重做的范围值。
 *
 * 不进 [EXTENSION_QUIZ_SCOPES]：那个列表的用途是「照常入库但不参与排名」，
 * 而重做局根本不写 quiz_record（见 [QUIZ_SPEC_WRONG]），
 * 混进去反而会让「拓展练习做过哪些关」这类按 unit 统计的查询多认一个假范围。
 */
const val QUIZ_SCOPE_WRONG = -103

/**
 * 全词挑战的题量。单元 / 首字母 / 两个专题都是 10 题，只有这一关拉长到 50 题。
 *
 * 之所以是 50 而不是 30：连击类成就最高档要求「单次闯关连对 50 题」，
 * 30 题的封顶会让它成为永远无法解锁的死成就。选关页的说明文案也引用这个常量，
 * 避免题量改了、文案还说 30 题。
 */
const val STAGE_QUIZ_COUNT = 50

/** 普通关卡（单元 / 首字母 / 专题）的题量 */
const val UNIT_QUIZ_COUNT = 10

/**
 * 正确率的唯一口径：四舍五入到整数百分比。
 *
 * 之前结算页、学习页、排行榜各写各的：前两处用整数除法（截断），
 * 排行榜的「最高正确率」用 SQL 的 `correct * 100 / total`（也是截断），
 * 首页的本级进度却用 roundToInt。同一份答题数据在不同页面能差出 1 个百分点
 * （99.5% 一边显示 99、一边显示 100），而 100% 是「满分通关」成就的判据，
 * 差这 1 会被用户当成判分错了。
 */
fun percentOf(part: Int, total: Int): Int =
    if (total <= 0) 0 else kotlin.math.round(part * 100.0 / total).toInt()

/**
 * 用时的统一写法。
 *
 * 结算页原来写「123s」（英文单位），排行榜写「123 秒」，
 * 同一局的成绩在两个页面出现两种单位。超过一分钟统一折成「分+秒」。
 */
fun durationText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s < 60) "${s}秒" else "${s / 60}分${s % 60}秒"
}


/**
 * 错词本强化一次最多取多少个词。
 *
 * 词库页「错词本」分段显示的列表长度与「开始强化错词」实际装入队列的长度
 * 必须是同一个常量。此前一个是 40、一个是 50：错词超过 40 个时，
 * 页面上写「共 40 个单词」，学习页的进度却是「1 / 50」，
 * 用户会以为凭空多出 10 个没见过的词。
 */
const val WRONG_STUDY_LIMIT = 50

/**
 * 拓展资源练习：入口在「我的」页成就卡下方，定位是**练习**而不是闯关。
 *
 * 不参与排行榜排名 —— [com.hdu.cetvocab.data.UserDb.quizRecords]（榜单列表）、
 * [com.hdu.cetvocab.data.UserDb.bestScore]（最高分）、
 * [com.hdu.cetvocab.data.UserDb.quizPlayedCount]（闯关次数）三处统计都会把它们排除。
 *
 * 成绩记录本身仍然照常写入 `quiz_record`：成就系统要靠它按 unit 统计
 * 「易混词辨析 / 翻译全对 5 关」这类进度（见 `quizPerfectCountOfUnit`），
 * 只是这些记录不再进入排名口径。学习时长（`quizTotalMs`）也照常计入，
 * 因为那本来就是实打实的学习时间，与排名无关。
 */
val EXTENSION_QUIZ_SCOPES = listOf(QUIZ_SCOPE_CONFUSABLE, QUIZ_SCOPE_TRANSLATION)

/**
 * 一局闯关的「可重放配方」。
 *
 * 存在的唯一理由：进程被系统回收（开发者选项「不保留活动」，或低内存下后台被杀）
 * 后，Compose Navigation 会把 QUIZ 路由恢复出来，但闯关的题目是随机抽的、
 * 只存在于内存里 —— 用户看到的是选关页，刚才那局的分值、连击、答到第几题全部消失，
 * 且一行数据都没入库。把「种子 + 出题参数」记下来就能原样重放出同一批题：
 * 词库是随包的只读 SQLite，取词全部带 ORDER BY "rank" / ORDER BY id，
 * 同一 seed 走同一条 shuffle 链必然得到同一份题序，因此不需要把题目本身存下来。
 */
data class QuizSpec(
    /** [QUIZ_SPEC_UNIT] / _STAGE / _LETTER / _CONFUSABLE / _TRANSLATION / _WRONG */
    val kind: Int,
    val stage: Int,
    val unit: Int,
    val count: Int,
    val seed: Long,
    /**
     * 错题重做专用：开局时冻结下来的题目清单（`kind:wordId` 逗号串）。
     *
     * 其余关卡的题序完全由「池子 + seed」决定，重放时再查一次表即可；
     * 错题集却会在重做过程中被答对的题缩小，不冻结就没法原样重放，
     * 所以这一局必须把题目本身带在快照里。其它 kind 恒为空串。
     */
    val refs: String = ""
)

const val QUIZ_SPEC_UNIT = 0
const val QUIZ_SPEC_STAGE = 1
const val QUIZ_SPEC_LETTER = 2
const val QUIZ_SPEC_CONFUSABLE = 3
const val QUIZ_SPEC_TRANSLATION = 4

/**
 * 错题重做。
 *
 * 与其余 kind 的区别只有一个但很关键：这一局**不写 quiz_record、不触发成就**。
 * 错题可以无限次重做，若照常入库，刷十几遍就能把排行榜顶到一个从没真实达到过的分数，
 * 「历次成绩」的语义会被自己毁掉。所以口径与拓展练习一致：只更新错题集本身。
 */
const val QUIZ_SPEC_WRONG = 5

/** 单场重做的题量上限：再多就变成抄题库，失去「订正」的意义 */
const val WRONG_REDO_LIMIT = 20

/**
 * 闯关错题集的一行（表结构见 [UserDb]）。
 *
 * [kind] 存 [QuizKind] 的序号，重做时按原题型重新出题：
 * 选义错的就再给一次四选一，拼写错的就还得自己拼出来，
 * 不能把拼写错题降级成选义题来「送分」。
 */
data class QuizWrong(
    val stage: Int,
    val kind: Int,
    val wordId: Int,
    val wrongCount: Int,
    val lastTs: Long
)

/** 首字母对应的闯关范围值；非 A–Z 字母返回 null */
fun quizScopeOfLetter(letter: Char): Int? {
    val upper = letter.uppercaseChar()
    if (upper !in 'A'..'Z') return null
    return -(upper - 'A') - 2
}

/** 闯关范围值对应的首字母；非字母范围返回 null */
fun quizLetterOfScope(unit: Int): Char? {
    if (unit > -2) return null
    val index = -unit - 2
    // 越界时返回 null 而不是 `'A' + index` 算出乱七八糟的字符
    if (index !in 0..25) return null
    return 'A' + index
}

/**
 * 闯关范围的可读标签，用于答题页标题与排行榜。
 *
 * 分隔符不用间隔号：「四级 · 全词挑战」这种写法在中文正文里属于装饰性标点，
 * 直接接读即可（用户口径「少用·和空格」）。紧贴中文的空格一并去掉，
 * 只在拉丁字母与数字两侧留必要的气口。
 */
fun quizScopeLabel(stage: Int, unit: Int): String {
    val prefix = WordDb.stageLabel(stage)
    return when {
        unit >= 1 -> "${prefix}第${unit}单元"
        unit == QUIZ_SCOPE_ALL -> "${prefix}全词挑战"
        unit == QUIZ_SCOPE_CONFUSABLE -> "${prefix}易混词辨析"
        unit == QUIZ_SCOPE_TRANSLATION -> "${prefix}真题句子翻译"
        unit == QUIZ_SCOPE_WRONG -> "${prefix}错题重做"
        else -> quizLetterOfScope(unit)?.let { "${prefix}首字母${it}" } ?: "${prefix}闯关"
    }
}

/**
 * 闯关题型。
 * - [MEANING]     选出正确释义（四选一）
 * - [SPELLING]    听音拼写（填空）
 * - [CONFUSABLE]  易混词辨析：给中文义，从四个形近词里挑出对应单词
 * - [TRANSLATION] 真题句子翻译：给中文句，从四个译文中挑出全部可接受译法（可多选）
 */
enum class QuizKind {
    MEANING, SPELLING, CONFUSABLE, TRANSLATION
}

/** 真题句子翻译题（内置题库 translation_quiz） */
data class TranslationItem(
    val id: Int,
    val stage: Int,
    /** 被考查的关键词 */
    val keyword: String,
    /** 中文题干 */
    val chinese: String,
    /** 四个英文译文 */
    val options: List<String>,
    /** 可接受译法的下标集合（多选题，通常两个 —— 体现「一词多译」） */
    val correct: Set<Int>,
    /** 解析：关键词用法 + 其他可接受译法 */
    val analysis: String
)

/** 每日学习统计 */
data class DayStat(
    val day: String,
    val learned: Int,
    val correct: Int,
    val wrong: Int
)

/** 成就定义 */
data class AchievementDef(
    val code: String,
    val title: String,
    val desc: String,
    val glyph: String,
    val tier: Int   // 1 铜 2 银 3 金
)

/** 学习总览 */
data class StudyOverview(
    val todayLearned: Int = 0,
    val totalLearned: Int = 0,
    val mastered: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val streakDays: Int = 0,
    val bestStreak: Int = 0,
    val totalMinutes: Int = 0,
    val quizPlayed: Int = 0,
    val bestScore: Int = 0
) {
    val accuracy: Int
        get() {
            val t = totalCorrect + totalWrong
            return percentOf(totalCorrect, t)
        }
}
