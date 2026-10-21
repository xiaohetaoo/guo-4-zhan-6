package com.hdu.cetvocab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hdu.cetvocab.ui.theme.Motion

/**
 * 新手教程的一步：高亮哪块区域 + 说什么。
 *
 * [target] 由被讲解的控件自己上报位置（见 [rememberTutorialAnchor]）。
 * 传 null 表示这一步没有具体指向，遮罩整片盖满、气泡居中 ——
 * 用来讲「这一屏是什么」这类整体说明。
 */
data class TutorialStep(
    val title: String,
    val body: String,
    val target: TutorialAnchor?
)

/**
 * 被高亮控件的坐标上报器。
 *
 * 为什么让控件自己上报、而不是由教程层去「找」：Compose 没有查询任意
 * Composable 位置的公开 API，唯一可靠的做法是让目标自己在 `onGloballyPositioned`
 * 里把坐标交出来。于是在学习页的那几个控件上挂
 * `Modifier.tutorialAnchor(anchor)`，教程层读 [bounds] 即可。
 *
 * **必须用 `boundsInRoot()`，并且教程层也必须铺满 root**（见 [TutorialOverlay] 的说明）：
 * `boundsInWindow()` 会把状态栏与导航栏的高度也算进去，两层坐标系差一个竖直偏移，
 * 越靠屏幕下方的目标偏得越多 —— 实测底部按钮组的洞会被推出画布之外，
 * 表现为「最后一步没有高亮」。
 *
 * 有意做成普通可变对象而非 State：坐标在布局阶段频繁回写，用 State 包一层会让
 * 每次布局位移都触发一次教程层重组；而教程层真正需要的只是「当前该画在哪」，
 * 在步骤切换时读一次就够（见 [TutorialOverlay] 里的 LaunchedEffect）。
 */
class TutorialAnchor {
    var bounds: Rect? = null
        private set

    internal fun update(rect: Rect) {
        bounds = rect
    }
}

/** 把一个 [TutorialAnchor] 挂到控件上，持续上报它在根坐标系里的位置 */
fun Modifier.tutorialAnchor(anchor: TutorialAnchor): Modifier = this.onGloballyPositioned {
    anchor.update(it.boundsInRoot())
}

/** 建一个稳定的锚点，供 [Modifier.tutorialAnchor] 使用 */
@Composable
fun rememberTutorialAnchor(): TutorialAnchor = remember { TutorialAnchor() }

/** 遮罩色：暖色调下的深褐，比纯黑更贴主题；透明度保证底下内容仍可辨认 */
private val ScrimColor = Color(0xE6231A14)

/**
 * 高亮框相对控件的向外扩张量。
 *
 * 太小边框会压在控件内容上；太大高亮区会指向旁边不相干的东西。
 * 6dp 大致是「一眼看出框住的是这块，但框本身不碰到里面的字」的距离。
 */
private val HALO: Dp = 6.dp

/** 洞的圆角 */
private val HoleCorner: Dp = 16.dp

/**
 * 聚光灯遮罩 + 气泡。
 *
 * 做法是「一整片半透明遮罩，在高亮区挖一个圆角洞」。挖洞用 Path 的
 * EvenOdd 填充规则，而不是在遮罩上叠一块透明色块 —— 后者做不出「洞」的效果，
 * 洞里的内容会被那层半透明压暗，与「聚光灯」的语义正好相反。
 *
 * 两条必须守住的约定：
 * 1. **遮罩要自己吞掉手势**。Compose 的命中测试会穿过没有输入处理的兄弟节点，
 *    落到下面真实控件上 —— 高亮圈写着「点这里」，用户却可能点到旁边那些
 *    看不见的按钮（甚至把四六级切了）。唯一点得动的是气泡里的两个按钮。
 *    这与 `AppRoot.BootPlaceholder` 是同一个坑，那边的注释写得更细。
 * 2. **洞的位置走弹簧过渡**，不硬跳。步骤之间的目标控件在屏幕上差得很远
 *    （顶栏 → 卡片中央 → 底栏），硬跳会让遮罩「闪一下换个地方」，
 *    看不出「聚光灯移过去了」。关掉界面动效时弹簧退化成 snap，与全站一致。
 *
 * @param step 当前步骤；null 表示教程已关闭（整个遮罩不渲染）
 * @param stepIndex 当前步骤下标，从 0 起
 * @param stepCount 总步数，只用于「2 / 5」这样的进度显示
 * @param onNext 点「下一步」
 * @param onSkip 点「跳过教程」
 */
