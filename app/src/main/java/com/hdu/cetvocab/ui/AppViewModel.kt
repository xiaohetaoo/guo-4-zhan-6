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
import com.hdu.cetvocab.data.QUIZ_SCOPE_WRONG
import com.hdu.cetvocab.data.QuizKind
import com.hdu.cetvocab.data.QuizSpec
import com.hdu.cetvocab.data.QuizAggregate
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.QUIZ_SPEC_CONFUSABLE
import com.hdu.cetvocab.data.QUIZ_SPEC_LETTER
import com.hdu.cetvocab.data.QUIZ_SPEC_STAGE
import com.hdu.cetvocab.data.QUIZ_SPEC_TRANSLATION
import com.hdu.cetvocab.data.QUIZ_SPEC_UNIT
import com.hdu.cetvocab.data.QUIZ_SPEC_WRONG
import com.hdu.cetvocab.data.STAGE_QUIZ_COUNT
import com.hdu.cetvocab.data.WRONG_REDO_LIMIT
import com.hdu.cetvocab.data.WRONG_STUDY_LIMIT
import com.hdu.cetvocab.data.StudyOverview
import com.hdu.cetvocab.data.percentOf
import com.hdu.cetvocab.data.UNIT_QUIZ_COUNT
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.data.quizLetterOfScope
import com.hdu.cetvocab.data.quizScopeOfLetter
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.util.LauncherIcons
import com.hdu.cetvocab.util.Sfx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

/** 选关页用来挂「继续上一局」提示条的信息 */
data class RestorableQuiz(
    val spec: QuizSpec,
    val index: Int,
    val score: Int,
    val correct: Int,
    val combo: Int,
    val maxCombo: Int,
    val elapsedMs: Long,
    /** 快照还能用多久（分钟），用来把提示写得具体 */
    val remaining: Int
)

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
     * 本局是不是「错题重做」。决定三件事：结算页的文案、要不要写成绩、要不要评成就。
     * 由 [openQuiz] 从 spec.kind 推导，不单独传参，避免两处口径打架。
     */
    val isRedo: Boolean = false,
    /** 重做结算后错题集还剩几道；-1 表示本局不是重做，结算页不显示这一行 */
    val remainingWrong: Int = -1,
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
    /**
     * 桌面此刻**实际**在用的图标风格；读不出来时为 null。
     *
     * 与 [iconStyle]（用户的选择）分开存：图标要到 onStop 才落地，
     * 而且个别设备的桌面根本不允许应用换图标。设置页原来按 [iconStyle]
     * 写「当前桌面显示」，切换失败时那句话就是假的。
     */
    val desktopIconStyle: Int? = null,
    val themeMode: Int = VocabRepository.THEME_SYSTEM,
    /** 界面动效开关（设置页）。false 时转场、弹簧与按压回弹全部退化为瞬间落位。 */
    val animationsEnabled: Boolean = true,
    /** 闯关答题音效开关（设置页） */
    val soundEnabled: Boolean = true,
    val leaderboard: List<QuizRecord> = emptyList(),
    /**
     * 当前级别待重做的闯关错题数，排行榜顶部的汇总卡用它。
     * 与「错词本」是两个口径：错词本来自学习卡片的答错计数，这里只收闯关答错的题。
     */
    val wrongRedoCount: Int = 0,
    /**
     * 排行榜顶部统计的聚合口径（次数 / 最高分 / 平均分 / 最高正确率）。
     * 由 SQL 在**全表**上算，不受 [leaderboard] 最多 50 条的限制。
     */
    val aggregate: QuizAggregate = QuizAggregate()
)

