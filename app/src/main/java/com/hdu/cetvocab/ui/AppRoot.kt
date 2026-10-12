package com.hdu.cetvocab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                if (route != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppNavHost(nav, vm, speech::speak, speech::speakAuto)
        }
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    vm: AppViewModel,
    speak: (String) -> Unit,
    speakAuto: (String) -> Unit
) {
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        /*
         * 转场策略：按「两个页面是不是同一层」二分，而不是给每个页面写死一套。
         *
         * - 两端都是底部标签 → 平行转场（淡入淡出 + 轻微缩放），不表达方向；
         * - 只要有一端不是标签（二级页 / 学习页 / 单词详情）→ 层级推进，
         *   新页从右侧滑入，旧页只左移 1/4 屏并缩小变暗，形成前后景视差。
         *
         * 之所以四条转场都判 `initialState.isTab() && targetState.isTab()`：
         * 底部标签用 `popUpTo(HOME) + restoreState` 切换，从「我的」回「首页」
         * 在 NavController 眼里是一次 **pop** 而不是 navigate，如果只按 navigate 判，
         * 同一组标签之间来回切会一会儿滑、一会儿淡，方向感是乱的。
         */
        enterTransition = {
            if (initialState.isTab() && targetState.isTab()) Motion.parallelEnter()
            else Motion.pushEnter()
        },
        exitTransition = {
            if (initialState.isTab() && targetState.isTab()) Motion.parallelExit()
            else Motion.pushExit()
        },
        popEnterTransition = {
            if (initialState.isTab() && targetState.isTab()) Motion.parallelEnter()
            else Motion.popEnter()
        },
        popExitTransition = {
            if (initialState.isTab() && targetState.isTab()) Motion.parallelExit()
            else Motion.popExit()
        }
    ) {

        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onStartToday = {
                    vm.startTodayStudy()
                    nav.navigate(Routes.STUDY)
                }
            )
        }

        composable(Routes.LEARN) {
            LearnScreen(
                vm = vm,
                onOpenUnit = { unit ->
                    vm.startUnitStudy(unit)
                    nav.navigate(Routes.STUDY)
                },
                onOpenWord = { id -> nav.navigate("${Routes.WORD}/$id") },
                onStartFav = {
                    vm.startFavStudy()
                    nav.navigate(Routes.STUDY)
                },
                onStartWrong = {
                    vm.startWrongStudy()
                    nav.navigate(Routes.STUDY)
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
                onSpeak = speak,
                onSpeakAuto = speakAuto,
                onOpenLeaderboard = { nav.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                vm = vm,
                onOpenAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
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
