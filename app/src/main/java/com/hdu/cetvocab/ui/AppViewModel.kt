package com.hdu.cetvocab.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdu.cetvocab.CetVocabApp
import com.hdu.cetvocab.data.AchievementDef
import com.hdu.cetvocab.data.BackupManager
import com.hdu.cetvocab.data.DayStat
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.StudyOverview
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.data.quizLetterOfScope
import com.hdu.cetvocab.data.quizScopeOfLetter
import com.hdu.cetvocab.util.LauncherIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val allWords: List<Word> = emptyList(),
    val allWordsLoading: Boolean = false,
    val favWords: List<Word> = emptyList(),
    val wrongWords: List<Word> = emptyList()
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
    val typed: String = "",
    val revealed: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val finished: Boolean = false,
    val startTs: Long = 0L,
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
    val leaderboard: List<QuizRecord> = emptyList()
)

// ---------------- ViewModel ----------------

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: VocabRepository = (app as CetVocabApp).repo

    /** 数据导出与备份/恢复（文件写入应用外部私有目录，无需存储权限） */
    val backup: BackupManager by lazy { BackupManager(getApplication(), repo.userDb, repo.wordDb) }

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
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val overview = repo.overview()
                val due = repo.dueWords(40).size
                val queue = repo.todayQueue()
                // 「目前进度」只算当前级别：全局已学数会把另一个级别也算进来，不能直接用
                val stageIds = repo.wordDb.idsOfStage(repo.stage).toHashSet()
                val learnedInStage = repo.userDb.states()
                    .count { (id, st) -> st.level > 0 && id in stageIds }
                HomeUiState(
                    loading = false,
                    stage = repo.stage,
                    totalWords = repo.wordDb.countOfStage(repo.stage),
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

    fun resetAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.resetAll() }
            refreshAll()
            notify("学习数据已清空")
        }
    }

    fun refreshAll() {
        refreshHome()
        refreshLearn()
        refreshProfile()
    }

    // ---------- 学习 ----------

    fun refreshLearn() {
        viewModelScope.launch {
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
                LearnUiState(
                    loading = false,
                    stage = stage,
                    units = units,
                    letters = letters,
                    totalWords = repo.wordDb.countOfStage(stage),
                    learnedWords = allWords.count { (states[it.id]?.level ?: 0) > 0 },
                    favWords = repo.favWords(),
                    wrongWords = repo.wrongWords(40)
                )
            }
            _learn.value = data
        }
    }

    fun previewAllWords() {
        val stage = repo.stage
        if (_learn.value.stage == stage && _learn.value.allWords.isNotEmpty()) return
        _learn.update { it.copy(allWordsLoading = true, allWords = emptyList()) }
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) { repo.wordDb.wordsOfStage(stage) }
            _learn.update {
                it.copy(stage = stage, allWords = words, allWordsLoading = false)
            }
        }
    }

    fun search(keyword: String) {
        _learn.update { it.copy(query = keyword) }
        if (keyword.isBlank()) {
            _learn.update { it.copy(searchResult = emptyList()) }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repo.wordDb.search(keyword) }
            _learn.update { it.copy(searchResult = result) }
        }
    }

    // ---------- 背单词流程 ----------

    fun startTodayStudy() {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.todayQueue() }
            startQueue("今日学习", queue)
        }
    }

    fun startUnitStudy(unit: Int) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.unitWords(unit) }
            startQueue("第 $unit 单元", queue)
        }
    }

    fun startFavStudy() {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.favWords() }
            if (queue.isEmpty()) {
                notify("还没有收藏的生词")
                return@launch
            }
            startQueue("生词本复习", queue)
        }
    }

    fun startWrongStudy() {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { repo.wrongWords(50) }
            if (queue.isEmpty()) {
                notify("暂时没有错词记录")
                return@launch
            }
            startQueue("错词强化", queue)
        }
    }

    private suspend fun startQueue(title: String, queue: List<Word>) {
        if (queue.isEmpty()) {
            notify("这里暂时没有需要学习的单词")
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
    }

    fun flipCard() {
        _study.update { it.copy(flipped = !it.flipped) }
    }

    fun answerCurrent(known: Boolean) {
        val state = _study.value
        val word = state.current ?: return
        viewModelScope.launch {
            val newLevel = withContext(Dispatchers.IO) {
                val lv = repo.answerWord(word, known)
                repo.evaluateAchievements()
                lv
            }
            val nextIndex = state.index + 1
            if (nextIndex >= state.queue.size) {
                val gained = withContext(Dispatchers.IO) { repo.evaluateAchievements() }
                _study.update {
                    it.copy(
                        index = nextIndex,
                        flipped = false,
                        knownCount = it.knownCount + if (known) 1 else 0,
                        unknownCount = it.unknownCount + if (known) 0 else 1,
                        finished = true,
                        newAchievements = gained
                    )
                }
                refreshHome()
            } else {
                _study.update {
                    it.copy(
                        index = nextIndex,
                        flipped = false,
                        knownCount = it.knownCount + if (known) 1 else 0,
                        unknownCount = it.unknownCount + if (known) 0 else 1,
                        currentLevel = repo.userDb.stateOf(state.queue[nextIndex].id).level
                    )
                }
            }
            if (newLevel >= 3) { /* 等级提升，无需提示 */ }
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

    fun addStudySeconds(seconds: Long) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.addStudySeconds(seconds) } }
    }

    // ---------- 闯关 ----------

    fun startQuiz(unit: Int) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) {
                if (unit < 0) repo.buildStageQuiz(count = 30) else repo.buildQuiz(unit, 10)
            }
            if (questions.isEmpty()) {
                notify(if (unit < 0) "全词挑战暂不可用" else "该单元单词不足，换一个单元试试")
                return@launch
            }
            _quiz.value = QuizUiState(
                active = true,
                stage = challengeStage,
                unit = unit,
                questions = questions,
                startTs = System.currentTimeMillis()
            )
        }
    }

    /** 首字母挑战：从该首字母下的全部单词出题（字母不分大小写） */
    fun startLetterQuiz(letter: Char) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val questions = withContext(Dispatchers.IO) { repo.buildLetterQuiz(letter, 10) }
            if (questions.isEmpty()) {
                notify("「${letter.uppercaseChar()}」开头的单词不足，换一个字母试试")
                return@launch
            }
            _quiz.value = QuizUiState(
                active = true,
                stage = challengeStage,
                unit = quizScopeOfLetter(letter),
                questions = questions,
                startTs = System.currentTimeMillis()
            )
        }
    }

    fun selectOption(index: Int) {
        val state = _quiz.value
        if (state.revealed) return
        val question = state.current ?: return
        val correct = index == question.correctIndex
        applyAnswer(correct, state, selected = index)
    }

    fun submitSpelling() {
        val state = _quiz.value
        if (state.revealed) return
        val question = state.current ?: return
        val correct = state.typed.trim().equals(question.word.word.trim(), ignoreCase = true)
        applyAnswer(correct, state)
    }

    fun updateTyped(text: String) {
        _quiz.update { it.copy(typed = text) }
    }

    private fun applyAnswer(correct: Boolean, state: QuizUiState, selected: Int = state.selected) {
        val gain = if (correct) {
            val base = if (state.current?.spelling == true) 15 else 10
            base + minOf(state.combo, 5) * 2
        } else 0
        _quiz.update {
            it.copy(
                selected = selected,
                revealed = true,
                score = it.score + gain,
                correctCount = it.correctCount + if (correct) 1 else 0,
                combo = if (correct) it.combo + 1 else 0,
                maxCombo = if (correct) maxOf(it.maxCombo, it.combo + 1) else it.maxCombo
            )
        }
    }

    fun nextQuestion() {
        val state = _quiz.value
        val next = state.index + 1
        if (next >= state.questions.size) {
            finishQuiz()
        } else {
            _quiz.update { it.copy(index = next, selected = -1, typed = "", revealed = false) }
        }
    }

    private fun finishQuiz() {
        val state = _quiz.value
        val duration = System.currentTimeMillis() - state.startTs
        val accuracy = if (state.questions.isEmpty()) 0
        else state.correctCount * 100 / state.questions.size
        val stars = when {
            accuracy >= 90 -> 3
            accuracy >= 70 -> 2
            accuracy >= 50 -> 1
            else -> 0
        }
        viewModelScope.launch {
            val gained = withContext(Dispatchers.IO) {
                val record = repo.saveQuizRecord(
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
            _quiz.update {
                it.copy(
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
        _quiz.value = QuizUiState()
        refreshHome()
        refreshProfile()
    }

    // ---------- 我的 / 成就 / 排行榜 ----------

    fun refreshProfile() {
        viewModelScope.launch {
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
                    leaderboard = repo.userDb.quizRecords(50)
                )
            }
            _profile.value = data
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.userDb.quizRecords(50) }
            _profile.update { it.copy(leaderboard = list) }
        }
    }
}
