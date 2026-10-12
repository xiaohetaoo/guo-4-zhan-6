package com.hdu.cetvocab

import android.app.Application
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.util.LauncherIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CetVocabApp : Application() {

    val repo: VocabRepository by lazy { VocabRepository(this) }

    /**
     * 应用级协程作用域。
     * 用于「不该被任何界面生命周期打断」的后台任务（例如写桌面图标组件状态）。
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 每次冷启动都校正一次桌面图标：
        // 覆盖「恢复备份带回旧偏好」「系统清理组件状态」「应用升级」等情况，
        // LauncherIcons.apply 内部做了幂等判断，状态已正确时不会产生写入。
        //
        // 必须放到后台线程：apply 内部最多会做 6 次
        // getComponentEnabledSetting / setComponentEnabledSetting 的跨进程调用，
        // 在 Application.onCreate 里同步执行会直接拖慢冷启动。
        appScope.launch {
            runCatching { LauncherIcons.apply(this@CetVocabApp, repo.iconStyle, repo.stage) }
        }
    }
}
