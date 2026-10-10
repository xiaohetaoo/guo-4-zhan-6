package com.hdu.cetvocab.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.hdu.cetvocab.data.WordDb

/**
 * 桌面图标切换器。
 *
 * 应用在 AndroidManifest 里声明了三个 activity-alias，各自绑定不同的图标，
 * 同一时刻只启用其中一个，从而实现「默认图标 / 简洁·四 / 简洁·六」的切换：
 *
 * - [DEFAULT]  书本图标，用户选择「默认图标」时启用；
 * - [SI]       橙底白字「四」，用户选择「简洁图标」且级别为四级时启用；
 * - [LIU]      橙底白字「六」，用户选择「简洁图标」且级别为六级时启用。
 *
 * 用 [PackageManager.DONT_KILL_APP] 提交，避免切换瞬间把正在使用的应用杀掉。
 * 注意：三个入口必须【恰好启用一个】，全部关掉会导致桌面图标消失、应用再也打不开，
 * 因此这里始终按「目标 = 启用，其余 = 停用」成组提交。
 */
object LauncherIcons {

    /** 图标风格：默认（书本） */
    const val STYLE_DEFAULT = 0

    /** 图标风格：简洁（橙底白字，跟随级别显示「四」或「六」） */
    const val STYLE_SIMPLE = 1

    private const val DEFAULT = "com.hdu.cetvocab.LauncherDefault"
    private const val SI = "com.hdu.cetvocab.LauncherSimpleSi"
    private const val LIU = "com.hdu.cetvocab.LauncherSimpleLiu"

    private val ALL = listOf(DEFAULT, SI, LIU)

    /** 按当前风格与级别算出应该启用的入口，供界面提示用。 */
    fun targetAlias(style: Int, stage: Int): String = when {
        style != STYLE_SIMPLE -> DEFAULT
        stage == WordDb.STAGE_CET6 -> LIU
        else -> SI
    }

    /** 当前生效的入口（读取系统实际状态，便于界面回显与自检）。 */
    fun currentAlias(context: Context): String? {
        val pm = context.packageManager
        return ALL.firstOrNull { name ->
            runCatching {
                pm.getComponentEnabledSetting(ComponentName(context.packageName, name))
            }.getOrNull() == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }

    /**
     * 应用图标设置。幂等：若系统当前状态已经正确则不做任何写入，
     * 避免每次冷启动都去改包管理器的组件状态。
     *
     * @return true 表示确实发生了切换（桌面图标已改变）
     */
    fun apply(context: Context, style: Int, stage: Int): Boolean {
        val pm = context.packageManager
        val want = targetAlias(style, stage)
        var changed = false

        ALL.forEach { name ->
            val component = ComponentName(context.packageName, name)
            val desired = if (name == want) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val current = runCatching {
                pm.getComponentEnabledSetting(component)
            }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            if (current != desired) {
                runCatching {
                    pm.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
                }.onSuccess { changed = true }
            }
        }
        return changed
    }
}