@Composable
fun TutorialOverlay(
    step: TutorialStep?,
    stepIndex: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (step == null) return

    val density = LocalDensity.current
    val haloPx = with(density) { HALO.toPx() }
    val cornerPx = with(density) { HoleCorner.toPx() }

    /*
     * 遮罩自己在 root 坐标系里的原点。
     *
     * 锚点上报的是 `boundsInRoot()`，而遮罩通常**不是**贴在窗口左上角：
     * 学习页外面套着 Scaffold 的 contentPadding（状态栏那 24dp）与底部栏，
     * 于是遮罩的 (0,0) 与 root 的 (0,0) 差着一个竖直偏移。
     * 直接拿 root 坐标去 Canvas 里画，越靠屏幕下方的目标偏得越多 ——
     * 实测最后一步（底部按钮组）的洞整个被推出画布，表现为「最后一步没有高亮」。
     *
     * 让遮罩记下自己的原点、绘制前减掉，两层坐标系就对齐了，
     * 也不用去猜外层到底有多少 padding。
     */
    var selfOrigin by remember { mutableStateOf(Offset.Zero) }

    // 目标矩形（root 坐标）外扩 HALO；没有目标时为 null（整片遮罩、气泡居中）
    val targetInRoot = step.target?.bounds?.inflate(haloPx)
    // 换算到遮罩自身的坐标系
    val holeTarget = targetInRoot?.translate(-selfOrigin.x, -selfOrigin.y)

    /*
     * 四条独立弹簧分别插值 left/top/right/bottom。
     * 用 Rect 整体插值当然也可以，但目标尺寸变化时（不同控件宽高差很多）
     * 四条独立弹簧收敛得更自然，也不会在宽高差异大时出现「一边先到」。
     */
    val spring = Motion.spatial<Float>()
    val rectAnim = remember { RectAnimatable() }
    LaunchedEffect(holeTarget) {
        if (holeTarget != null) rectAnim.animateTo(holeTarget, spring)
    }
    // 首次出现时初值直接给目标值，第一帧不会从 (0,0) 飞过来
    val hole = rectAnim.current(holeTarget)
    val hasHole = hole.width > 0f && hole.height > 0f

    // 主题色必须在组合期读出来：Canvas 的 DrawScope 不是 @Composable 上下文，
    // 在里面调 MaterialTheme.colorScheme 编译不过（即便能过，也拿不到正确的局部值）
    val borderColor = MaterialTheme.colorScheme.primary
    val borderWidth = with(density) { 2.5.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // ---------- 遮罩 + 高亮边框（同一块 Canvas 画，保证两者像素对齐） ----------
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 记下自己在 root 坐标系里的原点，用于把锚点坐标换算进来
                .onGloballyPositioned { selfOrigin = it.boundsInRoot().topLeft }
                // 吞手势：点击与拖动都要拦。只拦点击时，横向拖动会冒到父层那些
                // 挂了 tabSwipe / stageSwipe 的容器上，教程期间就把页面切走了。
                .pointerInput(Unit) { detectTapGestures { } }
                .pointerInput(Unit) { detectDragGestures(onDrag = { _, _ -> }) }
        ) {
            if (hasHole) {
                /*
                 * 外圈减去内圈：EvenOdd 填充规则下，外矩形与内圆角矩形重叠的部分
                 * 会被判成「洞」，于是这片 path 只在洞以外着色。
                 * 内圈要往外扩一个描边宽度，否则边框会被遮罩自己盖住一半。
                 */
                val scrim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addRoundRect(
                        RoundRect(
                            rect = hole,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    )
                }
                drawPath(scrim, ScrimColor)
                // 高亮边框：圈在洞的边缘上，压住遮罩与内容的接缝
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = borderWidth)
                )
            } else {
                // 没有目标：整片遮罩，只留气泡
                drawPath(
                    Path().apply {
                        fillType = PathFillType.NonZero
                        addRect(Rect(Offset.Zero, size))
                    },
                    ScrimColor
                )
            }
        }

        // ---------- 气泡 ----------
        /*
         * 位置规则：洞在屏幕中线以上 → 气泡摆到下面；洞在下 → 摆到上面。
         * 永远不盖住被讲解的东西 —— 教程盖住自己正在讲的那个控件，等于没讲。
         * 没有洞（整体说明）时摆下半屏，视觉重心更稳。
         */
        val bubbleBelow = hole.center.y < screenHeightPx / 2f
        TutorialBubble(
            step = step,
            stepIndex = stepIndex,
            stepCount = stepCount,
            onNext = onNext,
            onSkip = onSkip,
            modifier = Modifier
                .align(if (bubbleBelow) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(horizontal = 20.dp)
                .padding(top = if (bubbleBelow) 0.dp else 40.dp)
                .padding(bottom = if (bubbleBelow) 40.dp else 0.dp)
        )
    }
}

