package com.hdu.cetvocab.data

import android.content.Context
import com.hdu.cetvocab.util.LauncherIcons
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 闯关题目。
 *
 * 早期只有「选义」与「拼写」两种题型，靠 `spelling` 一个布尔量区分；
 * 加入「易混词辨析」与「真题句子翻译」后再加布尔量就会变成状态组合爆炸，
 * 因此统一收敛到 [kind] 上，保留 `spelling` 只是为了兼容旧调用点。
 *
 * 两种新题型的差异：
 * - [QuizKind.CONFUSABLE]  单选，`prompt` 是中文释义，选项是四个形近词；
 * - [QuizKind.TRANSLATION] 多选，`prompt` 是中文句子，`correctSet` 是全部可接受译法。
 */
data class QuizQuestion(
    val word: Word,
    val options: List<String>,
    val correctIndex: Int,
    val spelling: Boolean,
    val kind: QuizKind = if (spelling) QuizKind.SPELLING else QuizKind.MEANING,
    /** 多选题的全部正确下标；单选题留空，判分时走 [answerSet] */
    val correctSet: Set<Int> = emptySet(),
    /** 题干：辨析题为中文释义，翻译题为中文句子；为空时展示单词本身 */
    val prompt: String = "",
    /** 揭晓答案后的解析 */
    val analysis: String = "",
    /** 辨析题专用：每个选项「单词 — 释义」的对照，便于横向比较 */
    val optionNotes: List<String> = emptyList()
) {
    /** 是否需要多选 */
    val multi: Boolean get() = kind == QuizKind.TRANSLATION

    /** 正确选项集合：单选与多选都归一成集合，判分逻辑只需写一份 */
    val answerSet: Set<Int>
        get() = if (correctSet.isNotEmpty()) correctSet else setOf(correctIndex)
}

/** 业务聚合层：词库 + 用户数据 + 复习算法 + 成就判定 */
class VocabRepository(context: Context) {

    val wordDb = WordDb(context)
    val userDb = UserDb(context)
    private val prefs = context.getSharedPreferences("vocab_meta", Context.MODE_PRIVATE)

    var stage: Int
        get() = prefs.getInt(KEY_STAGE, WordDb.STAGE_CET4)
        set(value) = prefs.edit().putInt(KEY_STAGE, value).apply()

    var dailyGoal: Int
        get() = prefs.getInt(KEY_GOAL, 20)
        set(value) = prefs.edit().putInt(KEY_GOAL, value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTOSPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOSPEAK, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_RATE, 0.92f)
        set(value) = prefs.edit().putFloat(KEY_RATE, value).apply()

    /**
     * 桌面图标风格：0 = 默认书本图标，1 = 简洁图标（橙底白字）。
     * 简洁图标下具体显示「四」还是「六」，由 [stage] 决定，不单独存偏好。
     */
    var iconStyle: Int
        get() = prefs.getInt(KEY_ICON_STYLE, LauncherIcons.STYLE_DEFAULT)
        set(value) = prefs.edit().putInt(KEY_ICON_STYLE, value).apply()

    /**
     * 明暗模式：[THEME_SYSTEM] 跟随系统、[THEME_LIGHT] 强制浅色、[THEME_DARK] 强制深色。
     * 默认跟随系统，但用户可以手动固定，不受系统深浅色开关影响。
     */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, THEME_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

    // ---------------- 掌握等级 ----------------

    /** 等级含义：0 未学 1 模糊 2 认识 3 熟悉 4 掌握 5 精通 */
    fun levelLabel(level: Int): String = when (level) {
        0 -> "未学"
        1 -> "模糊"
        2 -> "认识"
        3 -> "熟悉"
        4 -> "掌握"
        else -> "精通"
    }

