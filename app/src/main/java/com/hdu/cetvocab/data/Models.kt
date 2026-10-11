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
    val level: Int = 0,        // 0 未学 1 模糊 2 认识 3 掌握
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
 * 闯关范围编码（`QuizUiState.unit` 与 `QuizRecord.unit` 共用同一套约定）：
 * - `>= 1`  → 单元挑战，值即单元号
 * - `-1`    → 全阶段全词挑战
 * - `<= -2` → 首字母挑战，字母索引 = `-unit - 2`（0 = A，25 = Z）
 */
const val QUIZ_SCOPE_ALL = -1

/** 首字母对应的闯关范围值 */
fun quizScopeOfLetter(letter: Char): Int = -(letter.uppercaseChar() - 'A') - 2

/** 闯关范围值对应的首字母；非字母范围返回 null */
fun quizLetterOfScope(unit: Int): Char? = if (unit <= -2) ('A' + (-unit - 2)) else null

/** 闯关范围的可读标签，用于答题页标题与排行榜 */
fun quizScopeLabel(stage: Int, unit: Int): String {
    val prefix = WordDb.stageLabel(stage)
    return when {
        unit >= 1 -> "$prefix · 第 $unit 单元"
        unit == QUIZ_SCOPE_ALL -> "$prefix · 全词挑战"
        else -> "$prefix · 首字母 ${quizLetterOfScope(unit)}"
    }
}

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

/** 成就解锁记录 */
data class AchievementRecord(
    val code: String,
    val unlockedTs: Long
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
