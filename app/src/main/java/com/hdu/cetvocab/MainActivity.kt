package com.hdu.cetvocab

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
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
import androidx.compose.ui.graphics.toArgb
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.ui.AppRoot
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.theme.CetVocabTheme
import com.hdu.cetvocab.ui.theme.DarkBackground
import com.hdu.cetvocab.ui.theme.DarkSurface
import com.hdu.cetvocab.ui.theme.WarmBackground
import com.hdu.cetvocab.ui.theme.WarmSurface
import com.hdu.cetvocab.util.LauncherIcons
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as CetVocabApp).repo

        // 冷启动窗口的底色必须按「应用自己的明暗模式」显式覆盖。
        // themes.xml 里的 @color/warm_background 由 values / values-night 两套资源按
        // **系统 uiMode** 解析，而设置页允许把应用固定为浅色或深色；两者不一致时
        // （系统深色 + 应用固定浅色）窗口会先亮一帧深色底，再被 Compose 首帧接管，
        // 表现为启动时闪一下。CetVocabTheme 里的 SideEffect 只管系统栏，
        // 管不到「首帧之前」的窗口背景。
        val systemDark = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val darkNow = when (repo.themeMode) {
            VocabRepository.THEME_LIGHT -> false
            VocabRepository.THEME_DARK -> true
            else -> systemDark
        }
        val background = if (darkNow) DarkBackground else WarmBackground
        val surface = if (darkNow) DarkSurface else WarmSurface
        window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
        window.statusBarColor = background.toArgb()
        window.navigationBarColor = surface.toArgb()

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

    /**
     * 可见且可交互 = 开始计时。
     *
     * 用 onResume/onPause 而不是 onStart/onStop：下拉通知栏、系统权限弹窗、
     * TTS 语音包准备弹窗这些都只让 Activity 走到 onPause（它们不是全屏 Activity，
     * 不会触发 onStop）。挂在 onStart/onStop 上时，用户停着不动的这几十秒
     * 会被 flushStudyTimer 漏掉、pauseQuizTimer 也不会触发，
     * 直接虚高「累计学习时长」和「本关用时」—— 后者是要进排行榜的。
     */
    override fun onResume() {
        super.onResume()
        vm.onAppForeground()
    }

    /** 失去焦点即停表，口径与 [onResume] 对称 */
    override fun onPause() {
        super.onPause()
        vm.onAppBackground()
    }

    /**
     * 桌面图标在这里落地。
     *
     * 切换图标要停用 / 启用 activity-alias，而被停用的那个 alias 往往正是当前任务的根组件，
     * 当场切换会让系统结束任务、把用户顶到桌面。放到 onStop 执行，此时应用已经退到后台，
     * 用户看到的是「回到桌面 → 图标已经是新的」，切换过程完全无感。
     *
     * [LauncherIcons.apply] 自带幂等判断，状态已经正确时不会产生任何写入。
     * 计时已上移到 [onPause]，这里只负责图标落地。
     */
    override fun onStop() {
        super.onStop()
        val app = application as CetVocabApp
        // 放到后台线程执行：写组件状态是跨进程调用，不宜占用主线程。
        //
        // 这里传 app（Application）而不是 this@MainActivity：appScope 的生命周期
        // 与应用进程等长，把 Activity 引用塞进它启动的协程，就等于让 Activity
        // 在任务栈销毁后仍被协程闭包持有 —— 一次有界但真实的泄漏。
        // LauncherIcons 只需要 Context 拿 PackageManager，Application 完全够用。
        app.appScope.launch {
            // 失败不在这里直接提示：应用此刻已经在后台，Snackbar 没人看得见。
            // 交给 onIconApplyFailed() 存起来，下次回到前台补一句（K10）。
            val result = runCatching {
                LauncherIcons.apply(app, app.repo.iconStyle, app.repo.stage)
            }.getOrDefault(LauncherIcons.ApplyResult.FAILED)
            if (result == LauncherIcons.ApplyResult.FAILED) vm.onIconApplyFailed()
        }
    }
}
