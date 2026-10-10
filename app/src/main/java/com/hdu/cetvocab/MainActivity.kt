package com.hdu.cetvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.hdu.cetvocab.ui.AppRoot
import com.hdu.cetvocab.ui.theme.CetVocabTheme
import com.hdu.cetvocab.util.LauncherIcons

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CetVocabTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppRoot()
                }
            }
        }
    }

    /**
     * 桌面图标在这里落地。
     *
     * 切换图标要停用 / 启用 activity-alias，而被停用的那个 alias 往往正是当前任务的根组件，
     * 当场切换会让系统结束任务、把用户顶回桌面。放到 onStop 执行，此时应用已经退到后台，
     * 用户看到的是「回到桌面 → 图标已经是新的」，切换过程完全无感。
     *
     * [LauncherIcons.apply] 自带幂等判断，状态已经正确时不会产生任何写入。
     */
    override fun onStop() {
        super.onStop()
        val app = application as CetVocabApp
        LauncherIcons.apply(this, app.repo.iconStyle, app.repo.stage)
    }
}
