package com.hdu.cetvocab.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdu.cetvocab.CetVocabApp
import com.hdu.cetvocab.data.AchievementDef
import com.hdu.cetvocab.data.BackupManager
import com.hdu.cetvocab.data.DayStat
import com.hdu.cetvocab.data.QUIZ_SCOPE_CONFUSABLE
import com.hdu.cetvocab.data.QUIZ_SCOPE_TRANSLATION
import com.hdu.cetvocab.data.QuizKind
import com.hdu.cetvocab.data.QuizAggregate
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.STAGE_QUIZ_COUNT
import com.hdu.cetvocab.data.StudyOverview
import com.hdu.cetvocab.data.UNIT_QUIZ_COUNT
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.data.quizLetterOfScope
import com.hdu.cetvocab.data.quizScopeOfLetter
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.util.LauncherIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- UI 状态 ----------------

data class HomeUiState(
    val loading: Boolean = true,
    val stage: Int = WordDb.STAGE_CET4,
    /** 当前级别的总词数，用于计算「目前进度」 */
    val totalWords: Int = 0,
    /** 当前级别的已学词数（注意：与 overview.totalLearned 的全局口径不同） */
    val stageLearned: Int = 0,
    val overview: StudyOverview = StudyOverview(),
    val goal: Int = 20,
    val weekStats: List<DayStat> = emptyList(),
    val dueCount: Int = 0,
    val queueCount: Int = 0
)

data class UnitItem(
    val unit: Int,
    val learned: Int,
    val total: Int,
    val fromWord: String,
    val toWord: String
) {
    val progress: Float get() = if (total <= 0) 0f else learned.toFloat() / total
    val done: Boolean get() = total > 0 && learned >= total
}

/** 首字母分组（闯关页按 A–Z 选关） */
data class LetterItem(
    val letter: Char,
    val learned: Int,
    val total: Int
) {
    val progress: Float get() = if (total <= 0) 0f else learned.toFloat() / total
    val done: Boolean get() = total > 0 && learned >= total
}

data class LearnUiState(
    val loading: Boolean = true,
    val stage: Int = WordDb.STAGE_CET4,
    val units: List<UnitItem> = emptyList(),
    val letters: List<LetterItem> = emptyList(),
    val totalWords: Int = 0,
    val learnedWords: Int = 0,
    val query: String = "",
    val searchResult: List<Word> = emptyList(),
    /** 搜索结果条数正好等于 [WordDb.SEARCH_LIMIT]：很可能还有没显示出来的匹配项 */
    val searchTruncated: Boolean = false,
    val allWords: List<Word> = emptyList(),
    val allWordsLoading: Boolean = false,
    val favWords: List<Word> = emptyList(),
    val wrongWords: List<Word> = emptyList(),
    /**
     * 已学单词的掌握等级（wordId -> level，只含 level > 0 的词）。
     * 供词库列表的掌握度圆点使用；此前圆点是硬编码 0，永远显示「未学」。
     */
    val levels: Map<Int, Int> = emptyMap()
)

data class StudyUiState(
    val title: String = "",
    val queue: List<Word> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val favIds: Set<Int> = emptySet(),
    val currentLevel: Int = 0,
    val finished: Boolean = false,
    val newAchievements: List<AchievementDef> = emptyList()
) {
    val current: Word? get() = queue.getOrNull(index)
    val total: Int get() = queue.size
}

data class QuizUiState(
    val active: Boolean = false,
    val stage: Int = WordDb.STAGE_CET4,
    val unit: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int = -1,
    /**
     * 多选题已勾选的选项下标。
     * 单选继续用 [selected]，多选走这个集合，两者分开是为了让
     * 「已选」的判定在两种题型下都保持单一来源、不会互相污染。
     */
    val selectedSet: Set<Int> = emptySet(),
    val typed: String = "",
    val revealed: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val finished: Boolean = false,
    /**
     * 已累计的答题用时（毫秒）。
     * 不用「当前时间 - 开始时间」结算，是因为那样会把用户切到别的页面、
     * 甚至把应用退到后台的时间也算进去，导致排行榜里的「用时」虚高。
     */
    val elapsedMs: Long = 0L,
    val lastRecord: QuizRecord? = null,
    val stars: Int = 0,
    val newAchievements: List<AchievementDef> = emptyList()
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
    val total: Int get() = questions.size
    /** 首字母挑战时的字母；单元 / 全词挑战返回 null */
    val letter: Char? get() = quizLetterOfScope(unit)
}

data class ProfileUiState(
    val loading: Boolean = true,
    val overview: StudyOverview = StudyOverview(),
    val unlocked: Map<String, Long> = emptyMap(),
    val weekStats: List<DayStat> = emptyList(),
    val goal: Int = 20,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 0.92f,
    val stage: Int = WordDb.STAGE_CET4,
    val iconStyle: Int = LauncherIcons.STYLE_DEFAULT,
    val themeMode: Int = VocabRepository.THEME_SYSTEM,
    /** 界面动效开关（设置页）。false 时转场、弹簧与按压回弹全部退化为瞬间落位。 */
    val animationsEnabled: Boolean = true,
    val leaderboard: List<QuizRecord> = emptyList(),
    /**
     * 排行榜顶部统计的聚合口径（次数 / 最高分 / 平均分 / 最高正确率）。
     * 由 SQL 在**全表**上算，不受 [leaderboard] 最多 50 条的限制。
     */
    val aggregate: QuizAggregate = QuizAggregate()
)

