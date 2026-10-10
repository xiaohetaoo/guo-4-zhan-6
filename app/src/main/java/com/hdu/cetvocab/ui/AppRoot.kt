package com.hdu.cetvocab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
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
    Tab(Routes.LEARN, "词库", Icons.Filled.List),
    Tab(Routes.QUIZ, "闯关", Icons.Filled.PlayArrow),
    Tab(Routes.PROFILE, "我的", Icons.Filled.Person)
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    // 系统 TTS：完全离线，不产生网络请求
    val speech = remember { Speech(context) { vm.speechRate } }
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
            AnimatedVisibility(visible = showBar) {
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
            AppNavHost(nav, vm, speech::speak)
        }
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    vm: AppViewModel,
    speak: (String) -> Unit
) {
    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onStartToday = {
                    vm.startTodayStudy()
                    nav.navigate(Routes.STUDY)
                },
                onOpenUnit = { unit ->
                    vm.startUnitStudy(unit)
                    nav.navigate(Routes.STUDY)
                },
                onOpenLearn = { nav.navigate(Routes.LEARN) },
                onOpenQuiz = { nav.navigate(Routes.QUIZ) },
                onOpenLeaderboard = { nav.navigate(Routes.LEADERBOARD) },
                onOpenAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onOpenWord = { id -> nav.navigate("${Routes.WORD}/$id") }
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
                },
                onQuizUnit = { unit ->
                    // 词库页每个单元上的「闯关」按钮：先出题，再跳到闯关页。
                    // 之前只调了 startQuiz() 没导航，点了界面毫无反应（死按钮）。
                    // 这里沿用底部标签页的跳转方式，保证底部导航高亮正确、返回栈不堆积。
                    vm.startQuiz(unit)
                    nav.navigate(Routes.QUIZ) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.QUIZ) {
            QuizScreen(
                vm = vm,
                onExit = { nav.navigate(Routes.LEADERBOARD) },
                onSpeak = speak
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                vm = vm,
                onOpenAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onOpenLeaderboard = { nav.navigate(Routes.LEADERBOARD) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.STUDY) {
            StudyScreen(
                vm = vm,
                speak = speak,
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
