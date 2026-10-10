package com.hdu.cetvocab

import android.app.Application
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.util.LauncherIcons

class CetVocabApp : Application() {

    val repo: VocabRepository by lazy { VocabRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // 每次冷启动都校正一次桌面图标：
        // 覆盖「恢复备份带回旧偏好」「系统清理组件状态」「应用升级」等情况，
        // LauncherIcons.apply 内部做了幂等判断，状态已正确时不会产生写入。
        LauncherIcons.apply(this, repo.iconStyle, repo.stage)
    }
}
