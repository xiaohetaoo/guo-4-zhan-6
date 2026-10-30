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

    /**
     * [apply] 的结果。
     *
     * 早期 apply 返回的是 Boolean，含义是「有没有真的发生切换」——
     * 于是「已经是目标图标、无需写入」和「写入失败」都返回 false，
     * 调用方无法区分，只能把失败当正常、把正常当失败。
     * 设置页要按失败给出准确反馈，就必须把这两种情况拆开。
     */
    enum class ApplyResult {
        /** 本次写入让桌面图标变成了目标入口 */
        APPLIED,

        /** 系统状态本来就是对的，没有产生任何写入 */
        UNCHANGED,

        /**
         * 写入失败，或收尾后仍不是「恰好启用目标入口」。
         *
         * 与 [DISABLED] 的区别是「谁的问题」：FAILED 说明目标入口在这台设备上确实存在，
         * 但系统拒绝了这次写入；DISABLED 说明这台设备上根本没有这个入口。
         * 两者要对用户说不同的话，所以不能合成一个值。
         */
        FAILED,

        /**
         * 目标入口在本机不存在（[isAvailable] 判定为假）。
         *
         * 为什么必须单独判一次、不能指望后面的自检兜住：入口不存在时
         * `setComponentEnabledSetting()` **不会报错** —— 它只是把「想启用这个组件」
         * 记进包管理器的偏好；而 [isEnabled] 恰好也读这条偏好，于是
         * 「写入成功」与「此刻生效的入口正是想要的」同时成立，
         * [apply] 会返回 APPLIED，桌面上却什么都没发生。
         * 这个失败骗得过后面的每一步自检，只能在写入之前先问「这个入口真的在吗」。
         */
        DISABLED
    }

    private const val DEFAULT = "com.hdu.cetvocab.LauncherDefault"
    private const val SI = "com.hdu.cetvocab.LauncherSimpleSi"
    private const val LIU = "com.hdu.cetvocab.LauncherSimpleLiu"

    /**
     * 对外的 alias 常量。设置页要按**实际生效**的 alias 推出「四/六」，
     * 就得能认出这两个值；写成 public 常量免得调用方自己抄一遍字符串。
     */
    const val ALIAS_SI = SI
    const val ALIAS_LIU = LIU
    const val ALIAS_DEFAULT = DEFAULT

    private val ALL = listOf(DEFAULT, SI, LIU)

    /**
     * 清单里 `android:enabled="true"` 的入口。
     *
     * 关键点：`getComponentEnabledSetting()` 对「从未被显式设置过」的组件返回
     * `COMPONENT_ENABLED_STATE_DEFAULT(0)`，而 **0 并不等于 1**。若只认 1 为启用，
     * 首次启动（或恢复出厂设置后）就会把清单里默认启用的 [DEFAULT] 误判为「未启用」，
     * 于是 [ensureExactlyOne] 会以为三个入口全关而重写一遍状态，
     * [currentAlias] 也会返回 null。这里显式记下清单的默认值，把 0 解析成真实状态。
     */
    private val MANIFEST_ENABLED = setOf(DEFAULT)

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
    private fun enabledAliases(context: Context): List<String> =
        ALL.filter { isEnabled(context, it) }

    /** 原始组件状态：0=默认（跟随清单）1=启用 2=禁用 */
    private fun enabledState(context: Context, name: String): Int =
        runCatching {
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context.packageName, name)
            )
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

    /** 把「默认」状态按清单声明解析成真实启用与否 */
    private fun isEnabled(context: Context, name: String): Boolean =
        when (enabledState(context, name)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> name in MANIFEST_ENABLED
        }

    /**
     * 写入单个入口的启用状态。
     *
     * 比较的是 [isEnabled]（已把清单默认值算进去）而不是原始状态码：
     * 否则「目标本来就已启用、只是状态码为 DEFAULT」也会多写一次组件状态，
     * 让幂等判断失效。
     *
     * @return true 表示目标状态已经达成（本来就正确，或写入成功）；
     *         false 表示写入失败 —— 调用方必须据此中止后续操作。
     */
    private fun setEnabled(context: Context, name: String, enabled: Boolean): Boolean {
        if (isEnabled(context, name) == enabled) return true
        val pm = context.packageManager
        val component = ComponentName(context.packageName, name)
        val desired = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
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
     *
     * @return true 表示收尾后确实「恰好启用一个」。写入失败时为 false，
     *         调用方可以据此记录异常，而不是默认已经修好。
     */
    private fun ensureExactlyOne(context: Context): Boolean {
        if (enabledAliases(context).size == 1) return true
        // 先把默认入口启用，再停用其余：任何时刻至少有一个入口可用
        var ok = setEnabled(context, DEFAULT, true)
        ALL.filter { it != DEFAULT }.forEach { if (!setEnabled(context, it, false)) ok = false }
        return ok && enabledAliases(context).size == 1
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
     * ## 为什么必须加 @Synchronized
     * 本方法会被两处并发调用：[com.hdu.cetvocab.CetVocabApp.onCreate]（冷启动校正）
     * 与 [com.hdu.cetvocab.MainActivity.onStop]（退到后台落地切换）。两者都跑在
     * 各自的 IO 协程里，完全可能交错执行。若不串行化，就会出现
     * 「A 正在停用旧入口、B 刚读到旧的启用集合」这类撕裂读，
     * 最坏情况下三个入口同时处于禁用状态 —— 桌面图标消失，应用再也打不开。
     * 这是全局唯一的写入点，直接锁在单例上，代价可忽略。
     *
     * @return 见 [ApplyResult]：APPLIED 表示图标真的变了，
     *         UNCHANGED 表示本来就是这样（没做任何写入），
     *         FAILED 表示这台设备切不过去 —— 调用方必须据此收回「回到桌面即可看到」的承诺。
     */
    @Synchronized
    fun apply(context: Context, style: Int, stage: Int): ApplyResult {
        val want = targetAlias(style, stage)

        /*
         * 第零步：先确认目标入口真的在这台设备上。
         *
         * 必须在任何写入之前做，因为「入口不存在」这个失败是自检抓不到的：
         * setComponentEnabledSetting() 对不存在的组件不报错，只写一条偏好，
         * 而自检读的正是同一条偏好，于是写入与自检会一起「成功」。
         * 冷启动（CetVocabApp）与退到后台（MainActivity.onStop）两条路径都走这里，
         * 设置页那次点击前的探测（AppViewModel.setIconStyle）挡不住它们。
         */
        if (!isAvailable(context, want)) {
            ensureExactlyOne(context)
            return ApplyResult.DISABLED
        }

        val before = ALL.associateWith { isEnabled(context, it) }

        // 第一步：先确保目标入口启用。失败则保持现状并收敛不变量后返回。
        if (!setEnabled(context, want, true)) {
            ensureExactlyOne(context)
            return ApplyResult.FAILED
        }

        // 第二步：再停用其余入口。单个失败不影响不变量，因为目标已启用。
        ALL.filter { it != want }.forEach { setEnabled(context, it, false) }

        // 第三步：收尾自检，保证「恰好一个启用」。
        ensureExactlyOne(context)
        val after = ALL.associateWith { isEnabled(context, it) }
        // 自检通过还不够：必须确认「此刻生效的那个入口正是想要的」，
        // 否则说明收尾把图标收敛回了默认入口（写入被 ROM 拒绝时的真实表现）。
        if (currentAlias(context) != want) return ApplyResult.FAILED
        return if (before == after) ApplyResult.UNCHANGED else ApplyResult.APPLIED
    }

    /**
     * 这台设备上有没有 [alias] 这个入口（看清单声明，不看启用状态）。
     *
     * `activity-alias` 声明在包里，理论上一定在，但 ROM 把未使用的组件裁掉时就没了。
     *
     * 判的是「存在」而不是「可用」：若用户在系统里把某个入口禁用，
     * 本方法仍返回 true（组件还在），后续写入是否成功交给 [apply] 的返回值去说
     * ——「这台设备没有这个入口」和「有，但这次没启用成功」不是同一件事，
     * 提示语也不该一样。
     *
     * ## 为什么不能用 getComponentEnabledSetting 判断
     * 它查的是**包管理器里存的那条偏好**，不是组件是否存在：
     * 对一个根本不存在的组件，AOSP 的实现照样返回
     * `COMPONENT_ENABLED_STATE_DEFAULT`，一次异常都不会抛。
     * 用它来判断存在性，任何时候都得到 true —— 等于没判。
     * （历史实现就是这么写的，于是 [com.hdu.cetvocab.ui.AppViewModel.setIconStyle]
     * 里那次「点击前先探一下」永远探测不到，承诺照旧给出去。）
     *
     * 这里改成查包信息的组件清单。必须带 `MATCH_DISABLED_COMPONENTS`：
     * [SI] 与 [LIU] 在清单里就是 `android:enabled="false"`，
     * 不带这个 flag 会把它们过滤掉，于是「简洁图标」永远被判成不存在 ——
     * 那比原实现更糟：会把一个本来能用的功能整个关掉。
     *
     * 查询本身失败（理论上不该发生在自己包上）时返回 true：宁可漏报，
     * 也不要因为一次查询异常就把用户的图标切换入口锁死。
     */
    fun isAvailable(context: Context, alias: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        )
        info.activities?.any { it.name == alias } == true
    }.getOrDefault(true)

    /**
     * 桌面**现在**用的是哪种图标；读不出来返回 null（不猜）。
     *
     * 设置页原本按「用户选了哪个」来写「当前桌面显示」，
     * 于是切换失败时那句话是假的。这一句让用户自己核对系统状态。
     */
    fun appliedStyle(context: Context): Int? = when (currentAlias(context)) {
        DEFAULT -> STYLE_DEFAULT
        SI, LIU -> STYLE_SIMPLE
        else -> null
    }

    /**
     * 桌面**现在**生效的那个入口（[DEFAULT] / [SI] / [LIU]）；读不出来返回 null。
     *
     * [appliedStyle] 不够用：它只把三个入口归成两档（默认 / 简洁），
     * 而简洁档下「四」和「六」是两个不同的 alias。设置页那句
     * 「当前桌面显示：简洁图标「六」」里的字必须来自**实际生效**的 alias，
     * 不能读用户刚选的级别 —— 图标要到 onStop 才落地，
     * 「简洁图标 + 四级 → 回首页切六级 → 立刻进设置页」这个序列里，
     * 页面会写「六」而桌面此刻仍是「四」。
     */
    fun appliedAlias(context: Context): String? = currentAlias(context)
}