// ---------------- ViewModel ----------------

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: VocabRepository = (app as CetVocabApp).repo

    /** 数据导出与备份/恢复（文件写入应用外部私有目录，无需存储权限） */
    val backup: BackupManager by lazy { BackupManager(getApplication(), repo.userDb, repo.wordDb) }

    init {
        // 动效开关要在首帧之前就同步给动效系统：Motion 的默认值是「开」，
        // 若等 refreshAll() 落地，上次关闭了动效的用户在重启后会看到几帧动画。
        Motion.applyAnimationsEnabled(repo.animationsEnabled)
    }

    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home.asStateFlow()

    private val _learn = MutableStateFlow(LearnUiState())
    val learn: StateFlow<LearnUiState> = _learn.asStateFlow()

    private val _study = MutableStateFlow(StudyUiState())
    val study: StateFlow<StudyUiState> = _study.asStateFlow()

    private val _quiz = MutableStateFlow(QuizUiState())
    val quiz: StateFlow<QuizUiState> = _quiz.asStateFlow()

    private val _profile = MutableStateFlow(ProfileUiState())
    val profile: StateFlow<ProfileUiState> = _profile.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // ---------- 协程句柄 ----------
    // 每一类刷新都保留 Job 引用，启动新的之前先取消旧的。
    // 否则快速切换级别（四级→六级→四级）会叠加多组刷新协程，完成顺序不确定，
    // 慢的那个后完成就会把新数据覆盖成旧数据 —— 界面显示四级的词数、
    // 而切换按钮已经高亮在六级。
    private var homeJob: Job? = null
    private var learnJob: Job? = null
    private var profileJob: Job? = null
    private var searchJob: Job? = null
    private var finishJob: Job? = null

    /**
     * 作答处理中标志。
     * 「有印象 / 不认识」按钮连点两下时，两个协程会读到同一份快照、算出同一个 nextIndex，
     * 结果是同一道题被记两次而索引只 +1（跳过下一个单词）。用它把并发收成串行。
     */
    private var answering = false

    /** 闯关结算中标志：保证一次闯关只写入一条成绩记录 */
    private var finishing = false

    /** 当前题目的起始时刻；0 表示计时处于暂停态（应用在后台、或闯关页不在前台） */
    private var questionStartTs = 0L

    /** 闯关页是否在前台（在组合中）。用于区分「切到别的标签」与「退到后台」 */
    private var quizScreenActive = false

    // ---------- 学习时长计时 ----------
    // 只累计「学习页在前台 **且** 应用在前台」的时间。
    // 历史实现用 DisposableEffect 记下进入时刻、在 onDispose 时一次性结算，
    // 中途切到别的页面、或把应用退到后台停留的时间都会被算进去，导致累计学习时长虚高。
    private var studyScreenActive = false
    private var studyTimerStart = 0L

    /**
     * 学习时长结算时不足 1 秒的余数（毫秒）。
     * 见 [flushStudyTimer]：整除会把每个片段的尾巴丢掉，高频进出会系统性少记。
     */
    private var studyTimerCarryMs = 0L

    val stage: Int get() = repo.stage
    val dailyGoal: Int get() = repo.dailyGoal
    val autoSpeak: Boolean get() = repo.autoSpeak
    val speechRate: Float get() = repo.speechRate

    fun consumeToast() {
        _toast.value = null
    }

    private fun notify(msg: String) {
        _toast.value = msg
    }

    // ---------- 首页 ----------

    fun refreshHome() {
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                // 级别只读一次：中途被改会导致「按四级取 id、按六级算总数」这类口径不一致
                val stage = repo.stage
                val overview = repo.overview()
                // 待复习要取真实总数，不能用 dueWords(40).size：后者末尾是 take(40)，
                // 到期词积压到上百个时首页会一直显示「待复习 40 词」，看不出欠了多少
                val due = repo.dueCount()
                val queue = repo.todayQueue()
                // 「目前进度」只算当前级别：全局已学数会把另一个级别也算进来，不能直接用
                val stageIds = repo.wordDb.idsOfStage(stage).toHashSet()
                val learnedInStage = repo.userDb.states()
                    .count { (id, st) -> st.level > 0 && id in stageIds }
                HomeUiState(
                    loading = false,
                    stage = stage,
                    totalWords = repo.wordDb.countOfStage(stage),
                    stageLearned = learnedInStage,
                    overview = overview,
                    goal = repo.dailyGoal,
                    weekStats = repo.userDb.recentDays(7),
                    dueCount = due,
                    queueCount = queue.size
                )
            }
            _home.value = data
        }
    }

    fun setStage(stage: Int) {
        repo.stage = stage
        // 学习进度按 word_id 独立存放，两个级别互不影响，切换时不需要重置任何进度。
        // 这里只把新级别同步进首页状态，让按钮选中态与切换动画立即响应，
        // 词数 / 进度等具体数据随后由 refreshAll() 异步补齐（本地库，通常几十毫秒）。
        _home.update { it.copy(stage = stage) }
        refreshAll()
    }

    /**
     * 切换桌面图标风格。
     *
     * 这里只改偏好，不立刻写系统组件状态：被切换的 activity-alias 正是当前任务的根组件，
     * 一旦当场停用，系统会连带结束当前任务，把用户直接顶回桌面。
     * 真正落地放在 [com.hdu.cetvocab.MainActivity.onStop]（应用退到后台时），
     * 用户回到桌面时新图标已经就绪，切换过程无感。
     */
    fun setIconStyle(style: Int) {
        repo.iconStyle = style
        _profile.update { it.copy(iconStyle = style) }
        notify(
            if (style == LauncherIcons.STYLE_SIMPLE) "已选择简洁图标，回到桌面即可看到"
            else "已切回默认图标"
        )
    }

    /** 切换明暗模式：跟随系统 / 始终浅色 / 始终深色。 */
    fun setThemeMode(mode: Int) {
        repo.themeMode = mode
        _profile.update { it.copy(themeMode = mode) }
        notify(
            when (mode) {
                VocabRepository.THEME_LIGHT -> "已切换为浅色模式"
                VocabRepository.THEME_DARK -> "已切换为深色模式"
                else -> "已改为跟随系统"
            }
        )
    }

    fun setDailyGoal(goal: Int) {
        repo.dailyGoal = goal
        refreshHome()
        refreshProfile()
    }

    fun setAutoSpeak(enabled: Boolean) {
        repo.autoSpeak = enabled
        _profile.update { it.copy(autoSpeak = enabled) }
    }

    fun setSpeechRate(rate: Float) {
        repo.speechRate = rate
        _profile.update { it.copy(speechRate = rate) }
    }

    /**
     * 切换界面动效。
     *
     * 两步都不能省：写偏好只保证下次启动还生效，同步给 [Motion] 才能让**当前这次**
     * 立刻生效 —— 否则用户要重开应用才看到效果，会被当成开关坏了。
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        repo.animationsEnabled = enabled
        Motion.applyAnimationsEnabled(enabled)
        _profile.update { it.copy(animationsEnabled = enabled) }
    }

    fun resetAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.resetAll() }
            // 一并清掉进行中的学习 / 闯关状态与并发标志，
            // 否则清空数据后仍可能停在上一次的答题或结算页上。
            answering = false
            finishing = false
            questionStartTs = 0L
            studyTimerStart = 0L
            studyTimerCarryMs = 0L
            studyScreenActive = false
            quizScreenActive = false
            _study.value = StudyUiState()
            _quiz.value = QuizUiState()
            refreshAll()
            notify("学习数据已清空")
        }
    }

    fun refreshAll() {
        // 备份恢复 / 清空数据都会改到动效开关，这里统一再同步一次。
        Motion.applyAnimationsEnabled(repo.animationsEnabled)
        refreshHome()
        refreshLearn()
        refreshProfile()
    }

    // ---------- 学习 ----------

    fun refreshLearn() {
        learnJob?.cancel()
        learnJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val stage = repo.stage
                val states = repo.userDb.states()
                val allWords = repo.wordDb.wordsOfStage(stage)
                val unitCount = repo.wordDb.unitCount(stage)
                val units = (1..unitCount).map { u ->
                    val words = repo.wordDb.wordsOfUnit(stage, u)
                    UnitItem(
                        unit = u,
                        learned = words.count { (states[it.id]?.level ?: 0) > 0 },
                        total = words.size,
                        fromWord = words.firstOrNull()?.word ?: "",
                        toWord = words.lastOrNull()?.word ?: ""
                    )
                }
                // 首字母分组（闯关页按 A–Z 选关），复用已加载的全量词表，避免重复查询
                val letters = allWords
                    .filter { it.word.isNotEmpty() }
                    .groupBy { it.word.first().uppercaseChar() }
                    .filterKeys { it in 'A'..'Z' }
                    .toSortedMap()
                    .map { (ch, ws) ->
                        LetterItem(
                            letter = ch,
                            learned = ws.count { (states[it.id]?.level ?: 0) > 0 },
                            total = ws.size
                        )
                    }
                // 顺带把每个词的掌握等级下发，供列表里的掌握度圆点使用
                val levels = states.values
                    .filter { it.level > 0 }
                    .associate { it.wordId to it.level }
                LearnUiState(
                    loading = false,
                    stage = stage,
                    units = units,
                    letters = letters,
                    totalWords = repo.wordDb.countOfStage(stage),
                    learnedWords = allWords.count { (states[it.id]?.level ?: 0) > 0 },
                    levels = levels,
                    favWords = repo.favWords(),
                    wrongWords = repo.wrongWords(40)
                )
            }
            // 用 copy 合并，而不是 `_learn.value = data` 整体替换：
            // refreshLearn 会被收藏、学习结束等操作随时触发，整体替换会把用户
            // 正在浏览的「全部单词」列表和搜索关键词一起清空（列表突然变空、输入框自己清空）。
            //
            // 但**跨级别时必须清空**这些浏览态：四级→六级后 allWords 还留着四级的词表，
            // 「预览全部单词」会显示上一级别的词；搜索框里的关键词与结果同样是上一级别的。
            // 这是数据正确性问题，不能靠「保留体验」搪塞过去。
            _learn.update { old ->
                val sameStage = old.stage == data.stage
                data.copy(
                    query = if (sameStage) old.query else "",
                    searchResult = if (sameStage) old.searchResult else emptyList(),
                    allWords = if (sameStage) old.allWords else emptyList(),
                    allWordsLoading = if (sameStage) old.allWordsLoading else false
                )
            }
        }
    }

    fun previewAllWords() {
        val stage = repo.stage
        // 已按当前级别加载过就直接复用。这里必须同时要求 stage 一致 ——
        // 只看 allWords 非空会把上一级别的词表当成当前级别的结果。
        if (_learn.value.stage == stage && _learn.value.allWords.isNotEmpty()) return
        _learn.update { it.copy(allWordsLoading = true, allWords = emptyList()) }
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) { repo.wordDb.wordsOfStage(stage) }
            // 加载期间用户又切了级别：结果已过期，丢弃而不是覆盖新状态。
            // 丢弃时也必须把 allWordsLoading 落回 false —— 否则一旦这次过期返回是
            // 在 refreshLearn 的合并之后到达，全词预览就永久转圈。
            if (repo.stage != stage) {
                _learn.update { if (it.stage != stage) it.copy(allWordsLoading = false) else it }
                return@launch
            }
            _learn.update {
                it.copy(stage = stage, allWords = words, allWordsLoading = false)
            }
        }
    }

    /**
     * 搜索。
     *
     * 每次击键都查库会带来两个问题：① 输入长词时明显卡顿；
     * ② 先发出的慢查询可能后返回，把新关键词的结果覆盖掉（结果与输入框不符）。
     * 因此这里做 250ms 防抖 + 取消上一个查询。
     */
    fun search(keyword: String) {
        _learn.update { it.copy(query = keyword) }
        searchJob?.cancel()
        if (keyword.isBlank()) {
            _learn.update { it.copy(searchResult = emptyList(), searchTruncated = false) }
            return
        }
        // 搜索结果要跟随当前级别：六级词库包含了四级全部词条，
        // 跨级别搜会让同一个词出现两次。
        //
        // 级别取 repo.stage（真值来源）而不是 _learn.value.stage：
        // 后者要等 refreshLearn 跑完才更新，用户刚切级别就输入时读到的是旧级别。
        val stage = repo.stage
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val result = withContext(Dispatchers.IO) { repo.wordDb.search(keyword, stage) }
            // 防抖期间又切了级别：这次结果属于旧级别，丢弃
            if (repo.stage != stage) return@launch
            // 命中数正好等于上限时，几乎一定还有匹配项没显示出来
            // （例如搜常见字母「a」）。不标出来的话，用户会以为整个级别只有这些词。
            _learn.update {
                it.copy(
                    searchResult = result,
                    searchTruncated = result.size >= WordDb.SEARCH_LIMIT
                )
            }
        }
    }

    // ---------- 背单词流程 ----------

    /**
     * 各个学习入口统一带一个 [onReady] 回调，**只有真的取到词了才让调用方跳转**。
     *
     * 此前是「先 navigate(STUDY)，再异步装队列」：
     * · 生词本 / 错词本为空时，VM 里 `notify("还没有收藏的生词")` 直接 return，
     *   可页面已经跳过去了，用户看到的是学习页上的「本组暂无单词」+ 一条提示；
     * · 队列加载期间（冷启动首次还要拷词库）学习页会先闪一下空态。
     * 回调在主线程触发（viewModelScope 默认 Main.immediate），可以安全地拿它做导航。
     */
    fun startTodayStudy(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.todayQueue() }
            startQueue("今日学习", queue, onReady)
        }
    }

    /**
     * 今日目标完成后的「继续学习」：先还清到期复习，再预学一批新词（超前学习）。
     * 与「开始今日学习」共用同一套学习页与结算逻辑，只是取数口径不同。
     */
    fun startExtraStudy(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.extraQueue() }
            startQueue("继续学习", queue, onReady)
        }
    }

    fun startUnitStudy(unit: Int, onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.unitWords(unit) }
            startQueue("第 $unit 单元", queue, onReady)
        }
    }

    fun startFavStudy(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.favWords() }
            if (queue.isEmpty()) {
                notify("还没有收藏的生词")
                onReady(false)
                return@launch
            }
            startQueue("生词本复习", queue, onReady)
        }
    }

    fun startWrongStudy(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.wrongWords(50) }
            if (queue.isEmpty()) {
                notify("暂时没有错词记录")
                onReady(false)
                return@launch
            }
            startQueue("错词强化", queue, onReady)
        }
    }

    private suspend fun startQueue(title: String, queue: List<Word>, onReady: (Boolean) -> Unit) {
        if (queue.isEmpty()) {
            notify("这里暂时没有需要学习的单词")
            onReady(false)
            return
        }
        val favIds = withContext(Dispatchers.IO) {
            repo.userDb.states().values.filter { it.fav }.map { it.wordId }.toSet()
        }
        _study.value = StudyUiState(
            title = title,
            queue = queue,
            index = 0,
            favIds = favIds,
            currentLevel = repo.userDb.stateOf(queue.first().id).level
        )
        onReady(true)
    }

    fun flipCard() {
        _study.update { it.copy(flipped = !it.flipped) }
    }

    /**
     * 记录一次作答并推进到下一个单词。
     *
     * 三个要点：
     * 1. 用 [answering] 把并发收成串行。历史实现先取 `_study.value` 快照、
     *    再在协程里用快照算 `nextIndex`；连点两下时两个协程读到同一份快照、
     *    算出同一个 nextIndex —— 同一题被记两次，而索引只 +1，导致跳过下一个单词。
     * 2. 索引推进放在 `update { }` 里基于**当前**状态计算，不再依赖挂起前的快照。
     * 3. 成就只评估一次并保留返回值。历史实现调了两次 `evaluateAchievements()`、
     *    第一次的结果被丢弃，而 `unlock()` 对已解锁成就会返回 false，
     *    于是「在最后一个单词上解锁的成就」提示永远丢失。
     */
    fun answerCurrent(known: Boolean) {
        if (answering) return
        val snapshot = _study.value
        val word = snapshot.current ?: return
        answering = true
        viewModelScope.launch {
            try {
                // 写库、取下一个词的等级、评估成就，一次 IO 里做完，避免主线程碰数据库
                val nextWordLevel = withContext(Dispatchers.IO) {
                    repo.answerWord(word, known)
                    repo.userDb.stateOf(snapshot.queue.getOrNull(snapshot.index + 1)?.id ?: -1)
                        .level
                }
                val gained = withContext(Dispatchers.IO) { repo.evaluateAchievements() }
                _study.update { s ->
                    // 学习会话已被替换（例如用户退出后重开）时，不要把旧队列的进度写进去
                    if (s.queue !== snapshot.queue) return@update s
                    val next = s.index + 1
                    val done = next >= s.queue.size
                    s.copy(
                        index = next,
                        flipped = false,
                        knownCount = s.knownCount + if (known) 1 else 0,
                        unknownCount = s.unknownCount + if (known) 0 else 1,
                        currentLevel = if (done) s.currentLevel else nextWordLevel,
                        finished = done,
                        // 整场累计的成就必须一路累加。此前写成
                        // `if (done) gained else s.newAchievements`：未结束时不累加、
                        // 结束时又用「最后一个单词那次评估的结果」整体覆盖，
                        // 于是第 5 个、第 23 个词解锁的成就在结算页和提示里都看不到
                        // （库里确实解锁了，纯粹是提示丢失）。
                        newAchievements = s.newAchievements + gained
                    )
                }
                if (_study.value.finished) {
                    // 进入结算页即停表：结算页的停留时间不算学习时长，
                    // 与闯关侧「结算页不计入本关用时」的口径保持一致。
                    flushStudyTimer()
                    refreshHome()
                }
            } finally {
                answering = false
            }
        }
    }

    fun toggleFavCurrent() {
        val word = _study.value.current ?: return
        viewModelScope.launch {
            val fav = withContext(Dispatchers.IO) { repo.toggleFav(word) }
            _study.update {
                val set = it.favIds.toMutableSet()
                if (fav) set.add(word.id) else set.remove(word.id)
                it.copy(favIds = set)
            }
            refreshLearn()
        }
    }

    fun toggleFav(word: Word) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.toggleFav(word) }
            refreshLearn()
            refreshProfile()
        }
    }

    fun finishStudy() {
        val state = _study.value
        val gained = state.newAchievements
        _study.value = StudyUiState()
        refreshAll()
        if (gained.isNotEmpty()) {
            notify("解锁成就：" + gained.joinToString("、") { it.title })
        }
    }

    /** 学习页进入组合时调用：开始累计学习时长 */
    fun onStudyScreenEnter() {
        studyScreenActive = true
        resumeStudyTimer()
    }

    /** 学习页离开组合时调用：结算并停止累计 */
    fun onStudyScreenExit() {
        studyScreenActive = false
        flushStudyTimer()
    }

    /**
     * 应用回到前台：恢复学习时长与闯关用时的计时。
     *
     * 注意顺序——必须**先**恢复学习计时再恢复闯关计时，两者互不影响，
     * 但统一入口能保证「前后台」只有一个真相来源，不会出现某处漏判。
     */
    fun onAppForeground() {
        resumeStudyTimer()
        // 只有闯关页确实在前台时才恢复本题计时。
        // 否则会出现这种情况：用户把应用退到后台时正停在「首页」，
        // 回到前台后计时被恢复，而人还在首页 —— 这段停留会被算进本关用时。
        if (quizScreenActive && _quiz.value.active && !_quiz.value.finished && questionStartTs <= 0) {
            questionStartTs = System.currentTimeMillis()
        }
    }

    /**
     * 应用退到后台：结算学习时长与本题用时。
     *
     * 若不结算，用户切出去几分钟再回来，这段时间会被算进「本关用时」与「累计学习时长」，
     * 排行榜里的「用时 N 秒」因此虚高、失去参考价值。
     */
    fun onAppBackground() {
        flushStudyTimer()
        pauseQuizTimer()
    }

    /**
     * 闯关页离开组合：结算并暂停本题计时。
     *
     * 用户点底部标签切到别的页面时，应用并没有退到后台，[onAppBackground] 不会被调用；
     * 若不在这里停表，离开的这段时间会被算进本关用时。进入组合时再恢复。
     */
    fun onQuizScreenExit() {
        quizScreenActive = false
        pauseQuizTimer()
    }

    /** 闯关页进入组合：若本局仍在进行且处于停表状态，重新起表。 */
    fun onQuizScreenEnter() {
        quizScreenActive = true
        if (_quiz.value.active && !_quiz.value.finished && questionStartTs <= 0) {
            questionStartTs = System.currentTimeMillis()
        }
    }

    /** 把「当前这一题」已经花掉的时间结算进 elapsedMs，并停表（questionStartTs 置 0）。 */
    private fun pauseQuizTimer() {
        if (questionStartTs <= 0) return
        val spent = (System.currentTimeMillis() - questionStartTs).coerceAtLeast(0)
        questionStartTs = 0L
        _quiz.update { if (it.active && !it.finished) it.copy(elapsedMs = it.elapsedMs + spent) else it }
    }

    private fun resumeStudyTimer() {
        // 队列已空（本组完成 / 无词可学）时不起表：
        // 结算页与空页的停留不该计入学习时长，口径与闯关侧一致。
        if (studyScreenActive && studyTimerStart <= 0L && _study.value.current != null) {
            studyTimerStart = System.currentTimeMillis()
        }
    }

    /**
     * 结算并暂停学习时长计时。
     *
     * 用 [studyTimerCarryMs] 保存不足 1 秒的余数：此前每次结算都做 `(now - start) / 1000`
     * 的整除，一个 40 秒的片段记 40 秒、0.9 秒的片段记 0 秒，用户高频进出学习页时
     * 累计时长会系统性偏低。余数攒满 1 秒才落库，长期累计与真实时间一致。
     */
    private fun flushStudyTimer() {
        val start = studyTimerStart
        studyTimerStart = 0L
        if (start <= 0L) return
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L) + studyTimerCarryMs
        val seconds = elapsed / 1000
        studyTimerCarryMs = elapsed % 1000
        if (seconds > 0) addStudySeconds(seconds)
    }

    private fun addStudySeconds(seconds: Long) {
        if (seconds <= 0) return
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.addStudySeconds(seconds) } }
    }

    // ---------- 闯关 ----------

    /**
     * 开局：装好题目、进入答题态，并把上一局的残留标志全部复位。
     *
     * [finishing] 必须在这里复位。它是 `finishQuiz()` 的幂等标志，过去的实现只在
     * [exitQuiz] 里清零 —— 而结算页的「再来一局」是直接调 `start*Quiz`、不经过退出流程，
     * 于是第二局答到最后一题时 `finishQuiz()` 一进门就被 `if (finishing) return` 拦掉：
     * 成绩不入库、成就判不到、结算页不出现，界面死在最后一题（只能点 X 退出）。
     * 把复位收敛到这一个开局入口，任何新的开局路径都不会再漏。
     */
    private fun openQuiz(
        challengeStage: Int,
        unit: Int,
        questions: List<QuizQuestion>,
        onReady: (Boolean) -> Unit = {}
    ) {
        finishJob?.cancel()
        finishJob = null
        finishing = false
        _quiz.value = QuizUiState(
            active = true,
            stage = challengeStage,
            unit = unit,
            questions = questions
        )
        questionStartTs = System.currentTimeMillis()
        onReady(true)
    }

    fun startQuiz(unit: Int) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) {
                if (unit < 0) repo.buildStageQuiz(count = STAGE_QUIZ_COUNT) else repo.buildQuiz(unit, UNIT_QUIZ_COUNT)
            }
            if (questions.isEmpty()) {
                notify(if (unit < 0) "全词挑战暂不可用" else "该单元单词不足，换一个单元试试")
                return@launch
            }
            openQuiz(challengeStage, unit, questions)
        }
    }

    /**
     * 易混词辨析专题：形近词四选一。
     *
     * 与单元 / 首字母挑战不同，这个专题不按范围取词，而是直接遍历本级词库里
     * 带形近词注解的词条，因此不需要「单词数不足」的前置判断——
     * 但仍保留了出题为 0 时的提示，避免数据库异常时静默进入空关卡。
     */
    fun startConfusableQuiz(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) { repo.buildConfusableQuiz(UNIT_QUIZ_COUNT) }
            if (questions.isEmpty()) {
                notify("易混词辨析暂不可用")
                onReady(false)
                return@launch
            }
            openQuiz(challengeStage, QUIZ_SCOPE_CONFUSABLE, questions, onReady)
        }
    }

    /**
     * 真题句子翻译专题：中文句 + 四个译文，选出全部可接受译法（多选）。
     * 题库按级别内置在词库里，六级与四级各自成卷。
     */
    fun startTranslationQuiz(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) { repo.buildTranslationQuiz(UNIT_QUIZ_COUNT) }
            if (questions.isEmpty()) {
                notify("真题句子翻译题库暂不可用")
                onReady(false)
                return@launch
            }
            openQuiz(challengeStage, QUIZ_SCOPE_TRANSLATION, questions, onReady)
        }
    }

    /** 首字母挑战：从该首字母下的全部单词出题（字母不分大小写） */
    fun startLetterQuiz(letter: Char) {
        // 非 A–Z 的字符算不出合法的范围值，直接拒绝而不是让它和单元号碰撞
        val scope = quizScopeOfLetter(letter) ?: run {
            notify("「$letter」不是有效的首字母")
            return
        }
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) { repo.buildLetterQuiz(letter, UNIT_QUIZ_COUNT) }
            if (questions.isEmpty()) {
                notify("「${letter.uppercaseChar()}」开头的单词不足，换一个字母试试")
                return@launch
            }
            openQuiz(challengeStage, scope, questions)
        }
    }

    fun selectOption(index: Int) {
        val question = _quiz.value.current ?: return
        applyAnswer(index == question.correctIndex, selected = index)
    }

    /**
     * 多选题勾选 / 取消勾选。
     * 只在未揭晓答案时生效：揭晓后再点会让人以为还能改答案。
     */
    fun toggleTranslationOption(index: Int) {
        _quiz.update { s ->
            if (s.revealed) return@update s
            val next = if (index in s.selectedSet) s.selectedSet - index else s.selectedSet + index
            s.copy(selectedSet = next)
        }
    }

    /** 多选题提交：必须与正确译法完全一致（不多选也不少选）才算答对 */
    fun submitTranslation() {
        val state = _quiz.value
        val question = state.current ?: return
        if (state.selectedSet.isEmpty()) {
            notify("请先选择译文")
            return
        }
        val correct = state.selectedSet == question.answerSet
        applyAnswer(correct, selected = state.selectedSet.minOrNull() ?: -1)
    }

    fun submitSpelling() {
        val state = _quiz.value
        val question = state.current ?: return
        val correct = state.typed.trim().equals(question.word.word.trim(), ignoreCase = true)
        applyAnswer(correct, selected = state.selected)
    }

    fun updateTyped(text: String) {
        _quiz.update { it.copy(typed = text) }
    }

    /**
     * 落定本次作答。
     *
     * `revealed` 的判定必须放在 `update { }` **内部**：外面先读 `_quiz.value` 再调用，
     * 两步之间存在窗口，连点两下时两个调用都会通过外面的检查、各自加一次分。
     * 放到 update 里判等于是原子操作，同一题只会结算一次。
     */
    /**
     * 单题基础分。拼写要自己回忆字母、翻译题要同时判断多个译文，
     * 难度都高于普通选义，因此给更高的基础分。
     */
    private fun baseScoreOf(q: QuizQuestion?): Int = when (q?.kind) {
        QuizKind.SPELLING -> 15
        QuizKind.TRANSLATION -> 18
        QuizKind.CONFUSABLE -> 12
        else -> 10
    }

    private fun applyAnswer(correct: Boolean, selected: Int) {
        _quiz.update { s ->
            if (s.revealed) return@update s
            val gain = if (correct) {
                val base = baseScoreOf(s.current)
                base + minOf(s.combo, 5) * 2
            } else 0
            s.copy(
                selected = selected,
                revealed = true,
                score = s.score + gain,
                correctCount = s.correctCount + if (correct) 1 else 0,
                combo = if (correct) s.combo + 1 else 0,
                maxCombo = if (correct) maxOf(s.maxCombo, s.combo + 1) else s.maxCombo
            )
        }
    }

    /**
     * 推进到下一题（最后一题则进入结算）。
     *
     * **必须原子地抢占本题**：`onClick` 读的是组合时的快照，连点两下时两次都会带着
     * 「已揭晓」的状态进来，索引会一次 +2，中间那道题直接被跳过。
     * 这里先在 `update { }` 里把 `revealed` 抢下来 —— 只有抢到的一次才继续推进，
     * 后一次看到的 `revealed` 已经是 false，直接返回。
     * 与 [applyAnswer] 用 `revealed` 判重、[answerCurrent] 用 [answering] 串行是同一套思路。
     */
    fun nextQuestion() {
        var claimed = false
        _quiz.update { s ->
            if (s.active && s.revealed) {
                claimed = true
                s.copy(revealed = false)
            } else s
        }
        if (!claimed) return

        // 先把本题用时结算进 elapsedMs，再推进
        val now = System.currentTimeMillis()
        val spent = if (questionStartTs > 0) (now - questionStartTs).coerceAtLeast(0) else 0L
        // 先停表；确认还有下一题时再重新起表。
        // 若直接置成 now，最后一题结算完 questionStartTs 仍非 0，
        // 之后任何一次「暂停计时」都会把结算页停留的时间也算进本关用时。
        questionStartTs = 0L

        val state = _quiz.value
        val next = state.index + 1
        if (next >= state.questions.size) {
            _quiz.update { it.copy(elapsedMs = it.elapsedMs + spent) }
            finishQuiz()
        } else {
            questionStartTs = now
            _quiz.update {
                it.copy(
                    index = next,
                    selected = -1,
                    selectedSet = emptySet(),
                    typed = "",
                    revealed = false,
                    elapsedMs = it.elapsedMs + spent
                )
            }
        }
    }

    private fun finishQuiz() {
        // 幂等：nextQuestion 判定「已是最后一题」时会调到这里，而 finished 标志要等
        // 数据库写完才置位，中间这段窗口内再次触发就会写入两条闯关记录。
        if (finishing) return
        finishing = true

        val state = _quiz.value
        val duration = state.elapsedMs
        val accuracy = if (state.questions.isEmpty()) 0
        else state.correctCount * 100 / state.questions.size
        val stars = when {
            accuracy >= 90 -> 3
            accuracy >= 70 -> 2
            accuracy >= 50 -> 1
            else -> 0
        }
        finishJob = viewModelScope.launch {
            val gained = withContext(Dispatchers.IO) {
                val record = repo.saveQuizRecord(
                    // 用开局时记下的级别，而不是 repo.stage 实时值：
                    // 答题中途切去别的标签改级别，会让入库级别与题目实际级别不一致
                    stage = state.stage,
                    unit = state.unit,
                    mode = "mix",
                    score = state.score,
                    total = state.questions.size,
                    correct = state.correctCount,
                    maxCombo = state.maxCombo,
                    durationMs = duration
                )
                repo.evaluateAchievements(record) to record
            }
            // 只在本次闯关仍然有效时写入结算态：用户若已点退出，
            // 状态会被重置为 QuizUiState()，此时再置 finished 就会弹出「幽灵结算页」
            _quiz.update { s ->
                if (!s.active || s.finished) s
                else s.copy(
                    finished = true,
                    stars = stars,
                    lastRecord = gained.second,
                    newAchievements = gained.first
                )
            }
            refreshHome()
            refreshProfile()
        }
    }

    fun exitQuiz() {
        finishJob?.cancel()
        finishing = false
        questionStartTs = 0L
        _quiz.value = QuizUiState()
        refreshHome()
        refreshProfile()
    }

    // ---------- 文件任务（导出 / 备份 / 恢复）----------
    // 这些操作必须跑在 ViewModel 作用域里，不能用 rememberCoroutineScope()：
    // 后者绑定页面生命周期，用户点完「备份数据」立刻返回上一页，协程就会被取消，
    // 磁盘上留下一个写了一半的 JSON —— 它还会出现在 listBackups() 里被当成可恢复的备份。

    /** 导出/备份成功后的结果：标题 -> 文件绝对路径 */
    private val _fileDone = MutableStateFlow<Pair<String, String>?>(null)
    val fileDone: StateFlow<Pair<String, String>?> = _fileDone.asStateFlow()

    /** 文件任务失败原因 */
    private val _fileError = MutableStateFlow<String?>(null)
    val fileError: StateFlow<String?> = _fileError.asStateFlow()

    /**
     * 是否有文件任务正在进行。设置页据此禁用「导出 / 备份」四行。
     *
     * 没有这道闸时，ActionRow 连点两下会并发跑两个写盘任务：备份文件名只精确到秒，
     * 同一秒内两次算出同一个路径，而 `writeText` 先 truncate 再写、不是原子操作，
     * 两个线程交错就能写出一个半截 JSON —— 它还会出现在备份列表里被当成可恢复的备份。
     */
    private val _fileBusy = MutableStateFlow(false)
    val fileBusy: StateFlow<Boolean> = _fileBusy.asStateFlow()

    fun consumeFileDone() {
        _fileDone.value = null
    }

    fun consumeFileError() {
        _fileError.value = null
    }

    /**
     * 把异常整理成能看的文案。
     *
     * 之前这里直接把 `exception.message` 丢进对话框正文，而 IO / Security / JSON
     * 异常的 message 多是英文技术串（`Permission denied`、`End of input at character 0 of`），
     * 标题「操作失败」配一行英文栈式描述，用户完全不知道该做什么。
     *
     * 规则：我们自己抛出的中文说明（BackupManager 的 require 文案）原样展示 ——
     * 那些本来就是完整句子；其余异常包一句中文行动建议，技术细节降级成第二行，
     * 方便用户复制给开发者排查。
     */
    private fun fileErrorMessage(action: String, error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        return if (raw.isNotEmpty() && raw.any { it in '一'..'龥' }) raw
        else "$action，请检查本机存储空间是否充足、文件是否可读，然后重试。" +
            "\n（技术信息：${raw.ifBlank { error.javaClass.simpleName }}）"
    }

    private fun runFileTask(title: String, block: () -> File) {
        if (_fileBusy.value) return
        _fileBusy.value = true
        viewModelScope.launch {
            try {
                runCatching { withContext(Dispatchers.IO) { block() } }
                    .onSuccess { _fileDone.value = title to it.absolutePath }
                    .onFailure { _fileError.value = fileErrorMessage("没能完成这次文件操作", it) }
            } finally {
                _fileBusy.value = false
            }
        }
    }

    fun exportStudyCsv() = runFileTask("学习记录已导出") { backup.exportStudyCsv() }

    fun exportFavCsv() = runFileTask("单词本已导出") { backup.exportFavCsv() }

    fun backupJson() = runFileTask("数据已备份") { backup.backupJson() }

    /**
     * 读取可用备份列表。
     * 只有一个备份时由调用方直接进入确认框，多个时由调用方弹选择框；
     * 一个都没有时统一在这里给出提示，避免每个调用点各写一遍文案。
     */
    fun loadBackups(onLoaded: (List<File>) -> Unit) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { backup.listBackups() } }
                .onSuccess { files ->
                    if (files.isEmpty()) _fileError.value = "备份目录中还没有任何备份文件"
                    else onLoaded(files)
                }
                .onFailure { _fileError.value = fileErrorMessage("没能读取备份目录", it) }
        }
    }

    /**
     * 从备份恢复。
     *
     * 结果通过 [notify]（全局 Snackbar）反馈，而不是由页面弹对话框：
     * 恢复可能要跑几秒，用户很可能已经离开设置页，页面级提示会永远不出现。
     * 全局提示挂在 AppRoot 上，无论用户在哪一页都能看到。
     */
    fun restoreBackup(file: File) {
        if (_fileBusy.value) return
        _fileBusy.value = true
        viewModelScope.launch {
            try {
                runCatching { withContext(Dispatchers.IO) { backup.restore(file) } }
                    .onSuccess { report ->
                        refreshAll()
                        notify(
                            buildString {
                                append("已从「${file.name}」恢复学习数据")
                                if (report.skipped > 0) append("，跳过 ${report.skipped} 条失效记录")
                            }
                        )
                    }
                    .onFailure { _fileError.value = fileErrorMessage("没能恢复数据", it) }
            } finally {
                _fileBusy.value = false
            }
        }
    }

    // ---------- 我的 / 成就 / 排行榜 ----------

    fun refreshProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                ProfileUiState(
                    loading = false,
                    overview = repo.overview(),
                    unlocked = repo.unlockedAchievements(),
                    weekStats = repo.userDb.recentDays(14),
                    goal = repo.dailyGoal,
                    autoSpeak = repo.autoSpeak,
                    speechRate = repo.speechRate,
                    stage = repo.stage,
                    iconStyle = repo.iconStyle,
                    themeMode = repo.themeMode,
                    animationsEnabled = repo.animationsEnabled,
                    leaderboard = repo.userDb.quizRecords(50),
                    aggregate = repo.userDb.quizAggregate()
                )
            }
            _profile.value = data
        }
    }

    private var leaderboardJob: Job? = null

    fun refreshLeaderboard() {
        // 与其它刷新器一样先取消上一个：并发跑时后完成的那个可能带着旧数据覆盖新结果
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                repo.userDb.quizRecords(50) to repo.userDb.quizAggregate()
            }
            _profile.update { it.copy(leaderboard = data.first, aggregate = data.second) }
        }
    }

    companion object {
        /** 搜索防抖时长：太短会在输入长词时频繁查库，太长会让结果显得迟钝 */
        private const val SEARCH_DEBOUNCE_MS = 250L
    }
}
