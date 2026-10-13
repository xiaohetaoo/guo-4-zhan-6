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
    private val prefs = context.getSharedPreferences(WordDb.PREFS_NAME, Context.MODE_PRIVATE)

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

    /**
     * 界面动效开关：true 为开启（默认），false 为关闭。
     *
     * 这里只负责持久化；真正让动效生效/失效要由调用方把这个值同步给
     * `Motion.applyAnimationsEnabled()` —— 数据层不反向依赖 ui 包。
     */
    var animationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIM, true)
        set(value) = prefs.edit().putBoolean(KEY_ANIM, value).apply()

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

    /**
     * 到期待复习词的**真实总数**，供首页「待复习 N 词」展示。
     *
     * 不能拿 `dueWords(40).size` 当总数：[dueWords] 末尾是 `.take(limit)`，
     * 那是「取前 40 条」而不是「有 40 条」。到期词积压到上百个时，
     * 首页会一直显示「待复习 40 词」，用户完全看不出欠了多少。
     */
    fun dueCount(): Int {
        val now = System.currentTimeMillis()
        val ids = userDb.states().values
            .filter { it.level in 1..5 && it.nextReviewTs in 1..now }
            .map { it.wordId }
        if (ids.isEmpty()) return 0
        // wordsByIds 内部分批（每批 500 个占位符），这里按各批命中数累加即为总数
        return ids.chunked(ID_BATCH).sumOf { chunk ->
            wordDb.wordsByIds(chunk).count { it.stage == stage }
        }
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

    /**
     * 「继续学习」队列：今日目标完成后的加量学习。
     *
     * 正常今日队列在目标达成后只剩到期复习词（newWords 补 0 个），全部还完即空，
     * 首页按钮显示「今日已完成」。此方法在到期词之外再预学一批新词，
     * 让用户完成每日目标后还能主动加量（超前学习，不计入当日目标口径）。
     * 返回为空（既无到期词也无新词）时由 startQueue 统一弹提示，无需在此特判。
     */
    fun extraQueue(count: Int = 20): List<Word> = dueWords(40) + newWords(count)

    fun unitWords(unit: Int): List<Word> = wordDb.wordsOfUnit(stage, unit)

    fun favWords(): List<Word> {
        val ids = userDb.states().values.filter { it.fav }.map { it.wordId }
        return wordDb.wordsByIds(ids).sortedBy { it.word }
    }

    fun wrongWords(limit: Int = 60): List<Word> {
        val ids = userDb.states().values
            // 已答到「精通」档（LEVEL_EXPERT）的词不再占用错词本，避免只增不减
            .filter { it.wrongCount > 0 && it.level < LEVEL_EXPERT }
            .sortedByDescending { it.wrongCount }
            .map { it.wordId }
            .take(limit)
        return wordDb.wordsByIds(ids)
    }

    // ---------------- 闯关 ----------------

    fun buildQuiz(unit: Int, count: Int, spellingRatio: Float = 0.25f): List<QuizQuestion> =
        buildQuizFromPool(wordDb.wordsOfUnit(stage, unit), count, spellingRatio)

    /**
     * 全阶段挑战：从当前级别完整词库随机抽题，unit 以 -1 标记为全词挑战。
     *
     * 题量取 50（原为 30）：全词挑战是唯一能拉长连击的关卡，
     * 30 题时「单次闯关连对 50 题」（QUIZ_COMBO_50）在数学上不可能达成
     * ——其余关卡都是 10 题。提到 50 题后这一档才有可达路径（需要 50 题全对）。
     */
    fun buildStageQuiz(count: Int = 50, spellingRatio: Float = 0.25f): List<QuizQuestion> =
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
                // 兜底：本级释义高度趋同（整批同义异形词）时，唯一释义可能凑不满 3 个。
                // 这里放宽到「只要与正确答案不同」——宁可给出两个释义相同的干扰项，
                // 也不要让一道题只剩 2–3 个选项（闯关页按 4 选项设计）。
                // 早先这层兜底写的过滤条件与上面的循环等价，等于没有兜底。
                if (distractors.size < 3) {
                    for (candidate in others) {
                        if (distractors.size >= 3) break
                        if (candidate.translation != word.translation) {
                            distractors.add(candidate.translation)
                        }
                    }
                }
                if (distractors.isEmpty()) return@map QuizQuestion(word, emptyList(), -1, true)
                // 正确项必须参与打乱，否则答案位置会偏向某几号
                val options = (distractors + word.translation).shuffled(random)
                QuizQuestion(word, options, options.indexOf(word.translation), false)
            }
        }
        return questions
    }

    /**
     * 写入一局闯关成绩。
     *
     * `stage` 必须由调用方传入**开局时的级别**（[com.hdu.cetvocab.ui.QuizUiState.stage]），
     * 不能在这里读属性 [stage]：属性读的是实时值，而用户完全可能在答到一半时
     * 切到别的标签把级别从四级改成六级，那样入库的 stage 与题目实际级别不一致，
     * 排行榜与按级别统计的成就都会错档。
     */
    fun saveQuizRecord(
        stage: Int, unit: Int, mode: String, score: Int, total: Int,
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

    /**
     * 由同一份学习日期同时算出「当前连续打卡」与「历史最长连续」。
     *
     * [streakDays] 与 [bestStreak] 各自都要把 `study_log` 的 DISTINCT day 全表读一遍，
     * 而这两个值都在「每答一个单词」就会跑的 [overview] 里用到 —— 合并成一次扫描，
     * 每次作答少一遍全表查询。语义与拆开写时完全一致：
     * 今天没学但昨天连了一串，当前连续仍从昨天往回数（不因为「今天还没学」而清零）。
     */
    private fun streaks(days: List<String>): Pair<Int, Int> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val set = days.toHashSet()
        if (set.isEmpty()) return 0 to 0

        var current = 0
        var cursor = LocalDate.now()
        if (!set.contains(cursor.format(fmt))) cursor = cursor.minusDays(1)
        while (set.contains(cursor.format(fmt))) {
            current++
            cursor = cursor.minusDays(1)
        }

        var best = 0
        var run = 0
        var previous: LocalDate? = null
        days.asSequence()
            .mapNotNull { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
            .distinct()
            .sorted()
            .forEach { d ->
                // 取一份局部副本再比较：previous 是被闭包捕获的可变局部变量，
                // Kotlin 不允许对它智能转换
                val prev = previous
                run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
                if (run > best) best = run
                previous = d
            }
        return current to best
    }

    fun streakDays(): Int = streaks(userDb.studyDays()).first

    /** 历史最长连续打卡：断签之后仍然保留的高光记录 */
    fun bestStreak(): Int = streaks(userDb.studyDays()).second

    /**
     * 学习总览。
     *
     * [preloadedStates] 用于避免重复读表：调用方（如 [evaluateAchievements]）往往
     * 已经拿到了 states，再让 overview 自己查一次就是白白多一次全表扫描。
     * 连续打卡与历史最长同样只读一次 `studyDays()`（见 [streaks]）。
     */
    fun overview(preloadedStates: Map<Int, WordState>? = null): StudyOverview {
        val states = preloadedStates ?: userDb.states()
        val (correct, wrong) = userDb.correctWrongTotals()
        val seconds = userDb.studySeconds() + userDb.quizTotalMs() / 1000
        val (streak, best) = streaks(userDb.studyDays())
        return StudyOverview(
            todayLearned = userDb.todayLearnedCount(),
            totalLearned = states.count { it.value.level > 0 },
            mastered = states.count { it.value.level >= MASTER_LEVEL },
            totalCorrect = correct,
            totalWrong = wrong,
            streakDays = streak,
            bestStreak = best,
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
     *
     * 成就数量涨到 80+ 之后还有第二个性能坑：如果对每个成就都无条件调
     * `userDb.unlock()`，每答一个单词就要跑 80 多次 INSERT。因此这里先取一次
     * 已解锁集合 [unlocked]，[tryUnlock] 对已解锁的 code 直接返回，
     * 真正落库的只有「尚未解锁且条件刚达成」的那零星几个（绝大多数调用是 0 次写入）。
     */
    fun evaluateAchievements(lastQuiz: QuizRecord? = null): List<AchievementDef> {
        val gained = ArrayList<AchievementDef>()
        val states = userDb.states()
        val ov = overview(preloadedStates = states)
        val favCount = states.count { it.value.fav }
        val hour = LocalTime.now().hour
        val answered = ov.totalCorrect + ov.totalWrong
        val unlocked = userDb.achievements().keys.toMutableSet()

        fun tryUnlock(code: String, condition: Boolean) {
            if (code in unlocked) return
            if (condition && userDb.unlock(code)) {
                unlocked.add(code)
                Achievements.def(code)?.let { gained.add(it) }
            }
        }

        // ---- 累计学习词数 ----
        tryUnlock(Achievements.FIRST_STEP, ov.totalLearned >= 1)
        tryUnlock(Achievements.LEARN_20, ov.totalLearned >= 20)
        tryUnlock(Achievements.LEARN_50, ov.totalLearned >= 50)
        tryUnlock(Achievements.LEARN_100, ov.totalLearned >= 100)
        tryUnlock(Achievements.LEARN_200, ov.totalLearned >= 200)
        tryUnlock(Achievements.LEARN_300, ov.totalLearned >= 300)
        tryUnlock(Achievements.LEARN_500, ov.totalLearned >= 500)
        tryUnlock(Achievements.LEARN_800, ov.totalLearned >= 800)
        tryUnlock(Achievements.LEARN_1000, ov.totalLearned >= 1000)
        tryUnlock(Achievements.LEARN_1500, ov.totalLearned >= 1500)
        tryUnlock(Achievements.LEARN_2000, ov.totalLearned >= 2000)
        tryUnlock(Achievements.LEARN_3000, ov.totalLearned >= 3000)
        tryUnlock(Achievements.LEARN_5000, ov.totalLearned >= 5000)

        // ---- 掌握词数 ----
        tryUnlock(Achievements.MASTER_50, ov.mastered >= 50)
        tryUnlock(Achievements.MASTER_100, ov.mastered >= 100)
        tryUnlock(Achievements.MASTER_200, ov.mastered >= 200)
        tryUnlock(Achievements.MASTER_300, ov.mastered >= 300)
        tryUnlock(Achievements.MASTER_500, ov.mastered >= 500)
        tryUnlock(Achievements.MASTER_800, ov.mastered >= 800)
        tryUnlock(Achievements.MASTER_1000, ov.mastered >= 1000)
        tryUnlock(Achievements.MASTER_2000, ov.mastered >= 2000)

        // ---- 连续打卡（当前连续 / 历史最长分开判：断签后历史记录仍该保留）----
        tryUnlock(Achievements.STREAK_3, ov.streakDays >= 3)
        tryUnlock(Achievements.STREAK_7, ov.streakDays >= 7)
        tryUnlock(Achievements.STREAK_10, ov.streakDays >= 10)
        tryUnlock(Achievements.STREAK_15, ov.streakDays >= 15)
        tryUnlock(Achievements.STREAK_21, ov.streakDays >= 21)
        tryUnlock(Achievements.STREAK_30, ov.streakDays >= 30)
        tryUnlock(Achievements.STREAK_60, ov.streakDays >= 60)
        tryUnlock(Achievements.STREAK_100, ov.streakDays >= 100)
        tryUnlock(Achievements.STREAK_180, ov.streakDays >= 180)
        tryUnlock(Achievements.STREAK_365, ov.streakDays >= 365)
        tryUnlock(Achievements.BEST_STREAK_7, ov.bestStreak >= 7)
        tryUnlock(Achievements.BEST_STREAK_30, ov.bestStreak >= 30)
        tryUnlock(Achievements.BEST_STREAK_100, ov.bestStreak >= 100)
        tryUnlock(Achievements.BEST_STREAK_365, ov.bestStreak >= 365)

        // ---- 闯关次数 ----
        tryUnlock(Achievements.QUIZ_FIRST, ov.quizPlayed >= 1)
        tryUnlock(Achievements.QUIZ_5, ov.quizPlayed >= 5)
        tryUnlock(Achievements.QUIZ_10, ov.quizPlayed >= 10)
        tryUnlock(Achievements.QUIZ_20, ov.quizPlayed >= 20)
        tryUnlock(Achievements.QUIZ_50, ov.quizPlayed >= 50)
        tryUnlock(Achievements.QUIZ_100, ov.quizPlayed >= 100)

        // ---- 单次闯关得分（历史最高分已包含刚写入的本局记录）----
        tryUnlock(Achievements.QUIZ_SCORE_100, ov.bestScore >= 100)
        tryUnlock(Achievements.QUIZ_SCORE_200, ov.bestScore >= 200)
        tryUnlock(Achievements.QUIZ_SCORE_300, ov.bestScore >= 300)
        tryUnlock(Achievements.QUIZ_SCORE_400, ov.bestScore >= 400)
        tryUnlock(Achievements.QUIZ_SCORE_500, ov.bestScore >= 500)

        // ---- 累计学习时长 ----
        tryUnlock(Achievements.MINUTES_30, ov.totalMinutes >= 30)
        tryUnlock(Achievements.MINUTES_60, ov.totalMinutes >= 60)
        tryUnlock(Achievements.MINUTES_120, ov.totalMinutes >= 120)
        tryUnlock(Achievements.MINUTES_300, ov.totalMinutes >= 300)
        tryUnlock(Achievements.MINUTES_600, ov.totalMinutes >= 600)
        tryUnlock(Achievements.MINUTES_1200, ov.totalMinutes >= 1200)

        // ---- 收藏 ----
        tryUnlock(Achievements.FAV_20, favCount >= 20)
        tryUnlock(Achievements.FAV_50, favCount >= 50)
        tryUnlock(Achievements.FAV_100, favCount >= 100)
        tryUnlock(Achievements.FAV_200, favCount >= 200)

        // ---- 正确率（题目太少时正确率没有统计意义，因此按档位设作答数下限）----
        tryUnlock(Achievements.ACCURACY_90, answered >= 50 && ov.accuracy >= 90)
        tryUnlock(Achievements.ACCURACY_95, answered >= 100 && ov.accuracy >= 95)
        tryUnlock(Achievements.ACCURACY_98, answered >= 200 && ov.accuracy >= 98)

        // ---- 单日学习量 ----
        tryUnlock(Achievements.GOAL_DONE, ov.todayLearned >= dailyGoal)
        tryUnlock(Achievements.DAILY_50, ov.todayLearned >= 50)
        tryUnlock(Achievements.DAILY_100, ov.todayLearned >= 100)

        // ---- 学习时段 ----
        tryUnlock(Achievements.NIGHT_OWL, hour >= 22 && ov.totalLearned >= 1)
        tryUnlock(Achievements.EARLY_BIRD, hour in 1..6 && ov.totalLearned >= 1)
        tryUnlock(Achievements.MIDNIGHT, hour in 0..1 && ov.totalLearned >= 1)
        tryUnlock(Achievements.NOON_STUDY, hour == 12 && ov.totalLearned >= 1)

        // 本次闯关专属的成就。QUIZ_SCORE_* 上面已按历史最高分判过一次，
        // 这里再按「本局得分」判一次是有意为之：本局刷出高分也应立即解锁。
        // 连击只有本局才知道（历史上没有存 maxCombo 的聚合值），只能放在这里判。
        lastQuiz?.let {
            tryUnlock(Achievements.QUIZ_PERFECT, it.total > 0 && it.correct == it.total)
            tryUnlock(Achievements.QUIZ_COMBO_5, it.maxCombo >= 5)
            tryUnlock(Achievements.QUIZ_COMBO_10, it.maxCombo >= 10)
            tryUnlock(Achievements.QUIZ_COMBO_20, it.maxCombo >= 20)
            tryUnlock(Achievements.QUIZ_COMBO_30, it.maxCombo >= 30)
            tryUnlock(Achievements.QUIZ_COMBO_50, it.maxCombo >= 50)
            tryUnlock(Achievements.QUIZ_SCORE_100, it.score >= 100)
            tryUnlock(Achievements.QUIZ_SCORE_200, it.score >= 200)
            tryUnlock(Achievements.QUIZ_SCORE_300, it.score >= 300)
            tryUnlock(Achievements.QUIZ_SCORE_400, it.score >= 400)
            tryUnlock(Achievements.QUIZ_SCORE_500, it.score >= 500)

            // 专题关卡成就：只在对应的专题结算时才去查表，
            // 避免普通单元 / 首字母闯关也白白多跑几次 COUNT 查询。
            when (it.unit) {
                QUIZ_SCOPE_CONFUSABLE -> {
                    tryUnlock(Achievements.CONFUSABLE_FIRST, it.total > 0)
                    val perfect = userDb.quizPerfectCountOfUnit(QUIZ_SCOPE_CONFUSABLE)
                    tryUnlock(Achievements.CONFUSABLE_PERFECT_5, perfect >= 5)
                    tryUnlock(Achievements.CONFUSABLE_PERFECT_10, perfect >= 10)
                    tryUnlock(Achievements.CONFUSABLE_PERFECT_20, perfect >= 20)
                }
                QUIZ_SCOPE_TRANSLATION -> {
                    tryUnlock(Achievements.TRANSLATION_FIRST, it.total > 0)
                    val perfect = userDb.quizPerfectCountOfUnit(QUIZ_SCOPE_TRANSLATION)
                    tryUnlock(Achievements.TRANSLATION_PERFECT_5, perfect >= 5)
                    tryUnlock(Achievements.TRANSLATION_PERFECT_10, perfect >= 10)
                    tryUnlock(Achievements.TRANSLATION_PERFECT_20, perfect >= 20)
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
        // 词库安装标记与用户设置同处 vocab_meta 这一个 prefs 文件。
        // 无脑 clear() 会把 words_db_version 一起清掉，于是下次冷启动
        // 「已装版本(0) != 内置版本(6)」成立，首个词库查询要同步从 assets
        // 重拷 7.98 MB 词库，表现为「清空数据后第一次变卡」。清完必须把它写回去。
        val installedVersion = prefs.getInt(WordDb.PREFS_DB_VERSION_KEY, 0)
        prefs.edit().clear().putInt(WordDb.PREFS_DB_VERSION_KEY, installedVersion).apply()
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
        private const val KEY_ANIM = "animations_enabled"

        /**
         * 单次传入 [WordDb.wordsByIds] 的 id 上限。
         * 与词库内部的 SQL 变量分批保持同一量级，避免一次性把上万个 id 拼进一条语句。
         */
        private const val ID_BATCH = 500

        /**
         * 「掌握」的最低等级。[levelLabel] 里 3 档文案是「熟悉」，
         * 掌握词数（成就、总览）按 level >= 3 起算，两处必须用同一个常量，
         * 不要再散着写魔法数字。
         */
        private const val MASTER_LEVEL = 3

        /** 最高等级「精通」（levelLabel 的 5 档）：达到该档的词不再占用错词本 */
        private const val LEVEL_EXPERT = 5
    }
}