/** 教程气泡：步数 + 标题 + 说明 + 「跳过 / 下一步」 */
@Composable
private fun TutorialBubble(
    step: TutorialStep,
    stepIndex: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stepIndex + 1} / $stepCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = step.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "跳过教程",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    // 最后一步的按钮就是「照着做」的那个动作，文案要跟着变
                    text = if (stepIndex + 1 >= stepCount) "开始背词" else "下一步",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ------------------------------------------------------------------ 小工具

/** 把矩形向外扩 [px]（负值即内缩），宽高同步变化 */
private fun Rect.inflate(px: Float): Rect =
    Rect(left - px, top - px, right + px, bottom + px)

/** 平移矩形（坐标系换算用） */
private fun Rect.translate(dx: Float, dy: Float): Rect =
    Rect(left + dx, top + dy, right + dx, bottom + dy)

/**
 * 四条独立弹簧组成的矩形插值器。
 *
 * 单独抽出来是因为 [TutorialOverlay] 要在组合期用同一组 Animatable 驱动四个边，
 * 写在一个类里比在调用点摊开四个 remember 更容易读，也避免四次
 * `current()` 各自取到不同帧的值（同一帧读四个 edge，画面才不抖）。
 */
private class RectAnimatable {
    private val left = Animatable(0f)
    private val top = Animatable(0f)
    private val right = Animatable(0f)
    private val bottom = Animatable(0f)
    private var initialized = false

    /** 当前值；尚未初始化过时直接返回 [target]（首帧不飞入） */
    fun current(target: Rect?): Rect {
        if (!initialized) return target ?: Rect.Zero
        return Rect(left.value, top.value, right.value, bottom.value)
    }

    suspend fun animateTo(target: Rect, spec: androidx.compose.animation.core.FiniteAnimationSpec<Float>) {
        if (!initialized) {
            // 第一次有目标：直接落位，不播动画
            left.snapTo(target.left)
            top.snapTo(target.top)
            right.snapTo(target.right)
            bottom.snapTo(target.bottom)
            initialized = true
            return
        }
        left.animateTo(target.left, spec)
        top.animateTo(target.top, spec)
        right.animateTo(target.right, spec)
        bottom.animateTo(target.bottom, spec)
    }
}
