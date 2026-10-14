package com.hdu.cetvocab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdu.cetvocab.R
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hdu.cetvocab.ui.screens.AchievementsScreen
import com.hdu.cetvocab.ui.screens.HomeScreen
import com.hdu.cetvocab.ui.screens.LeaderboardScreen
import com.hdu.cetvocab.ui.screens.LearnScreen
import com.hdu.cetvocab.ui.screens.ProfileScreen
import com.hdu.cetvocab.ui.screens.QuizScreen
import com.hdu.cetvocab.ui.screens.SettingsScreen
import com.hdu.cetvocab.ui.screens.StudyScreen
import com.hdu.cetvocab.ui.screens.WordDetailScreen
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.util.Speech

object Routes {
    const val HOME = "home"
    const val LEARN = "learn"
    const val QUIZ = "quiz"
    const val PROFILE = "profile"
    const val STUDY = "study"
    const val ACHIEVEMENTS = "achievements"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val WORD = "word"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "首页", Icons.Filled.Home),
    Tab(Routes.LEARN, "词库", Icons.AutoMirrored.Filled.List),
    Tab(Routes.QUIZ, "闯关", Icons.Filled.PlayArrow),
    Tab(Routes.PROFILE, "我的", Icons.Filled.Person)
)

/** 底部标签对应的路由集合，用于判断一次转场是「同级平行」还是「进入下一层」。 */
private val TAB_ROUTES = TABS.map { it.route }.toSet()

private fun NavBackStackEntry?.isTab(): Boolean =
    this?.destination?.route in TAB_ROUTES

/**
 * 两端都是标签时为 true，此时按左右次序做滑动转场。
 *
 * 名字不叫 `and`：`and` 在 Kotlin 里是 `Boolean` 上的 infix 函数名，
 * 自定义同名函数必须补 `infix` 修饰符才行，容易踩坑。
 */
private fun bothTabs(a: NavBackStackEntry?, b: NavBackStackEntry?): Boolean =
    a.isTab() && b.isTab()

/**
 * 底部标签之间的左右滑动手势：手指向左滑看右边的标签，向右滑看左边的标签。
 *
 * 只在容器层挂一次，不给每个页面各写一遍 —— 导航状态只有这里知道，
 * 而且二级页本身就不该响应（它有「返回」语义，再叠横滑切页会误触）。
 *
 * 判定用**整轮手势的累计位移**而不是单帧速度：
 * - 阈值 72dp（约屏宽 1/15），长按后小幅抖动不会触发；
 * - 一轮手势只触发一次（[fired]），否则一次长滑会连跳好几页；
 * - 用 `detectHorizontalDragGestures` 而不是裸 `pointerInput`：
 *   它内部先做方向抢占，竖向列表滚动时不会把手势抢走。
 *
 * 目标页由 [goTab] 统一跳转 —— 方向和导航必须一起走，否则会退回
 * 「按 NavController 内部判断」那条会出错的路（详见 [AppRoot] 里的说明）。
 *
 * @param index 当前所在标签的序号；传 -1（当前是二级页）时不挂手势
 */
