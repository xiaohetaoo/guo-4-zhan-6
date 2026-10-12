package com.hdu.cetvocab.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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

    // ---------------------------------------------------------------- 弹簧

    /**
     * 空间位移 / 尺寸：轻微回弹，主用。
     * 用于页面转场、指示器滑动、展开收起等「位置会变」的动画。
     */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)

    /**
     * 空间位移 / 尺寸：更快收束、回弹更小，用于小元件。
     * 按钮按压、标签指示器这类幅度很小的位移，用主弹簧会显得拖沓。
     */
    fun <T> spatialSnappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMedium)

    /**
     * 透明度 / 颜色：不回弹。
     * 过冲会让淡入淡出出现「闪一下」，必须用临界阻尼。
     */
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

    /**
     * 翻卡等需要「一鼓作气走完、不回弹」的场合。
     * 阻尼比 1.0 保证不会转过头，刚度调高让节奏干脆。
     */
    fun <T> flip(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 520f)

    // ---------------------------------------------------------------- 导航转场

    /**
     * 平行转场 · 进入：淡入 + 从 94% 放大到 100%。
     *
     * 底部标签之间是**并列关系**，没有「谁在谁上面」，所以不能用左右滑动去暗示层级，
     * 否则方向感是错的（从「我的」切到「首页」时往左滑，逻辑上说不通）。
     * 用「淡入 + 轻微放大」表达「同一层的内容换了一批」。
     */
    fun parallelEnter(): EnterTransition =
        fadeIn(effects()) + scaleIn(initialScale = 0.94f, animationSpec = spatial())

    /** 平行转场 · 退出：淡出 + 放大到 104%，与进入形成对称的「推拉」。 */
    fun parallelExit(): ExitTransition =
        fadeOut(effects()) + scaleOut(targetScale = 1.04f, animationSpec = spatial())

    /**
     * 层级推进 · 进入二级页：新页从右侧滑入 + 淡入。
     * 这是「往深处走一层」的标准表达。
     */
    fun pushEnter(): EnterTransition =
        slideInHorizontally(animationSpec = spatial()) { it } + fadeIn(effects())

    /**
     * 层级推进 · 旧页退让：向左只移动 1/4 屏 + 缩小到 92% + 淡出。
     *
     * 关键在「只移 1/4」：旧页移动得比新页慢，就产生了前后景速度差（视差），
     * 深度感来自速度差而不是位移量本身。早期实现让旧页整屏滑走，
     * 两个页面速度一样，看起来就是「两张纸并排平移」，很平。
     */
    fun pushExit(): ExitTransition =
        slideOutHorizontally(animationSpec = spatial()) { -it / 4 } +
            scaleOut(targetScale = 0.92f, animationSpec = spatial()) +
            fadeOut(effects())

    /** 返回 · 上一层归位：从左侧 -1/4 处滑回 + 从 92% 放大回 100%。即 [pushExit] 的逆过程。 */
    fun popEnter(): EnterTransition =
        slideInHorizontally(animationSpec = spatial()) { -it / 4 } +
            scaleIn(initialScale = 0.92f, animationSpec = spatial()) +
            fadeIn(effects())

    /** 返回 · 二级页退出：向右滑走 + 淡出。 */
    fun popExit(): ExitTransition =
        slideOutHorizontally(animationSpec = spatial()) { it } + fadeOut(effects())

    /** 底部导航栏进出：整条栏从下方弹簧升起 / 落回。 */
    fun barEnter(): EnterTransition =
        slideInVertically(animationSpec = spatial()) { it } + fadeIn(effects())

    fun barExit(): ExitTransition =
        slideOutVertically(animationSpec = spatial()) { it } + fadeOut(effects())
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
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.spatialSnappy(),
        label = "springPress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