// ---------------- ViewModel ----------------

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: VocabRepository = (app as CetVocabApp).repo

    /**
     * 闯关答题提示音。
     *
     * 挂在 ViewModel 而不是 Composable 里：一次作答的判定发生在数据层回调里，
     * 若交给页面，切后台、重组、退出重进都可能把声音漏掉或重放。
     * 初值直接读偏好，避免设置里关了音效、首帧却又响一声。
     */
    private val sfx = Sfx(app).apply { enabled = repo.soundEnabled }

    /** 数据导出与备份/恢复（文件写入应用外部私有目录，无需存储权限） */
    val backup: BackupManager by lazy { BackupManager(getApplication(), repo.userDb, repo.wordDb) }

    init {
        // 动效开关要在首帧之前就同步给动效系统：Motion 的默认值是「开」，
        // 若等 refreshAll() 落地，上次关闭了动效的用户在重启后会看到几帧动画。
        Motion.applyAnimationsEnabled(repo.animationsEnabled)
    }

    override fun onCleared() {
        // Sfx 里有一条单线程 Executor（线程设了 daemon，不会拦住进程退出），
        // 但播放中的 AudioTrack 要靠它的队列任务释放。
        // ViewModel 销毁时不 shutdown 的话，待释放的尾音任务就再也没机会跑，
        // 音轨悬在系统里 —— 正是 Sfx 注释里点名要避免的那类泄漏。
        sfx.shutdown()
        super.onCleared()
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
     * 尚未落库的逐题作答写入（错题集 / 错词计数）。
     *
     * 结算页的「待重做错题还剩 N 道」直接数 quiz_wrong 表，而每答一题的入库
     * 是另一条发出去就不管的协程。末题答错后立刻结算，读表常常赶在写入之前，
     * 于是明明刚错了一道、结算页却说「错题集已经全部清完」—— 少报的正是最后那一两道。
     * 这里把在飞的写入登记下来，结算前 join 完再去数。
     * 只在主线程增删（作答与结算都由点击驱动），不需要并发容器。
     */
    private val answerWrites = ArrayList<Job>()

    /**
     * 作答处理中标志。
     * 「认识 / 不认识」按钮连点两下时，两个协程会读到同一份快照、算出同一个 nextIndex，
     * 结果是同一道题被记两次而索引只 +1（跳过下一个单词）。用它把并发收成串行。
     */
    private var answering = false

    /** 闯关结算中标志：保证一次闯关只写入一条成绩记录 */
    private var finishing = false

    /**
     * 闯关会话序号。openQuiz 每局自增，finishQuiz 在写库前捕获它，
     * 回写结算态时比对 —— 只有仍是同一局才写入。
     *
     * 没有这个序号时会出现「幽灵结算页」：saveQuizRecord 是不可取消的阻塞 IO，
     * 玩家打完末题后立刻退出并开新一局，上一局的回写在新局状态上仍然满足
     * `active && !finished`，于是把上一局的 stars / 得分 / finished
     * 盖到刚开的新局上，玩家看到的是新一局直接弹出上一局的结算页。
     */
    private var quizSession = 0L

    /** 当前局的出题配方，用于进程被回收后重放 */
    private var quizSpec: QuizSpec? = null

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

    /** 供非 ViewModel 持有者（如 AppRoot 里的发音回调）触发一次提示 */
    fun toast(msg: String) = notify(msg)

    /**
     * 词库损坏的提示只说一次。
     *
     * 这个状态不会因为「用户又刷了一次首页」而自愈，每次刷新都弹就变成骚扰；
     * 而且刷新入口有四个（冷启动、切级别、答完一题、返回前台），
     * 不做去重的话用户可能在几秒内看到四条一模一样的提示。
     */
    private var wordbookReported = false

    private fun reportWordbook() {
        if (wordbookReported) return
        val problem = repo.wordbookProblem ?: return
        wordbookReported = true
        notify(problem)
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
            reportWordbook()
        }
    }

    fun setStage(stage: Int) {
        repo.stage = stage
        // 学习进度按 word_id 独立存放，两个级别互不影响，切换时不需要重置任何进度。
        // 这里把新级别同时同步进首页与「我的」状态，让按钮选中态立即响应，
        // 词数 / 进度等具体数据随后由 refreshAll() 异步补齐（本地库，通常几十毫秒）。
        // 「我的」这一份不能省：refreshProfile 的合并逻辑刻意保留 old.stage
        // （见其注释），页面自己不会从 repo 重新取值，只改 _home 的话
        // 「当前阶段」会一直停在进应用时的那一刻，与首页/词库不一致。
        _home.update { it.copy(stage = stage) }
        _profile.update { it.copy(stage = stage) }
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
        val want = LauncherIcons.targetAlias(style, repo.stage)
        /*
         * 先探一次目标入口在这台设备上存不存在，再决定承诺什么。
         *
         * 图标不是在点击时写系统、而是在 onStop 落地（见 MainActivity），
         * 所以这里没法当场知道成败。但有一类失败是**点击时就能确定的**：
         * ROM 裁掉了 activity-alias，或用户在系统设置里禁用了某个入口 ——
         * 这种设备上写入必定失败，而原来的文案照样说「回到桌面即可看到」，
         * 用户回到桌面发现图标没变，只会以为功能坏了。
         * 探测不到就不改偏好、不承诺，直接说清原因。
         */
        if (!LauncherIcons.isAvailable(getApplication(), want)) {
            notify("这台设备的桌面不允许本应用切换图标，已保留原来的图标")
            return
        }
        repo.iconStyle = style
        _profile.update { it.copy(iconStyle = style) }
        notify(
            if (style == LauncherIcons.STYLE_SIMPLE) "已选择简洁图标，回到桌面即可看到"
            else "已切回默认图标"
        )
        // 选完立刻重读一次系统状态：设置页那句「当前桌面显示」讲的是实际图标，
        // 不是用户刚点的那一项（见 ProfileUiState.desktopIconStyle）。
        refreshProfile()
    }

    /**
     * onStop 落地图标时写入失败的待播报提醒。
     *
     * 失败发生在应用已经退到后台之后，此刻弹 Snackbar 用户看不见，
     * 等下一次回到前台再补这一句（[onAppForeground]）。
     */
    private var pendingIconNotice: String? = null

    fun onIconApplyFailed() {
        pendingIconNotice = "桌面图标没能切换，已保留原来的图标"
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
        // 与 setStage 同理：refreshProfile 刻意保留 old.goal，
        // 不先写 _profile 的话，设置页滑杆改完目标、「我的」页仍显示旧值。
        _profile.update { it.copy(goal = goal) }
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

    /**
     * 答题音效开关。写偏好 + 立即改 [sfx] 的标志 + 回写 UI 三件事一起做：
     * 只写偏好的话，用户关掉开关后本局仍会继续响，听起来就是开关没生效。
     */
    fun setSoundEnabled(enabled: Boolean) {
        repo.soundEnabled = enabled
        sfx.enabled = enabled
        _profile.update { it.copy(soundEnabled = enabled) }
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
            // prefsFromRepo=true：resetAll 里 prefs.edit().clear() 把设置全恢复成默认，
            // _profile 里缓存的旧设置必须整体作废，否则各开关显示的还是清空前的状态。
            refreshAll(prefsFromRepo = true)
            notify("学习数据已清空")
        }
    }

    /**
     * 全量刷新。
     *
     * [prefsFromRepo] 用于「外部把偏好整体换掉了」的两个场景：清空数据（prefs.clear()）
     * 和从备份恢复（写回备份里的级别 / 目标 / 语速 / 各开关）。
     * 这两种情况下 [_profile] 里缓存的设置已经不属于本机现状，
     * 必须整体采用刚从 repo 读出来的那一份；默认 false 走合并逻辑，
     * 保住用户在读表期间刚刚拨动的开关（见 [refreshProfile] 的注释）。
     */
    fun refreshAll(prefsFromRepo: Boolean = false) {
        // 备份恢复 / 清空数据会连带改掉动效与音效开关，这里统一再同步一次运行时状态：
        // Motion 是全局对象、sfx.enabled 是播放线程读的 volatile 标志，
        // 只改偏好不推给它们的话，用户要重开应用才看得到效果。
        Motion.applyAnimationsEnabled(repo.animationsEnabled)
        sfx.enabled = repo.soundEnabled
        refreshHome()
        refreshLearn()
        refreshProfile(prefsFromRepo)
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
                    wrongWords = repo.wrongWords(WRONG_STUDY_LIMIT)
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
                    allWordsLoading = if (sameStage) old.allWordsLoading else false,
                    // 截断标记必须跟着搜索结果一起继承。漏掉它时，收藏一个词、
                    // 打完一局这类随手触发的 refreshLearn 会把「按字母序只显示前 60 个」
                    // 悄悄改回「匹配 60 个单词」，而列表里的 60 条一条没变 —— 用户会以为
                    // 整个级别真的只有 60 个匹配。
                    searchTruncated = if (sameStage) old.searchTruncated else false
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
            startQueue("第${unit}单元", queue, onReady)
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
            val queue = withContext(Dispatchers.IO) { repo.wrongWords(WRONG_STUDY_LIMIT) }
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
        // 两次查询都要在 IO 线程做：viewModelScope 默认是 Main.immediate，
        // 上面取 favIds 已经包了 withContext(IO)，这里 `stateOf` 曾是漏网的主线程读库
        // ——冷启动首次进学习页正好撞上建库/拷 7.98 MB 词库的窗口，主线程会卡顿一下。
        val startWordId = queue.first().id
        val (favIds, startLevel) = withContext(Dispatchers.IO) {
            repo.userDb.states().values.filter { it.fav }.map { it.wordId }.toSet() to
                repo.userDb.stateOf(startWordId).level
        }
        _study.value = StudyUiState(
            title = title,
            queue = queue,
            index = 0,
            favIds = favIds,
            currentLevel = startLevel
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

    /**
     * 收藏操作的串行锁。
     *
     * `UserDb.toggleFav` 自己在事务里读改写，库里不会错；问题出在 UI 这一侧：
     * 每个协程拿到「自己那次翻转的结果」再写进 `favIds` 集合，连点两下时
     * 两个协程的 IO 完成顺序不确定，后发起的那次可能先写回集合，
     * 于是界面停在第一次的结果上（图标显示已收藏、库里其实是未收藏）。
     * 把「写库 + 写回 UI」整体串行，两次连点就变成有序的翻两次。
     */
    private val favMutex = Mutex()

    fun toggleFavCurrent() {
        val word = _study.value.current ?: return
        viewModelScope.launch {
            favMutex.withLock {
                val fav = withContext(Dispatchers.IO) { repo.toggleFav(word) }
                _study.update {
                    val set = it.favIds.toMutableSet()
                    if (fav) set.add(word.id) else set.remove(word.id)
                    it.copy(favIds = set)
                }
            }
            refreshLearn()
        }
    }

    fun toggleFav(word: Word) {
        viewModelScope.launch {
            favMutex.withLock { withContext(Dispatchers.IO) { repo.toggleFav(word) } }
            refreshLearn()
            refreshProfile()
        }
    }

    fun finishStudy() {
        val state = _study.value
        val gained = state.newAchievements
        /*
         * 这里刻意**不**把 _study 重置成空态。
         *
         * 旧实现先清态、再由调用方 popBackStack()：pop 之后学习页还要播完约 250ms
         * 的退场动画，而它在这整个动画期间读的就是刚被清空的状态，于是每次点「完成」
         * 都能看到一个结算页滑出、换成「本组暂无单词」空态的画面。
         * 清态交给下一次 startStudy（它本来就会整体赋值新的 StudyUiState），
         * 退出动画期间保留结算页，才是用户期望看到的东西。
         */
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
        // 补播后台期间发生、当时说不出口的失败（见 [onIconApplyFailed]）
        pendingIconNotice?.let {
            pendingIconNotice = null
            notify(it)
            refreshProfile()
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
        spec: QuizSpec,
        onReady: (Boolean) -> Unit = {}
    ) {
        finishJob?.cancel()
        finishJob = null
        finishing = false
        // 开新局前先把「答到一半的那一局」挪个地方存着，别让它无声消失
        preserveInterruptedSession()
        // 每开一局换一个新的会话号：上一局尚未写完的结算结果据此作废
        quizSession++
        _quiz.value = QuizUiState(
            active = true,
            stage = challengeStage,
            unit = unit,
            questions = questions,
            isRedo = spec.kind == QUIZ_SPEC_WRONG
        )
        questionStartTs = System.currentTimeMillis()
        quizSpec = spec
        saveQuizSnapshot()
        onReady(true)
    }

    /**
     * 开新局前保留被打断的上一局。
     *
     * 只有一个快照键，新局一写就把旧局盖掉：用户答了 8 题、退出后选了另一关，
     * 那 8 题既没有成绩也没有快照，等于凭空蒸发 —— 界面上一个字都没提。
     * 这里在覆盖前把它转存到 [VocabRepository.quizInterruptedSnapshot]，
     * 并在本局结束/退出后由 [clearQuizSnapshot] 取回，让用户仍然有机会打完它。
     *
     * 两种「被打断」都要保：
     * ① 内存里正在进行、且至少答过一题的那一局（从结算页「再来一局」之外的入口开新局）；
     * ② 进程被系统回收后留下的那份快照 —— 它不在内存态里（[QuizUiState.active] 是 false），
     *    却是选关页「有一局没打完」卡片指向的那一局。只挡 ① 的话，
     *    最常见的「杀进程 → 重开 → 直接点另一关」仍然会把旧局盖掉。
     *
     * 不保留的情况：一题没答（index==0，丢了不亏，留着只会让用户对着「第1题继续」发懵）、
     * 已超过 30 分钟可恢复窗口（peekRestorableQuiz 本来就要丢弃它）、
     * 已进结算页（finished，成绩已经落库）。
     */
    private fun preserveInterruptedSession() {
        val s = _quiz.value
        val live = s.active && !s.finished && s.index > 0
        if (!live && !snapshotHasProgress()) return
        // 内存态那一题刚落盘时 saveQuizSnapshot 会把最新进度写进快照；
        // 只有①需要这一步，②的快照本来就已经在 prefs 里。
        if (live) saveQuizSnapshot()
        repo.quizInterruptedSnapshot = repo.quizSessionSnapshot
        _quizSnapshotEpoch.update { it + 1 }
        notify(
            if (live) "上一局第${s.index}题前的进度已保留，本局结束后可继续"
            else "之前没打完的那一局已保留，本局结束后可继续"
        )
    }

    /** prefs 里那份快照是不是「答过题、且还在可恢复窗口内」的旧局（见 [preserveInterruptedSession] ②） */
    private fun snapshotHasProgress(): Boolean {
        val raw = repo.quizSessionSnapshot ?: return false
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (obj.optInt("index", 0) <= 0) return false
        val ts = obj.optLong("ts", 0L)
        return ts > 0L && System.currentTimeMillis() - ts <= RESTORE_WINDOW_MS
    }

    /**
     * 把「当前这一局」记进 prefs。
     *
     * 只存配方和进度，不存题目：题目由 seed 重放（词库只读、取词全部带
     * ORDER BY，同 seed 必得同题序），所以一条几百字节的 JSON 就够。
     */
    private fun saveQuizSnapshot() {
        val spec = quizSpec ?: return
        val s = _quiz.value
        if (!s.active || s.finished) return
        val json = JSONObject().apply {
            put("kind", spec.kind)
            put("stage", spec.stage)
            put("unit", spec.unit)
            put("count", spec.count)
            put("seed", spec.seed)
            // 只有错题重做局需要把题目清单一起存：它的题序依赖错题集当时的内容，
            // 而错题集会在重做过程中缩小（见 QuizSpec.refs）。其余关卡留空不写，
            // 免得每条快照都白白多背两三百字节。
            if (spec.refs.isNotEmpty()) put("refs", spec.refs)
            put("index", s.index)
            put("score", s.score)
            put("correct", s.correctCount)
            put("combo", s.combo)
            put("maxCombo", s.maxCombo)
            put("elapsed", s.elapsedMs)
            put("ts", System.currentTimeMillis())
        }
        // 只在「从无到有」时推进版本号：saveQuizSnapshot 每题都会调用，
        // 每写一次就自增会让选关页每答一题重新解析一遍快照 JSON，毫无收益。
        val hadSnapshot = repo.quizSessionSnapshot != null
        repo.quizSessionSnapshot = json.toString()
        if (!hadSnapshot) _quizSnapshotEpoch.update { it + 1 }
    }

    private fun clearQuizSnapshot() {
        quizSpec = null
        /*
         * 本局结束后，把「被它挤掉的那一局」放回快照位。
         * 放回来的时机只有这一处：结算页关闭与答题页退出都会走到这里，
         * 而用户此刻已经不再需要本局的快照，选关页那张「有一局没打完」的卡片
         * 正好换成更早的那一局。超过 30 分钟窗口则由 peekRestorableQuiz 自己丢弃。
         */
        repo.quizSessionSnapshot = repo.quizInterruptedSnapshot
        repo.quizInterruptedSnapshot = null
        _quizSnapshotEpoch.update { it + 1 }
    }

    /**
     * 闯关快照的版本号，每次写入或清除快照时自增。
     *
     * 选关页的「有一局没打完」卡片用它当重算依据之一。卡片原本只按
     * `state.active / state.finished` 两个键去 peek，而「恢复失败」这条路径
     * 既不进入关卡也不结束关卡 —— 两个键都没变，卡片就留在页上变成死按钮
     * （点「继续」只会在原地再提示一次无法还原）。快照本身才是这张卡片的依据，
     * 所以让它来触发重算。
     */
    private val _quizSnapshotEpoch = MutableStateFlow(0L)
    val quizSnapshotEpoch: StateFlow<Long> = _quizSnapshotEpoch.asStateFlow()

    /**
     * 读取并校验可恢复的上一局（不改变任何状态，供选关页决定要不要挂提示条）。
     *
     * 刻意做成「用户点一下才继续」而不是自动重放：快照在正常杀进程后同样存在，
     * 自动恢复会让用户点「闯关」标签时莫名其妙被塞进一局旧题，
     * 以为应用记住了他不想要的东西。
     */
    fun peekRestorableQuiz(): RestorableQuiz? {
        if (_quiz.value.active) return null
        val raw = repo.quizSessionSnapshot ?: return null
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: run {
            clearQuizSnapshot()
            return null
        }
        val ts = obj.optLong("ts", 0L)
        // 超过 30 分钟就当作「用户已经不记得这一局了」，直接丢弃快照
        if (ts <= 0L || System.currentTimeMillis() - ts > RESTORE_WINDOW_MS) {
            clearQuizSnapshot()
            return null
        }
        val spec = QuizSpec(
            kind = obj.optInt("kind", -1),
            stage = obj.optInt("stage", -1),
            unit = obj.optInt("unit", Int.MIN_VALUE),
            count = obj.optInt("count", 0),
            seed = obj.optLong("seed", 0L),
            refs = obj.optString("refs", "")
        )
        if (spec.kind !in 0..QUIZ_SPEC_WRONG || spec.count <= 0 ||
            spec.unit == Int.MIN_VALUE || spec.stage != repo.stage
        ) {
            // 级别对不上时不恢复：把四级的题算进六级的进度比丢掉一局更糟
            clearQuizSnapshot()
            return null
        }
        // 重做局的题目完全来自 refs，缺了它就没法原样重放，只能放弃恢复
        if (spec.kind == QUIZ_SPEC_WRONG && spec.refs.isEmpty()) {
            clearQuizSnapshot()
            return null
        }
        return RestorableQuiz(
            spec = spec,
            index = obj.optInt("index", 0),
            score = obj.optInt("score", 0),
            correct = obj.optInt("correct", 0),
            combo = obj.optInt("combo", 0),
            maxCombo = obj.optInt("maxCombo", 0),
            elapsedMs = obj.optLong("elapsed", 0L),
            // 剩余分钟数向上取整（不足一分钟也算「1 分钟内可继续」），
            // 但必须夹在 1..30：早先写成「整除后 +1」，快照刚写入那一刻
            // 30 分钟整除得 30、再 +1 就报「约31分钟内可继续」，比窗口本身还长。
            remaining = ((RESTORE_WINDOW_MS - (System.currentTimeMillis() - ts) + 59_999L) / 60_000L)
                .toInt().coerceIn(1, (RESTORE_WINDOW_MS / 60_000L).toInt())
        )
    }

    /**
     * 按配方重放上一局。
     *
     * 词库是随包的只读 SQLite，取词一律带 ORDER BY（words 按 "rank"、
     * 翻译题库按 id），rank 在本级内唯一，因此同一个 seed 走同一条 shuffle 链
     * 必然得到同一份题序 —— 不需要把题目本身存下来，一条配方 JSON 就够。
     * 重放结果与快照题数对不上时（词库或出题算法变过）放弃恢复，绝不凑合。
     */
    fun resumeQuiz(from: RestorableQuiz) {
        if (_quiz.value.active) return
        viewModelScope.launch {
            val questions = withContext(Dispatchers.IO) { repo.rebuildQuiz(from.spec) }
            if (questions.size != from.spec.count) {
                /*
                 * 恢复失败时必须把选关页那张「有一局没打完」的卡片一起收掉。
                 * clearQuizSnapshot() 只清了持久化快照，[QuizUiState] 一个字节都没动，
                 * 而卡片是页面按 `state.active/state.finished` 两个键重算的
                 * —— 失败路径上这俩仍是 (false, false)，键值没变，
                 * remember 不重算，于是「继续」按钮还挂在页上，点下去只会再提示一次无法还原。
                 * 推进会话号让页面拿到一次新的重算依据，卡片随快照一起消失。
                 */
                clearQuizSnapshot()
                notify("上一局的题目已无法还原，请重新选关")
                return@launch
            }
            val index = from.index.coerceIn(0, questions.size - 1)
            /*
             * 恢复路径必须把开局路径上的记账一件不落补齐，否则「恢复出来的那一局」
             * 和「新开的这一局」行为不一致。这里补两项：
             * 1. isRedo —— 漏掉它，错题重做被进程回收再恢复后就变成普通闯关，
             *    打完会把成绩写进排行榜并触发成就，正是这个功能刻意要避免的事
             *    （同一批题可以无限重做，刷出满分毫无意义）。
             * 2. finishJob / finishing / quizSession —— 上一局尚未写完的结算协程
             *    若还挂在路上，`finishing` 会把这一局的所有作答全挡掉；
             *    会话号不推进，那次迟到的写入还会盖到刚恢复出来的这一局上。
             */
            finishJob?.cancel()
            finishJob = null
            finishing = false
            quizSession++
            _quiz.value = QuizUiState(
                active = true,
                stage = from.spec.stage,
                unit = from.spec.unit,
                questions = questions,
                index = index,
                score = from.score,
                correctCount = from.correct,
                combo = from.combo,
                maxCombo = from.maxCombo,
                elapsedMs = from.elapsedMs,
                isRedo = from.spec.kind == QUIZ_SPEC_WRONG
            )
            quizSpec = from.spec
            questionStartTs = System.currentTimeMillis()
            saveQuizSnapshot()
            notify("已恢复上次未完成的闯关，从第${index + 1}题继续")
        }
    }

    fun startQuiz(unit: Int) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val seed = System.nanoTime()
            val count = if (unit < 0) STAGE_QUIZ_COUNT else UNIT_QUIZ_COUNT
            val questions = withContext(Dispatchers.IO) {
                if (unit < 0) repo.buildStageQuiz(count = count, seed = seed)
                else repo.buildQuiz(unit, count, seed = seed)
            }
            if (questions.isEmpty()) {
                notify(if (unit < 0) "全词挑战暂不可用" else "该单元单词不足，换一个单元试试")
                return@launch
            }
            openQuiz(
                challengeStage, unit, questions,
                // count 存**实际出题数**而不是请求数：小单元的题量本来就少于上限，
                // 存 10 会让 resumeQuiz 的 `questions.size != spec.count` 恒不成立，
                // 这一局永远恢复不了（首字母挑战的 z/y 等小词量字母同理）。
                QuizSpec(
                    if (unit < 0) QUIZ_SPEC_STAGE else QUIZ_SPEC_UNIT,
                    challengeStage, unit, questions.size, seed
                )
            )
        }
    }

    /**
     * 开一局错题重做（入口在排行榜）。
     *
     * 可以无限次开：本局不写 quiz_record、不评成就，所以刷多少遍都动不了排行榜，
     * 唯一的副作用是答对就把题移出错题集。
     * 题目清单在开局瞬间冻结进 spec.refs，进程被杀后能原样重放（见 [saveQuizSnapshot]）。
     */
    fun startWrongRedo(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val seed = System.nanoTime()
            val outcome = withContext(Dispatchers.IO) {
                val refs = repo.wrongRedoRefs()
                refs to repo.buildWrongRedoQuiz(refs, seed)
            }
            val (refs, questions) = outcome
            if (questions.isEmpty()) {
                notify("没有需要重做的错题")
                onReady(false)
                return@launch
            }
            openQuiz(
                challengeStage, QUIZ_SCOPE_WRONG, questions,
                QuizSpec(QUIZ_SPEC_WRONG, challengeStage, QUIZ_SCOPE_WRONG, questions.size, seed, refs),
                onReady
            )
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
            val seed = System.nanoTime()
            val questions = withContext(Dispatchers.IO) { repo.buildConfusableQuiz(UNIT_QUIZ_COUNT, seed) }
            if (questions.isEmpty()) {
                notify("易混词辨析暂不可用")
                onReady(false)
                return@launch
            }
            openQuiz(
                challengeStage, QUIZ_SCOPE_CONFUSABLE, questions,
                QuizSpec(QUIZ_SPEC_CONFUSABLE, challengeStage, QUIZ_SCOPE_CONFUSABLE, questions.size, seed),
                onReady
            )
        }
    }

    /**
     * 真题句子翻译专题：中文句 + 四个译文，选出全部可接受译法（多选）。
     * 题库按级别内置在词库里，六级与四级各自成卷。
     */
    fun startTranslationQuiz(onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val challengeStage = repo.stage
            val seed = System.nanoTime()
            val questions = withContext(Dispatchers.IO) { repo.buildTranslationQuiz(UNIT_QUIZ_COUNT, seed) }
            if (questions.isEmpty()) {
                notify("真题句子翻译题库暂不可用")
                onReady(false)
                return@launch
            }
            openQuiz(
                challengeStage, QUIZ_SCOPE_TRANSLATION, questions,
                QuizSpec(QUIZ_SPEC_TRANSLATION, challengeStage, QUIZ_SCOPE_TRANSLATION, questions.size, seed),
                onReady
            )
        }
    }

    /** 首字母挑战：从该首字母下的全部单词出题（字母不分大小写） */
    fun startLetterQuiz(letter: Char) {
        // 非 A–Z 的字符算不出合法的范围值，直接拒绝而不是让它和单元号碰撞
        val scope = quizScopeOfLetter(letter) ?: run {
            notify("「${letter}」不是有效的首字母")
            return
        }
        viewModelScope.launch {
            val challengeStage = repo.stage
            val seed = System.nanoTime()
            val questions = withContext(Dispatchers.IO) { repo.buildLetterQuiz(letter, UNIT_QUIZ_COUNT, seed = seed) }
            if (questions.isEmpty()) {
                notify("「${letter.uppercaseChar()}」开头的单词不足，换一个字母试试")
                return@launch
            }
            openQuiz(
                challengeStage, scope, questions,
                QuizSpec(QUIZ_SPEC_LETTER, challengeStage, scope, questions.size, seed)
            )
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
        var applied = false
        val question = _quiz.value.current
        val challengeStage = _quiz.value.stage
        _quiz.update { s ->
            if (s.revealed) return@update s
            /*
             * 最后一题的「揭晓 → 入库」窗口内也必须挡住作答。
             * nextQuestion 为了抢占这次推进，会把 revealed 置回 false，
             * 而 finished 要等 saveQuizRecord 的 IO 跑完才置位；这段窗口里
             * index 仍停在末题、active 仍为 true，末题因此变成「可以再答一次」：
             * 连点选项会二次加分、二次加连击，结算页显示的分数与入库记录不一致。
             */
            if (finishing) return@update s
            val gain = if (correct) {
                val base = baseScoreOf(s.current)
                base + minOf(s.combo, 5) * 2
            } else 0
            applied = true
            s.copy(
                selected = selected,
                revealed = true,
                score = s.score + gain,
                correctCount = s.correctCount + if (correct) 1 else 0,
                combo = if (correct) s.combo + 1 else 0,
                maxCombo = if (correct) maxOf(s.maxCombo, s.combo + 1) else s.maxCombo
            )
        }
        // 错题集与音效只在「这次作答真的把题揭晓了」之后做一次。
        // 放在 update 里不行：update 的 lambda 在并发更新下会重试，副作用会被执行多次；
        // 放在 update 外又不加判断的话，连点两下会记两次错、响两声。
        // 三个调用点都在主线程，applied 只会被真正提交的那一次置真。
        if (!applied || question == null) return
        sfx.play(correct)
        // 先清掉已经跑完的，再登记本次的。两个动作都在主线程（applyAnswer 由点击触发），
        // 所以这个集合不需要加锁；不要在 invokeOnCompletion 里删 —— 那个回调线程不确定。
        answerWrites.removeAll { it.isCompleted }
        answerWrites += viewModelScope.launch(Dispatchers.IO) {
            repo.onQuizAnswered(challengeStage, question.word, question.kind, correct)
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
            // 快照必须在 index 推进**之后**再落，否则记下的永远是「上一题」：
            // 实测答完 3 题、界面停在第 4 题时杀进程，恢复条却写「答到第 3 题」，
            // 点继续回到第 3 题重答一遍 —— 比不恢复更容易让人以为成绩丢了。
            saveQuizSnapshot()
        }
    }

    private fun finishQuiz() {
        // 幂等：nextQuestion 判定「已是最后一题」时会调到这里，而 finished 标志要等
        // 数据库写完才置位，中间这段窗口内再次触发就会写入两条闯关记录。
        if (finishing) return
        finishing = true

        val state = _quiz.value
        val session = quizSession
        val duration = state.elapsedMs
        val accuracy = percentOf(state.correctCount, state.questions.size)
        // 星级必须与结算页显示的那个正确率同口径。
        // 早期这里写的是 `correctCount * 100 / questions.size`（整数截断），
        // 而 QuizScreen 显示用 percentOf（四舍五入）：49 题对 44 题，
        // 页面写「正确率90%」、星数却按 89% 只给两颗星，用户会当成判分出错。
        val stars = when {
            accuracy >= 90 -> 3
            accuracy >= 70 -> 2
            accuracy >= 50 -> 1
            else -> 0
        }
        finishJob = viewModelScope.launch {
            // 等所有逐题写入落库，再去数错题集。见 [answerWrites]。
            // 放在这里而不是 applyAnswer 里串行：逐题入库本来就该是异步的，
            // 让它挡住下一题的点击会让答题手感变粘。
            answerWrites.toList().joinAll()
            val isRedo = state.isRedo
            val gained = withContext(Dispatchers.IO) {
                if (isRedo) {
                    /*
                     * 重做局既不写成绩也不评成就。
                     * 错题可以无限次重做，一旦入库，把同一批题刷到全对就能在排行榜上
                     * 挂出一个从未真实达到过的满分，「历次成绩」这个口径就被自己毁掉了；
                     * 成就里还有「单次闯关连对 50 题」这类按局计数的档位，同样会被刷出来。
                     */
                    emptyList<AchievementDef>() to null
                } else {
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
            }
            // 重做结算要报「错题集还剩几道」，必须等答对的题移出之后再去数
            val remaining = if (isRedo) {
                withContext(Dispatchers.IO) { repo.quizWrongCount(state.stage) }
            } else {
                -1
            }
            // 只在本次闯关仍然有效时写入结算态：用户若已点退出，
            // 状态会被重置为 QuizUiState()，此时再置 finished 就会弹出「幽灵结算页」。
            // 会话号比对补上另一半：退出后**又开了一局**时 active/finished 都是新局的值，
            // 单看标志位拦不住，必须确认回写的仍是开局那一局。
            if (session != quizSession) return@launch
            _quiz.update { s ->
                if (!s.active || s.finished) s
                else s.copy(
                    finished = true,
                    stars = stars,
                    lastRecord = gained.second,
                    newAchievements = gained.first,
                    remainingWrong = remaining
                )
            }
            clearQuizSnapshot()
            refreshHome()
            refreshProfile()
        }
    }

    fun exitQuiz() {
        finishJob?.cancel()
        finishing = false
        questionStartTs = 0L
        clearQuizSnapshot()
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
     * 规则：**按异常类型**识别自家文案，而不是按字符集猜。
     * BackupManager 的校验用 `require { }`，抛的是 IllegalArgumentException，
     * 那些本来就是完整的中文句子，原样展示；其余异常包一句中文行动建议，
     * 技术细节降级成第二行，方便用户复制给开发者排查。
     *
     * 早先用「message 里含中文」判断，被中文目录名骗过：导出/备份目录就叫
     * 「导出」「备份」，写盘失败时 FileNotFoundException 的 message 带完整路径，
     * 一定含中文 → 英文报错被当成友好文案原样弹出来，行动建议和技术信息全丢。
     */
    private fun fileErrorMessage(action: String, error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        return if (error is IllegalArgumentException && raw.isNotEmpty()) raw
        else "${action}，请检查本机存储空间是否充足、文件是否可读，然后重试。" +
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
     *
     * [allowWordbookMismatch] 只由「词库版本不一致」确认框点「仍要恢复」时传 true。
     */
    fun restoreBackup(file: File, allowWordbookMismatch: Boolean = false) {
        if (_fileBusy.value) return
        _fileBusy.value = true
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) { backup.restore(file, allowWordbookMismatch) }
                }
                    .onSuccess { report ->
                        // 备份里带着级别 / 目标 / 语速 / 各开关，恢复成功后 _profile
                        // 必须整体换成备份那份，不能让合并逻辑把 old 的当前设置留下。
                        refreshAll(prefsFromRepo = true)
                        notify(
                            buildString {
                                append("已从「${file.name}」恢复学习数据")
                                if (report.skipped > 0) append("，跳过${report.skipped}条失效记录")
                            }
                        )
                    }
                    .onFailure {
                        /*
                         * 「备份来自另一版词库」不是错误，是一次需要用户拍板的风险提示：
                         * 旧备份确实可能只有这一份，直接拒掉等于把用户的进度锁死在外面。
                         * 所以它不走 [_fileError]（那是一条红字，用户只会照着改备份），
                         * 而是放进 [_wordbookMismatch]，由设置页弹确认框、给出「仍要恢复」。
                         */
                        if (it is BackupManager.WordbookMismatchException) {
                            _wordbookMismatch.value = MismatchPrompt(file, it.message ?: "")
                        } else {
                            _fileError.value = fileErrorMessage("没能恢复数据", it)
                        }
                    }
            } finally {
                _fileBusy.value = false
            }
        }
    }

    /** 词库不一致时待确认的备份。直接持有文件，避免恢复期间目录被新备份挤掉后按名字找错文件 */
    data class MismatchPrompt(val file: File, val detail: String)

    private val _wordbookMismatch = MutableStateFlow<MismatchPrompt?>(null)
    val wordbookMismatch: StateFlow<MismatchPrompt?> = _wordbookMismatch.asStateFlow()

    fun dismissWordbookMismatch() {
        _wordbookMismatch.value = null
    }

    /** 用户在确认框里点了「仍要恢复」 */
    fun restoreIgnoringWordbookMismatch() {
        val prompt = _wordbookMismatch.value ?: return
        _wordbookMismatch.value = null
        restoreBackup(prompt.file, allowWordbookMismatch = true)
    }

    // ---------- 我的 / 成就 / 排行榜 ----------

    fun refreshProfile(prefsFromRepo: Boolean = false) {
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
                    desktopIconStyle = LauncherIcons.appliedStyle(getApplication()),
                    themeMode = repo.themeMode,
                    animationsEnabled = repo.animationsEnabled,
                    soundEnabled = repo.soundEnabled,
                    leaderboard = repo.userDb.quizRecords(50),
                    wrongRedoCount = repo.userDb.quizWrongCount(repo.stage),
                    aggregate = repo.userDb.quizAggregate()
                )
            }
            /*
             * 设置项必须以「当前值」为准合并，不能整体覆盖。
             * data 是在 IO 线程上按那一刻的 repo 快照拼出来的；用户如果在这一趟读表
             * 期间拨动了开关（setThemeMode / setAutoSpeak 等会立刻 _profile.update），
             * 直接 `_profile.value = data` 就会把刚改的开关按回旧值 ——
             * 表现为「进「我的」页顺手切深色，界面闪一下又变回浅色」。
             * 首次加载（loading=true）时当前值还是默认值，必须整体采用 data，
             * 否则会把默认的级别 / 目标一路带进会话里。
             *
             * 保留 old 值的前提是：**每个 setter 都会立刻把它写进 _profile**。
             * 级别与每日目标早期是例外（只写 _home / 只刷库），于是这里保留的
             * 是「从没被更新过的旧值」，「我的」和设置页就永远显示进应用时的级别和目标。
             * 现在 setStage / setDailyGoal 已经补上回写；新增设置项时请照同一套做，
             * 只写偏好不回写 _profile 的设置项，在这个合并逻辑下必然显示旧值。
             *
             * prefsFromRepo=true 时跳过合并：清空数据与恢复备份已经把偏好整体换掉，
             * old 里的值不再代表本机现状。
             */
            _profile.update { old ->
                if (old.loading || prefsFromRepo) data
                else data.copy(
                    goal = old.goal,
                    autoSpeak = old.autoSpeak,
                    speechRate = old.speechRate,
                    stage = old.stage,
                    iconStyle = old.iconStyle,
                    themeMode = old.themeMode,
                    animationsEnabled = old.animationsEnabled,
                    soundEnabled = old.soundEnabled
                )
            }
        }
    }

    private var leaderboardJob: Job? = null

    fun refreshLeaderboard() {
        // 与其它刷新器一样先取消上一个：并发跑时后完成的那个可能带着旧数据覆盖新结果
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                Triple(
                    repo.userDb.quizRecords(50),
                    repo.userDb.quizAggregate(),
                    repo.userDb.quizWrongCount(repo.stage)
                )
            }
            _profile.update {
                it.copy(leaderboard = data.first, aggregate = data.second, wrongRedoCount = data.third)
            }
        }
    }

    companion object {
        /** 搜索防抖时长：太短会在输入长词时频繁查库，太长会让结果显得迟钝 */
        private const val SEARCH_DEBOUNCE_MS = 250L

        /** 闯关快照的有效期：超过就当作「用户已经不记得这一局了」 */
        private const val RESTORE_WINDOW_MS = 30 * 60 * 1000L
    }
}
