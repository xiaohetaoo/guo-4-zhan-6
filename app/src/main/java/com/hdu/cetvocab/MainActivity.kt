package com.hdu.cetvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.ui.AppRoot
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.theme.CetVocabTheme
import com.hdu.cetvocab.util.LauncherIcons
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as CetVocabApp).repo

        setContent {
            // 启动瞬间先用本地偏好里的明暗模式，避免「先按系统渲染、读到偏好后再跳一次」的闪烁
            val initialMode = remember { repo.themeMode }
            val profile by vm.profile.collectAsState()
            val mode = if (profile.loading) initialMode else profile.themeMode
            val darkTheme = when (mode) {
                VocabRepository.THEME_LIGHT -> false
                VocabRepository.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            CetVocabTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppRoot(vm = vm)
                }
            }
        }
    }

    /** 回到前台：恢复学习时长与闯关用时的计时 */
    override fun onStart() {
        super.onStart()
        vm.onAppForeground()
    }

    /**
     * 桌面图标在这里落地。
     *
     * 切换图标要停用 / 启用 activity-alias，而被停用的那个 alias 往往正是当前任务的根组件，
     * 当场切换会让系统结束任务、把用户顶到桌面。放到 onStop 执行，此时应用已经退到后台，
     * 用户看到的是「回到桌面 → 图标已经是新的」，切换过程完全无感。
     *
     * [LauncherIcons.apply] 自带幂等判断，状态已经正确时不会产生任何写入。
     * 这里同时承担「退到后台即停表」——否则用户切出去几分钟再回来，
     * 这段时间会被算进累计学习时长与本关用时。
     */
    override fun onStop() {
        super.onStop()
        vm.onAppBackground()
        val app = application as CetVocabApp
        // 放到后台线程执行：写组件状态是跨进程调用，不宜占用主线程。
        //
        // 这里传 app（Application）而不是 this@MainActivity：appScope 的生命周期
        // 与应用进程等长，把 Activity 引用塞进它启动的协程，就等于让 Activity
        // 在任务栈销毁后仍被协程闭包持有 —— 一次有界但真实的泄漏。
        // LauncherIcons 只需要 Context 拿 PackageManager，Application 完全够用。
        app.appScope.launch {
            runCatching { LauncherIcons.apply(app, app.repo.iconStyle, app.repo.stage) }
        }
    }
}