    /**
     * 基于 SuperMemo-2（SM-2）算法的间隔重复调度。
     *
     * 每次作答给出质量分 q：答对 q=4，答错 q=1。
     * - 答错（q<3）：连续答对次数 reps 归零，间隔归零，立即重新排队；
     * - 答对：reps+1，间隔序列为 1 天 → 6 天 → 上一次间隔 × 难度因子 ease（向下取整，至少 1 天）；
     * - 难度因子 ease 按 SM-2 公式更新，并夹在 [1.3, 2.8] 区间内，越难记的词 ease 越小。
     */
    fun answerWord(word: Word, known: Boolean): Int {
        val s = userDb.stateOf(word.id)
        val q = if (known) 4 else 1
        val reps: Int
        val intervalDays: Int
        if (q < 3) {
            // 答错：重新排队，间隔清零
            reps = 0
            intervalDays = 0
        } else {
            reps = s.reps + 1
            intervalDays = when (reps) {
                1 -> 1
                2 -> 6
                else -> maxOf(1, Math.round(s.intervalDays * s.ease).toInt())
            }
        }
        // SM-2 难度因子更新公式
        val ease = (s.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceIn(1.3, 2.8)
        val now = System.currentTimeMillis()
        val nextReviewTs = now + intervalDays * 86_400_000L
        // level 仅用于 UI 展示（未学/模糊/认识/熟悉/掌握/精通），语义与旧版保持一致
        val newLevel = if (known) (s.level + 1).coerceAtMost(5) else 1
        userDb.markLearned(word.id, newLevel, nextReviewTs, ease, reps, intervalDays, correct = known)
        userDb.logStudy(word.id, known)
        return newLevel
    }

    /**
     * 切换收藏并返回新值。
     *
     * 翻转动作下沉到 [UserDb.toggleFav]，在同一个数据库事务里完成「读旧值 + 写新值」，
     * 避免这里 `!stateOf(id).fav` 再 `setFav(...)` 两次调用之间的竞态。
     */
    fun toggleFav(word: Word): Boolean = userDb.toggleFav(word.id)

    // ---------------- 学习计划 ----------------

    /**
     * 到期待复习的词。
     * 必须按**当前级别**过滤：学习记录是按 word_id 全局存放的，
     * 不过滤的话在六级学习时会把四级的待复习词也排进队列。
     */
    fun dueWords(limit: Int = 30): List<Word> {
        val now = System.currentTimeMillis()
        val ids = userDb.states().values
            .filter { it.level in 1..5 && it.nextReviewTs in 1..now }
            .sortedBy { it.nextReviewTs }
            .map { it.wordId }
        if (ids.isEmpty()) return emptyList()
        val order = ids.withIndex().associate { it.value to it.index }
        return wordDb.wordsByIds(ids)
            .filter { it.stage == stage }
            .sortedBy { order[it.id] ?: Int.MAX_VALUE }
            .take(limit)
    }

    fun newWords(limit: Int): List<Word> {
        if (limit <= 0) return emptyList()
        val states = userDb.states()
        return wordDb.wordsOfStage(stage)
            .filter { (states[it.id]?.level ?: 0) == 0 }
            .take(limit)
    }

    /** 今日学习队列：先复习到期词，再补新词到每日目标 */
    fun todayQueue(): List<Word> {
        val learnedToday = userDb.todayLearnedCount()
        val remain = (dailyGoal - learnedToday).coerceAtLeast(0)
        return dueWords(40) + newWords(remain)
    }

    fun unitWords(unit: Int): List<Word> = wordDb.wordsOfUnit(stage, unit)

    fun favWords(): List<Word> {
        val ids = userDb.states().values.filter { it.fav }.map { it.wordId }
        return wordDb.wordsByIds(ids).sortedBy { it.word }
    }

    fun wrongWords(limit: Int = 60): List<Word> {
        val ids = userDb.states().values
            // 已经掌握（level 5）的词不再占用错词本，避免只增不减
            .filter { it.wrongCount > 0 && it.level < 5 }
            .sortedByDescending { it.wrongCount }
            .map { it.wordId }
            .take(limit)
        return wordDb.wordsByIds(ids)
    }

    // ---------------- 闯关 ----------------

    fun buildQuiz(unit: Int, count: Int, spellingRatio: Float = 0.25f): List<QuizQuestion> =
        buildQuizFromPool(wordDb.wordsOfUnit(stage, unit), count, spellingRatio)

    /** 全阶段挑战：从当前级别完整词库随机抽题，unit 以 -1 标记为全词挑战。 */
    fun buildStageQuiz(count: Int = 30, spellingRatio: Float = 0.25f): List<QuizQuestion> =
        buildQuizFromPool(wordDb.wordsOfStage(stage), count, spellingRatio)

    /** 首字母挑战：从该首字母下的全部单词随机抽题（字母不分大小写）。 */
    fun buildLetterQuiz(letter: Char, count: Int = 10, spellingRatio: Float = 0.25f): List<QuizQuestion> =
        buildQuizFromPool(wordDb.wordsByInitial(stage, letter), count, spellingRatio)

    /**
     * 易混词辨析：给出中文释义，从四个形近词里挑出对应的单词。
     *
     * 语料直接用词库内置的 `similar`（形近词）字段，不额外造数据，
     * 因此本级只要还有词就能一直出新题，不存在题库耗尽的边界。
     */
    fun buildConfusableQuiz(count: Int = 10): List<QuizQuestion> {
        // 干扰项要凑够 3 个，所以只挑形近词不少于 3 个的词条
        val pool = wordDb.wordsOfStage(stage).filter { it.similarList.size >= 3 }
        if (pool.size < 4) return emptyList()
        val random = Random(System.nanoTime())
        return pool.shuffled(random).take(count).mapNotNull { word ->
            val candidates = word.similarList
                // 形近词里混着 airplane / aeroplane 这类同义异形条目，
                // 留着当干扰项会让一道题出现两个正确答案，必须剔除
                .filter { it.first != word.word && it.second != word.translation }
                .shuffled(random)
                .take(3)
            if (candidates.size < 3) return@mapNotNull null
            val entries = (candidates + (word.word to word.translation)).shuffled(random)
            val answer = entries.indexOfFirst { it.first == word.word }
            // 极端兜底：万一目标词没进选项（理论上不会），直接放弃这道题
            if (answer < 0) return@mapNotNull null
            QuizQuestion(
                word = word,
                options = entries.map { it.first },
                correctIndex = answer,
                spelling = false,
                kind = QuizKind.CONFUSABLE,
                prompt = word.translation,
                optionNotes = entries.map { "${it.first}  ${it.second}" },
                analysis = "「${word.translation}」对应 ${word.word}。" +
                    "四个词分别为：${entries.joinToString("；") { "${it.first} = ${it.second}" }}"
            )
        }
    }

    /**
     * 真题句子翻译：给出中文句子，从四个英文译文里选出**全部**正确译法。
     *
     * 每题都有两个可接受译法（一个直译、一个替换措辞的活译），
     * 用来训练「同一个意思可以有多种地道表达」，这也是四六级翻译的评分取向。
     * 必须全选对才算答对 —— 少选说明还没吃透，按错处理并给出解析。
     */
    fun buildTranslationQuiz(count: Int = 10): List<QuizQuestion> {
        val items = wordDb.translationItems(stage)
        if (items.isEmpty()) return emptyList()
        val random = Random(System.nanoTime())
        return items.shuffled(random).take(count).map { item ->
            // 关键词词条只用于展示音标与朗读；题库里的词都是本级大纲词，
            // 万一查不到就用最小占位词条兜底，保证题目本身仍可作答
            val keywordWord = wordDb.wordByText(item.keyword, item.stage)
                ?: placeholderWord(item.keyword, item.stage)
            QuizQuestion(
                word = keywordWord,
                options = item.options,
                correctIndex = item.correct.min(),
                spelling = false,
                kind = QuizKind.TRANSLATION,
                correctSet = item.correct,
                prompt = item.chinese,
                analysis = item.analysis
            )
        }
    }

    /** 关键词查不到词条时的占位对象，避免为了非空而伪造释义 */
    private fun placeholderWord(text: String, wordStage: Int) = Word(
        id = 0,
        word = text,
        phonetic = "",
        translation = "",
        transDetail = "",
        definition = "",
        pos = "",
        stage = wordStage,
        unit = 0,
        rank = 0,
        sentence = "",
        sentenceCn = "",
        roots = "",
        similar = "",
        mnemonic = ""
    )

    private fun buildQuizFromPool(
        pool: List<Word>,
        count: Int,
        spellingRatio: Float
    ): List<QuizQuestion> {
        if (pool.size < 4 || count <= 0) return emptyList()
        val random = Random(System.nanoTime())
        val questions = pool.shuffled(random).take(count).map { word ->
            val spelling = random.nextFloat() < spellingRatio
            if (spelling) {
                QuizQuestion(word, emptyList(), -1, true)
            } else {
                // 干扰项的释义必须与正确答案不同，彼此也不能重复
                // （词库中存在 airplane/aeroplane 这类同义异形词，需要显式规避）
                val used = mutableSetOf(word.translation)
                val distractors = ArrayList<String>(3)
                val others = pool.filter { it.id != word.id }.shuffled(random)
                for (candidate in others) {
                    if (distractors.size >= 3) break
                    if (used.add(candidate.translation)) distractors.add(candidate.translation)
                }
                // 极端情况下（同义释义过多）放宽唯一性，保证始终有 4 个选项
                if (distractors.size < 3) {
                    for (candidate in others) {
                        if (distractors.size >= 3) break
                        if (candidate.translation != word.translation &&
                            distractors.none { it == candidate.translation }
                        ) distractors.add(candidate.translation)
                    }
                }
                if (distractors.isEmpty()) return@map QuizQuestion(word, emptyList(), -1, true)
                val options = (distractors + word.translation).shuffled(random)
                QuizQuestion(word, options, options.indexOf(word.translation), false)
            }
        }
        return questions
    }

    fun saveQuizRecord(
        unit: Int, mode: String, score: Int, total: Int,
        correct: Int, maxCombo: Int, durationMs: Long
    ): QuizRecord {
        val record = QuizRecord(
            0L, stage, unit, mode, score, total, correct, maxCombo,
            durationMs, System.currentTimeMillis()
        )
        userDb.addQuizRecord(record)
        return record
    }

    // ---------------- 统计 ----------------

    fun streakDays(): Int {
        val set = userDb.studyDays().toHashSet()
        if (set.isEmpty()) return 0
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var cursor = LocalDate.now()
        if (!set.contains(cursor.format(fmt))) cursor = cursor.minusDays(1)
        var count = 0
        while (set.contains(cursor.format(fmt))) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun bestStreak(): Int {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dates = userDb.studyDays()
            .mapNotNull { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
            .distinct()
            .sorted()
        if (dates.isEmpty()) return 0
        var best = 1
        var cur = 1
        for (i in 1 until dates.size) {
            if (dates[i] == dates[i - 1].plusDays(1)) {
                cur++
                if (cur > best) best = cur
            } else {
                cur = 1
            }
        }
        return best
    }

    /**
     * 学习总览。
     *
     * [preloadedStates] 用于避免重复读表：调用方（如 [evaluateAchievements]）往往
     * 已经拿到了 states，再让 overview 自己查一次就是白白多一次全表扫描。
     */
    fun overview(preloadedStates: Map<Int, WordState>? = null): StudyOverview {
        val states = preloadedStates ?: userDb.states()
        val (correct, wrong) = userDb.correctWrongTotals()
        val seconds = userDb.studySeconds() + userDb.quizTotalMs() / 1000
        return StudyOverview(
            todayLearned = userDb.todayLearnedCount(),
            totalLearned = states.count { it.value.level > 0 },
            mastered = states.count { it.value.level >= 3 },
            totalCorrect = correct,
            totalWrong = wrong,
            streakDays = streakDays(),
            bestStreak = bestStreak(),
            totalMinutes = (seconds / 60).toInt(),
            quizPlayed = userDb.quizPlayedCount(),
            bestScore = userDb.bestScore()
        )
    }

    fun addStudySeconds(seconds: Long) = userDb.addStudySeconds(seconds)

    // ---------------- 成就 ----------------

    fun unlockedAchievements(): Map<String, Long> = userDb.achievements()

    /**
     * 评估并解锁成就。
     *
     * 性能注意：本方法在「每答一个单词」时都会被调用一次，因此这里
     * **只读一次 states 表**并把结果透传给 [overview]，
     * 否则单次调用就要扫两遍全表，学一组 50 个词会累积上百次全表扫描。
     */
    fun evaluateAchievements(lastQuiz: QuizRecord? = null): List<AchievementDef> {
        val gained = ArrayList<AchievementDef>()
        val states = userDb.states()
        val ov = overview(preloadedStates = states)
        val favCount = states.count { it.value.fav }
        val hour = LocalTime.now().hour

        fun tryUnlock(code: String, condition: Boolean) {
            if (condition && userDb.unlock(code)) {
                Achievements.def(code)?.let { gained.add(it) }
            }
        }

        tryUnlock(Achievements.FIRST_STEP, ov.totalLearned >= 1)
        tryUnlock(Achievements.LEARN_50, ov.totalLearned >= 50)
        tryUnlock(Achievements.LEARN_200, ov.totalLearned >= 200)
        tryUnlock(Achievements.LEARN_500, ov.totalLearned >= 500)
        tryUnlock(Achievements.STREAK_3, ov.streakDays >= 3)
        tryUnlock(Achievements.STREAK_7, ov.streakDays >= 7)
        tryUnlock(Achievements.STREAK_10, ov.streakDays >= 10)
        tryUnlock(Achievements.STREAK_30, ov.streakDays >= 30)
        tryUnlock(Achievements.QUIZ_FIRST, ov.quizPlayed >= 1)
        tryUnlock(Achievements.QUIZ_SCORE_300, ov.bestScore >= 300)
        tryUnlock(Achievements.MASTER_50, ov.mastered >= 50)
        tryUnlock(Achievements.MASTER_200, ov.mastered >= 200)
        tryUnlock(Achievements.ACCURACY_90, ov.totalCorrect + ov.totalWrong >= 50 && ov.accuracy >= 90)
        tryUnlock(Achievements.FAV_20, favCount >= 20)
        tryUnlock(Achievements.GOAL_DONE, ov.todayLearned >= dailyGoal)
        tryUnlock(Achievements.NIGHT_OWL, hour >= 22 && ov.totalLearned >= 1)
        tryUnlock(Achievements.EARLY_BIRD, hour in 1..6 && ov.totalLearned >= 1)

        // 本次闯关专属的成就。QUIZ_SCORE_300 上面已按历史最高分判过一次，
        // 这里再按「本局得分」判一次是有意为之：本局刷出高分也应立即解锁。
        lastQuiz?.let {
            tryUnlock(Achievements.QUIZ_PERFECT, it.total > 0 && it.correct == it.total)
            tryUnlock(Achievements.QUIZ_COMBO_10, it.maxCombo >= 10)
            tryUnlock(Achievements.QUIZ_SCORE_300, it.score >= 300)

            // 专题关卡成就：只在对应的专题结算时才去查表，
            // 避免普通单元 / 首字母闯关也白白多跑几次 COUNT 查询。
            when (it.unit) {
                QUIZ_SCOPE_CONFUSABLE -> {
                    tryUnlock(Achievements.CONFUSABLE_FIRST, it.total > 0)
                    tryUnlock(
                        Achievements.CONFUSABLE_PERFECT_5,
                        userDb.quizPerfectCountOfUnit(QUIZ_SCOPE_CONFUSABLE) >= 5
                    )
                }
                QUIZ_SCOPE_TRANSLATION -> {
                    tryUnlock(Achievements.TRANSLATION_FIRST, it.total > 0)
                    tryUnlock(
                        Achievements.TRANSLATION_PERFECT_5,
                        userDb.quizPerfectCountOfUnit(QUIZ_SCOPE_TRANSLATION) >= 5
                    )
                }
                else -> {
                    // 专题成就按关卡类型判断，普通闯关不触发；
                    // 但「三关通吃」可能在任一关卡结算时凑齐，因此放到外面统一判一次。
                }
            }
            tryUnlock(
                Achievements.TOPIC_SWEEP,
                userDb.quizPlayedUnits(
                    listOf(QUIZ_SCOPE_ALL, QUIZ_SCOPE_CONFUSABLE, QUIZ_SCOPE_TRANSLATION)
                ).size == 3
            )
        }
        return gained
    }

    fun resetAll() {
        userDb.writableDatabase.apply {
            execSQL("DELETE FROM word_state")
            execSQL("DELETE FROM study_log")
            execSQL("DELETE FROM quiz_record")
            execSQL("DELETE FROM achievement")
            execSQL("DELETE FROM app_meta")
        }
        prefs.edit().clear().apply()
    }

    companion object {
        /** 明暗模式：跟随系统 */
        const val THEME_SYSTEM = 0

        /** 明暗模式：始终浅色 */
        const val THEME_LIGHT = 1

        /** 明暗模式：始终深色 */
        const val THEME_DARK = 2

        private const val KEY_STAGE = "stage"
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_AUTOSPEAK = "auto_speak"
        private const val KEY_RATE = "speech_rate"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_THEME = "theme_mode"
    }
}
