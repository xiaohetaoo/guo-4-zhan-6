package com.hdu.cetvocab.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 全局动效规范。
 *
 * 参照 ColorOS 的「流体」动效思路重新设计，四条原则：
 *
 * 1. **一律用物理弹簧，而不是固定时长的缓动曲线**。固定时长的 tween 在被打断时
 *    只能从头重放，看起来「卡一下」；spring 可以继承当前速度、被新的目标自然接管，
 *    这是「跟手、丝滑」的根源。
 * 2. **位移 / 尺寸类变化允许极轻微回弹**（[spatial]，阻尼比 0.8）。元素落位时
 *    有一点过冲再收回，观感上有重量感，也就是常说的「流体感」。
 * 3. **透明度 / 颜色类变化绝不回弹**（[effects]，阻尼比 1.0）。这类量一旦过冲就会
 *    出现闪烁，必须单调收敛。
 * 4. **同族动画共用同一组参数**。全应用只有这几套弹簧，不各自为政，
 *    这样页面之间的节奏才是统一的「一个动效语言」。
 *
 * 另外提供导航转场助手（[parallelEnter] / [pushEnter] 等）与按压回弹
 * （[springPress]），避免各页面重复手写。
 */
object Motion {

    // ---------------------------------------------------------------- 开关

    /**
     * 全局动效开关（设置页「界面动效」，持久化在 SharedPreferences）。
     *
     * 用 `mutableStateOf` 而不是普通 `var`：所有弹簧工厂和转场助手都是在**组合过程中**
     * 被读取的，只有让快照系统观察它，开关一变全应用才能在同一次重组里改用新参数。
     * 普通 var 要等下一次无关重组才生效，表现为「开关切了、切页还是老动画」。
     *
     * 关闭后：弹簧退化为 `snap()`（瞬间跳到终值）、导航转场退化为 `None`、
     * 按压回弹不再缩放 —— 也就是「所有界面变化都直接落位」。
     */
    var animationsEnabled: Boolean by mutableStateOf(true)
        private set

    /** 由 ViewModel 在启动与用户改动时调用，把持久化值同步进动效系统。 */
    fun applyAnimationsEnabled(enabled: Boolean) {
        animationsEnabled = enabled
    }

    // ---------------------------------------------------------------- 弹簧

