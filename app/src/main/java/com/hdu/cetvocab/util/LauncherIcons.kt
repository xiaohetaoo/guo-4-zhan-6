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
 * 因此 [apply] 全程围绕这条不变量组织。
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
    fun currentAlias(context: Context): String? =
        enabledAliases(context).firstOrNull()

    /** 系统当前处于「已启用」状态的入口列表 */
    private fun enabledAliases(context: Context): List<String> = ALL.filter {
        enabledState(context, it) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun enabledState(context: Context, name: String): Int =
        runCatching {
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context.packageName, name)
            )
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

    /**
     * 写入单个入口的启用状态。
     *
     * @return true 表示目标状态已经达成（本来就正确，或写入成功）；
     *         false 表示写入失败 —— 调用方必须据此中止后续操作。
     */
    private fun setEnabled(context: Context, name: String, enabled: Boolean): Boolean {
        val pm = context.packageManager
        val component = ComponentName(context.packageName, name)
        val desired = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (enabledState(context, name) == desired) return true
        return runCatching {
            pm.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
        }.isSuccess
    }

    /**
     * 收敛「恰好启用一个入口」这条不变量。
     *
     * 若因为任何原因偏离（一个都没启用 → 桌面图标消失、应用打不开；
     * 启用了多个 → 桌面出现重复图标），统一回退到「只启用默认入口」这个确定状态。
     * 宁可图标风格与偏好暂时不符，也绝不能让应用从桌面消失。
     */
    private fun ensureExactlyOne(context: Context) {
        val enabled = enabledAliases(context)
        if (enabled.size == 1) return
        ALL.forEach { setEnabled(context, it, it == DEFAULT) }
    }

    /**
     * 应用图标设置。幂等：若系统当前状态已经正确则不做任何写入，
     * 避免每次冷启动都去改包管理器的组件状态。
     *
     * ## 执行顺序为什么必须是「先启用目标、再停用其余」
     * 反过来的话，从「默认图标」切到「简洁图标」时会短暂出现
     * 「旧的已停用、新的还没启用」的中间态。一旦此时进程被杀或系统回收内存，
     * 三个入口就全部处于禁用状态 —— 桌面图标消失，应用再也打不开。
     * 先启用目标则任何时刻都至少有一个可用入口。
     *
     * ## 为什么不再用 runCatching 静默吞掉失败
     * 历史实现在循环里对每次写入都 `runCatching{}.onSuccess{}`，失败被完全吞掉。
     * 万一「启用目标」失败而「停用旧入口」成功，用户就直接失去了桌面入口，
     * 且没有任何提示、没有自检。现在启用失败会立刻中止，不再动其他入口。
     *
     * @return true 表示确实发生了切换（桌面图标已改变）
     */
    fun apply(context: Context, style: Int, stage: Int): Boolean {
        val want = targetAlias(style, stage)
        val before = ALL.associateWith { enabledState(context, it) }

        // 第一步：先确保目标入口启用。失败则保持现状并收敛不变量后返回。
        if (!setEnabled(context, want, true)) {
            ensureExactlyOne(context)
            return false
        }

        // 第二步：再停用其余入口。单个失败不影响不变量，因为目标已启用。
        ALL.filter { it != want }.forEach { setEnabled(context, it, false) }

        // 第三步：收尾自检，保证「恰好一个启用」。
        ensureExactlyOne(context)

        val after = ALL.associateWith { enabledState(context, it) }
        return before != after
    }
}
