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
    /** 阶段内单元号（每 50 词一单元），对齐官方词表顺序 */
    val unit: Int,
    /** 阶段内序号（按官方词表顺序） */
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

/** 用户对单词的掌握状态 */
data class WordState(
    val wordId: Int,
    /**
     * 掌握等级，共 6 档，文案见
     * [com.hdu.cetvocab.data.VocabRepository.levelLabel]：
     * 0 未学 / 1 模糊 / 2 认识 / 3 熟悉 / 4 掌握 / 5 精通。
     * 「掌握词数」按 `level >= 3` 起算，错词本在 `level == 5` 时把词移出。
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
    val accuracy: Int get() = if (total <= 0) 0 else correct * 100 / total
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
 *
 * 两个专题关卡刻意取 -101 / -102：首字母最多只用到 -27，
 * 中间留出大段空档，后续再加专题也不会和已有编码撞车。
 */
const val QUIZ_SCOPE_ALL = -1
const val QUIZ_SCOPE_CONFUSABLE = -101
const val QUIZ_SCOPE_TRANSLATION = -102

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

/** 闯关范围的可读标签，用于答题页标题与排行榜 */
fun quizScopeLabel(stage: Int, unit: Int): String {
    val prefix = WordDb.stageLabel(stage)
    return when {
        unit >= 1 -> "$prefix · 第 $unit 单元"
        unit == QUIZ_SCOPE_ALL -> "$prefix · 全词挑战"
        unit == QUIZ_SCOPE_CONFUSABLE -> "$prefix · 易混词辨析"
        unit == QUIZ_SCOPE_TRANSLATION -> "$prefix · 真题句子翻译"
        else -> quizLetterOfScope(unit)?.let { "$prefix · 首字母 $it" } ?: "$prefix · 闯关"
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
            return if (t <= 0) 0 else totalCorrect * 100 / t
        }
}