private fun Modifier.tabSwipe(index: Int, goTab: (Int) -> Unit): Modifier {
    if (index < 0) return this
    return pointerInput(index) {
        val threshold = 72.dp.toPx()
        var total = 0f
        var fired = false
        fun reset() { total = 0f; fired = false }
        detectHorizontalDragGestures(
            onDragStart = { reset() },
            onDragEnd = { reset() },
            onDragCancel = { reset() },
            onHorizontalDrag = { _, dragAmount ->
                if (fired) return@detectHorizontalDragGestures
                total += dragAmount
                // 向左滑（位移为负）→ 看右边的标签；向右滑 → 看左边的标签
                val target = when {
                    total <= -threshold -> index + 1
                    total >= threshold -> index - 1
                    else -> -1
                }
                if (target in TABS.indices) {
                    fired = true
                    goTab(target)
                }
            }
        )
    }
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    // 系统 TTS：完全离线，不产生网络请求。
    // 「语速」与「自动朗读」两个开关以回调形式注入，Speech 内部每次发音都取最新值，
    // 因此用户改设置后无需重建实例即可生效。
    val speech = remember {
        Speech(context, { vm.speechRate }, { vm.autoSpeak })
    }
    DisposableEffect(Unit) {
        onDispose { speech.shutdown() }
    }

    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val toast by vm.toast.collectAsState()

    LaunchedEffect(toast) {
        toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = TABS.any { it.route == route }
    val tabIndex = TABS.indexOfFirst { it.route == route }

    /*
     * 首帧数据就绪前的占位。
     *
     * 三个首页/词库/「我的」状态都带 loading=true 默认值，但页面只读数据不读标志：
     * 冷启动（首启还要把 7.98 MB 词库拷进 files 目录）期间它们各自为空，
     * 于是用户会先看到一组**错的**内容 —— 首页队列 0 → 「今日已完成 · 额度用满」，
     * 词库页整片空白（连空态提示都不该出现，因为还不是空，是还没加载），
     * 「我的」统计全 0。数据到位后内容再翻回正确的一版，看起来像应用自己算错过。
     *
     * 用前景遮罩而不是把 NavHost 换成占位页：返回栈、底部栏、主题都保持原样，
     * 就绪后遮罩消失，不会额外触发一次导航或让底部栏重播入场动画。
     */
    val homeState by vm.home.collectAsState()
    val learnState by vm.learn.collectAsState()
    val profileState by vm.profile.collectAsState()
    val bootPending = homeState.loading || learnState.loading || profileState.loading

    /*
     * 本次标签切换的滑动方向：`1` 目标在右侧（内容从右往左推），`-1` 目标在左侧。
     *
     * 为什么必须由「用户点了哪个标签」在导航**之前**算好，而不是在转场 lambda 里
     * 用 NavController 给的 initialState / targetState 反推：
     * 底部标签用 `popUpTo(HOME) + restoreState` 切换，跨多级跳转（我的 → 词库）
     * 在 NavController 内部会被拆成两段 —— 先 pop 回首页、再 navigate 到词库。
     * 于是用户看到的不是「词库从左边滑进来」，而是「首页一闪，词库再从右边滑进来」，
     * 方向和预期正好相反。这种错乱只在跨越了中间标签时才出现，所以是「偶有几页」。
     *
     * 自己按用户意图定方向，就与 NavController 内部怎么拆无关了。
     */
    var swipeDir by remember { mutableIntStateOf(1) }

    /** 切到第 index 个标签：先定方向，再导航。两者必须是一个动作。 */
    fun goTab(index: Int) {
        if (index !in TABS.indices || index == tabIndex) return
        if (tabIndex >= 0) swipeDir = index.compareTo(tabIndex)
        nav.navigate(TABS[index].route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) { vm.refreshAll() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // 整条栏用弹簧升降，而不是默认的「瞬间展开」——二级页面推入时
            // 栏体下沉、内容上移是同一拍，观感才连贯。
            AnimatedVisibility(
                visible = showBar,
                enter = Motion.barEnter(),
                exit = Motion.barExit()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    TABS.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { goTab(i) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        // 横滑切标签的手势挂在容器层而不是各个页面：
        // 只有这里同时知道「当前是第几个标签」和 NavController，
        // 且二级页（学习 / 详情 / 设置）不该响应横滑，交给 AppNavHost 内部会一视同仁。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .tabSwipe(tabIndex, ::goTab)
        ) {
            AppNavHost(nav, vm, swipeDir, ::goTab, speech::speak, speech::speakAuto)

            AnimatedVisibility(
                visible = bootPending,
                enter = fadeIn(Motion.effects()),
                exit = fadeOut(Motion.effects())
            ) {
                BootPlaceholder()
            }
        }
    }
}

/** 词库与统计尚未就绪时的整屏占位（见 [AppRoot] 里 bootPending 的注释） */
@Composable
private fun BootPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    vm: AppViewModel,
    swipeDir: Int,
    goTab: (Int) -> Unit,
    speak: (String) -> Unit,
    speakAuto: (String) -> Unit
) {
    /*
     * 进学习页统一走这里，必须带 launchSingleTop。
     *
     * 学习入口都是「先在 IO 线程取词，取到了才跳转」（见 AppViewModel.startQueue 的回调），
     * 于是从点击到跳转之间有几十到几百毫秒的窗口，期间按钮没有禁用。
     * 连点两下就会压进两张 STUDY：学完点「完成」只 pop 掉一张，
     * 用户留在一张空队列的学习页上（StudyScreen 的空态），退不出去还得再点一次。
     */
    fun goStudy() = nav.navigate(Routes.STUDY) { launchSingleTop = true }

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        /*
         * 转场策略：按「两个页面是不是同一层」二分，而不是给每个页面写死一套。
         *
         * - 两端都是底部标签 → 左右滑动，方向取 [swipeDir]（由用户点了哪个标签决定）。
         *   四个标签横向排列，用户对手势方向有明确预期，滑动的方向感才对得上。
         * - 只要有一端不是标签（二级页 / 学习页 / 单词详情）→ 层级推进，
         *   新页从右侧滑入，旧页只左移 1/4 屏并缩小变暗，形成前后景视差。
         *
         * 四条转场都读同一个 [swipeDir]、都判「两端是否都是标签」：
         * 底部标签用 `popUpTo(HOME) + restoreState` 切换，同一组标签之间来回切，
         * NavController 有时判成 navigate、有时判成 pop，走哪条分支并不固定。
         * 只要分支判断一致、方向同源，观感就是连续的。
         */
        enterTransition = {
            if (bothTabs(initialState, targetState)) Motion.slideEnter(swipeDir >= 0)
            else Motion.pushEnter()
        },
        exitTransition = {
            if (bothTabs(initialState, targetState)) Motion.slideExit(swipeDir >= 0)
            else Motion.pushExit()
        },
        popEnterTransition = {
            if (bothTabs(initialState, targetState)) Motion.slideEnter(swipeDir >= 0)
            else Motion.popEnter()
        },
        popExitTransition = {
            if (bothTabs(initialState, targetState)) Motion.slideExit(swipeDir >= 0)
            else Motion.popExit()
        }
    ) {

        composable(Routes.HOME) {
            // 学习页一律「取到词再跳转」：先 navigate 再异步装队列的话，
            // 空队列（或冷启动拷词库的那一下）会让用户先看到学习页上的空态。
            HomeScreen(
                vm = vm,
                onStartToday = {
                    vm.startTodayStudy { ok -> if (ok) goStudy() }
                },
                onStartExtra = {
                    vm.startExtraStudy { ok -> if (ok) goStudy() }
                }
            )
        }

        composable(Routes.LEARN) {
            LearnScreen(
                vm = vm,
                onOpenUnit = { unit ->
                    vm.startUnitStudy(unit) { ok -> if (ok) goStudy() }
                },
                onOpenWord = { id -> nav.navigate("${Routes.WORD}/$id") },
                onStartFav = {
                    vm.startFavStudy { ok -> if (ok) goStudy() }
                },
                onStartWrong = {
                    vm.startWrongStudy { ok -> if (ok) goStudy() }
                }
            )
        }

        composable(Routes.QUIZ) {
            QuizScreen(
                vm = vm,
                // 退出闯关 = 离开闯关页回到上一个标签页。
                // 原先这里写的是「跳到排行榜」：一局打完点「返回」会被强行带到排行榜，
                // 而排行榜另有入口（闯关页内的入口 + 结算页按钮），属于重复跳转。
                onExit = { nav.popBackStack() },
                // 拓展练习从「我的」页进来，退出就该回「我的」。
                // 用 popBackStack 会被 popUpTo(HOME) 的返回栈带回**首页**，
                // 与结算页文案「返回后可再次进入拓展练习」的承诺不符。
                onOpenProfile = { goTab(TABS.indexOfFirst { it.route == Routes.PROFILE }) },
                onSpeak = speak,
                onSpeakAuto = speakAuto,
                onOpenLeaderboard = { nav.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                vm = vm,
                onOpenAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                // 拓展资源练习：入口在「我的」页，但答题界面复用闯关页。
                // **装好题再切标签**：此前是先 goTab、再异步装题库，
                // 用户会先看到闯关选关页闪一下才进关卡；题库为空时更是直接停在选关页。
                // 与底部标签点击走同一条 goTab 路径，返回栈与滑动方向才都一致。
                onOpenConfusable = {
                    vm.startConfusableQuiz { ok ->
                        if (ok) goTab(TABS.indexOfFirst { it.route == Routes.QUIZ })
                    }
                },
                onOpenTranslation = {
                    vm.startTranslationQuiz { ok ->
                        if (ok) goTab(TABS.indexOfFirst { it.route == Routes.QUIZ })
                    }
                }
            )
        }

        composable(Routes.STUDY) {
            StudyScreen(
                vm = vm,
                speak = speak,
                speakAuto = speakAuto,
                onFinish = {
                    // 结束本次学习：清空学习态并刷新首页 / 词库 / 我的，
                    // 同时弹出本次新解锁的成就提示
                    vm.finishStudy()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable("${Routes.WORD}/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            WordDetailScreen(vm = vm, wordId = id, speak = speak, onBack = { nav.popBackStack() })
        }
    }
}