    /**
     * 空间位移 / 尺寸：轻微回弹，主用。
     * 用于页面转场、指示器滑动、展开收起等「位置会变」的动画。
     */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        if (!animationsEnabled) snap()
        else spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)

    /**
     * 空间位移 / 尺寸：更快收束、回弹更小，用于小元件。
     * 按钮按压、标签指示器这类幅度很小的位移，用主弹簧会显得拖沓。
     */
    fun <T> spatialSnappy(): FiniteAnimationSpec<T> =
        if (!animationsEnabled) snap()
        else spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMedium)

    /**
     * 透明度 / 颜色：不回弹。
     * 过冲会让淡入淡出出现「闪一下」，必须用临界阻尼。
     */
    fun <T> effects(): FiniteAnimationSpec<T> =
        if (!animationsEnabled) snap()
        else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

    /**
     * 翻卡等需要「一鼓作气走完、不回弹」的场合。
     * 阻尼比 1.0 保证不会转过头，刚度调高让节奏干脆。
     */
    fun <T> flip(): FiniteAnimationSpec<T> =
        if (!animationsEnabled) snap()
        else spring(dampingRatio = 1f, stiffness = 520f)

    // ---------------------------------------------------------------- 导航转场

    /**
     * 平行转场 · 进入：淡入 + 从 94% 放大到 100%。
     *
     * 表达「同一层的内容换了一批」而不暗示方向，适合无法确定左右次序的场合。
     * 底部标签之间的切换已改用带方向的 [slideEnter]，本函数保留给其它平行场景。
     */
    fun parallelEnter(): EnterTransition =
        if (!animationsEnabled) EnterTransition.None
        else fadeIn(effects()) + scaleIn(initialScale = 0.94f, animationSpec = spatial())

    /** 平行转场 · 退出：淡出 + 放大到 104%，与进入形成对称的「推拉」。 */
    fun parallelExit(): ExitTransition =
        if (!animationsEnabled) ExitTransition.None
        else fadeOut(effects()) + scaleOut(targetScale = 1.04f, animationSpec = spatial())

    /**
     * 平行转场 · 带方向的左右滑动 · 进入。
     *
     * 底部四个标签虽然彼此并列、没有层级，但横向排列本身就有次序，
     * 用户点右侧的标签时期待内容「从右边来」，反之亦然 —— 这是手指的直觉。
     * 因此这里按序号增减决定方向，而不是像层级推进那样永远从右侧进。
     *
     * 位移取整屏、并叠一层淡入：整屏平移若不淡入，两页交界的那一帧会出现
     * 硬边，看起来像「抽」了一下；淡入把交界柔化掉。
     *
     * @param forward true 表示目标在右侧（新页从右进），false 表示目标在左侧
     */
    fun slideEnter(forward: Boolean): EnterTransition =
        if (!animationsEnabled) EnterTransition.None
        else slideInHorizontally(animationSpec = spatial()) { if (forward) it else -it } +
            fadeIn(effects())

    /** 平行转场 · 带方向的左右滑动 · 退出：旧页朝新页的反方向让位，两者同步平移。 */
    fun slideExit(forward: Boolean): ExitTransition =
        if (!animationsEnabled) ExitTransition.None
        else slideOutHorizontally(animationSpec = spatial()) { if (forward) -it else it } +
            fadeOut(effects())

    /**
     * 层级推进 · 进入二级页：新页从右侧滑入 + 淡入。
     * 这是「往深处走一层」的标准表达。
     */
    fun pushEnter(): EnterTransition =
        if (!animationsEnabled) EnterTransition.None
        else slideInHorizontally(animationSpec = spatial()) { it } + fadeIn(effects())

    /**
     * 层级推进 · 旧页退让：向左只移动 1/4 屏 + 缩小到 92% + 淡出。
     *
     * 关键在「只移 1/4」：旧页移动得比新页慢，就产生了前后景速度差（视差），
     * 深度感来自速度差而不是位移量本身。早期实现让旧页整屏滑走，
     * 两个页面速度一样，看起来就是「两张纸并排平移」，很平。
     */
    fun pushExit(): ExitTransition =
        if (!animationsEnabled) ExitTransition.None
        else slideOutHorizontally(animationSpec = spatial()) { -it / 4 } +
            scaleOut(targetScale = 0.92f, animationSpec = spatial()) +
            fadeOut(effects())

    /** 返回 · 上一层归位：从左侧 -1/4 处滑回 + 从 92% 放大回 100%。即 [pushExit] 的逆过程。 */
    fun popEnter(): EnterTransition =
        if (!animationsEnabled) EnterTransition.None
        else slideInHorizontally(animationSpec = spatial()) { -it / 4 } +
            scaleIn(initialScale = 0.92f, animationSpec = spatial()) +
            fadeIn(effects())

    /** 返回 · 二级页退出：向右滑走 + 淡出。 */
    fun popExit(): ExitTransition =
        if (!animationsEnabled) ExitTransition.None
        else slideOutHorizontally(animationSpec = spatial()) { it } + fadeOut(effects())

    /** 底部导航栏进出：整条栏从下方弹簧升起 / 落回。 */
    fun barEnter(): EnterTransition =
        if (!animationsEnabled) EnterTransition.None
        else slideInVertically(animationSpec = spatial()) { it } + fadeIn(effects())

    fun barExit(): ExitTransition =
        if (!animationsEnabled) ExitTransition.None
        else slideOutVertically(animationSpec = spatial()) { it } + fadeOut(effects())
}

/**
 * 按压回弹。
 *
 * 用法：先 `remember { MutableInteractionSource() }`，把这个 Modifier 挂在最外层，
 * 再把同一个 [interactionSource] 交给 `clickable(interactionSource = it, indication = null, ...)`。
 *
 * 为什么要显式传 interactionSource 而不是自己写 pointerInput 监听按下：
 * clickable 会消费掉按下事件，另起一个 pointerInput 往往收不到「抬起」，
 * 状态会卡在按下态。复用 clickable 自己的交互源最可靠。
 *
 * 按压缩放取 0.94 而不是更小：卡片面积大，缩太多会显得「弹」，0.94 刚好有回应感。
 */
@Composable
fun Modifier.springPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        // 动效关闭时目标恒为 1f：仍然保留 animateFloatAsState 的读取，
        // 这样开关切换会触发重组，而不是要等一次无关重组才生效。
        targetValue = if (pressed && Motion.animationsEnabled) pressedScale else 1f,
        animationSpec = Motion.spatialSnappy(),
        label = "springPress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
