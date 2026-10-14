package com.hdu.cetvocab.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.IconSparkle
import com.hdu.cetvocab.ui.components.ProgressRing
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.StatTile
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.WeekBarChart
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.Motion
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartToday: () -> Unit,
    onStartExtra: () -> Unit
) {
    val state by vm.home.collectAsState()
    val ov = state.overview
    val progress = if (state.goal <= 0) 0f
    else (ov.todayLearned.toFloat() / state.goal).coerceIn(0f, 1f)
    // 目前进度：当前级别已学单词 ÷ 本级总词数
    val stageProgressPct = if (state.totalWords <= 0) 0
    else (state.stageLearned * 100.0 / state.totalWords).roundToInt()

    // 四六级切换时整页重放一次「淡入 + 轻微上滑」。
    // 只认「级别真的变了」才播：首次进入首页、从别的 tab 切回来都不重放，避免无谓的闪动。
    // 这里用弹簧而不是固定时长缓动：切换级别是个可以被打断的操作
    // （用户可能在动画没走完时又点回四级），弹簧能自然接管而不是重新起播。
    val stageAnim = remember { Animatable(1f) }
    val seenStage = remember { mutableIntStateOf(state.stage) }
    LaunchedEffect(state.stage) {
        if (state.stage != seenStage.intValue) {
            seenStage.intValue = state.stage
            stageAnim.snapTo(0f)
            // 透明度不能回弹（会闪），所以整段用临界阻尼弹簧，只求收束干脆。
            stageAnim.animateTo(1f, Motion.effects())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = stageAnim.value
                translationY = (1f - stageAnim.value) * 28.dp.toPx()
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "过四斩六",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StageSwitch(
                    stage = state.stage,
                    onSelect = { vm.setStage(it) }
                )
            }
        }

        // ---------- 今日进度 ----------
        item {
            /*
             * 版式：圆环 + 文字并排在上半，**操作按钮独占整行宽度沉到卡片底部**。
             *
             * 此前按钮和圆环挤在同一个 Row 的右列里：圆环 116dp + 间距 18dp 之后，
             * 右列只剩约 110dp，两个按钮各分一半、再各扣掉 Material 按钮默认的
             * 24dp 左右内边距，留给文字的宽度只剩几个 dp —— 结果「今日已完成」
             * 被竖排成一列一个字，「开始今日学习」也会折成两行。
             * 按钮要好看，前提是给它整行的宽度，所以把它移出这个 Row。
             */
            AppCard(color = LocalAppColors.current.cardPeach, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = progress, diameter = 104.dp, strokeWidth = 11.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ov.todayLearned}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/ ${state.goal} 词",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (progress >= 1f) "今日目标已达成" else "今日学习目标",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (progress >= 1f) {
                                Spacer(Modifier.width(4.dp))
                                IconSparkle(
                                    modifier = Modifier.size(16.dp),
                                    color = LocalAppColors.current.gold
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // 三个数字各自为政地摆在一起会读成噪音（0 词尤其像坏掉了），
                        // 按实际情况只说当前最该知道的那件事。
                        Text(
                            text = when {
                                state.dueCount > 0 ->
                                    "待复习 ${state.dueCount} 词 · 队列 ${state.queueCount} 词"
                                state.queueCount > 0 ->
                                    "复习已还清 · 队列 ${state.queueCount} 词"
                                progress >= 1f ->
                                    "复习已还清 · 今日额度用满"
                                // 队列为空却还没达标，只可能是本级词已全部学过
                                // （todayQueue = 到期词 + 补足到目标的新词）。
                                // 这时不能说「今日额度用满」—— 今日一个都没学。
                                else ->
                                    "本级 ${state.totalWords} 词都已学过 · 可切换别的级别"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                // 队列为空时按钮必须禁用：此前只把文案换成「今日已完成」，
                // 点击仍然会走 onStartToday —— startTodayStudy 拿到空队列后
                // 只是弹一句提示，但导航是无条件执行的，用户会被带进一个空的学习页。
                val canStart = state.queueCount > 0
                val goalReached = progress >= 1f
                if (canStart) {
                    Button(
                        onClick = onStartToday,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "开始今日学习", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    /*
                     * 目标达成态：不再摆一个禁用的假按钮占位（灰底 + 竖排文字既难看
                     * 又容易被当成可点），状态改用一行 Tag 说清楚，动作只留一个整行的
                     * 「继续学习」—— 走 extraQueue（到期复习 + 预学一批新词），
                     * 完成目标后仍可加量。预学词同样计入学习记录，只是当日进度已经
                     * 按 todayLearned/goal 截断到 1f，不会溢出。
                     */
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tag(
                            text = if (goalReached) "今日已完成" else "本级已学完",
                            container = LocalAppColors.current.cardMint,
                            contentColor = LocalAppColors.current.success
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (goalReached) "想再学一批，可以提前学下一组新词"
                            else "本级没有可学的新词了，可切换另一个级别",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 「继续学习」走的是 extraQueue = 到期词 + 预学新词。
                    // 队列空又没达标时，到期词必然为 0、本级新词也必然为 0
                    // （否则 todayQueue 就能凑出词来），extraQueue 同样必然是空 ——
                    // 这时候摆一个整行的主按钮，点下去只弹一句「这里暂时没有需要学习的单词」，
                    // 是个骗人的按钮，直接不渲染。
                    if (goalReached) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onStartExtra,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(text = "继续学习", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ---------- 数据概览 ----------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "累计学习",
                    value = "${ov.totalLearned}",
                    unit = "词",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardSky,
                    tint = MaterialTheme.colorScheme.primary
                )
                StatTile(
                    // 与 levelLabel 对齐：这个数是按 `level >= 3`（熟悉）统计的，
                    // 而 levelLabel 第 4 档才叫「掌握」，统计位统一写「熟悉以上」。
                    label = "熟悉以上",
                    value = "${ov.mastered}",
                    unit = "词",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardMint,
                    tint = LocalAppColors.current.success
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "连续打卡",
                    value = "${ov.streakDays}",
                    unit = "天",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardAmber,
                    tint = LocalAppColors.current.warning
                )
                StatTile(
                    label = "目前进度",
                    value = "$stageProgressPct",
                    unit = "%",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardPeach,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------- 近 7 天 ----------
        item {
            AppCard {
                SectionTitle(text = "近 7 天学习量")
                Spacer(Modifier.height(14.dp))
                WeekBarChart(stats = state.weekStats, goal = state.goal)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 四舍五入而不是整除截断：日均 4.86 词此前显示成「4 词」
                    Tag(text = "日均 ${
                        if (state.weekStats.isEmpty()) 0
                        else (state.weekStats.sumOf { it.learned }.toFloat() /
                            state.weekStats.size).roundToInt()
                    } 词")
                    Spacer(Modifier.width(8.dp))
                    Tag(text = "最长连续 ${ov.bestStreak} 天")
                }
            }
        }

        // 注：原「最近闯关成绩」「快捷入口」两个区块已并入底部导航与「我的」页，
        //     避免在首页与底部 tab / 个人页之间出现重复入口。
    }
}

/**
 * 四六级分段控件的左右滑动手势：向左滑切六级，向右滑切四级。
 *
 * 方向与控件里的位置一致 —— 四级在左格、六级在右格，向左滑（手指把内容往左推）
 * 露出右格，也就是六级。
 *
 * 阈值取 36dp 而不是底部导航那个 72dp：这个控件只有 ~110dp 宽，
 * 沿用 72dp 的话在这么窄的范围里根本滑不动。36dp 约为控件宽的 1/3，
 * 既滑得动又不至于点一下就误切。
 */
private fun Modifier.stageSwipe(stage: Int, onSelect: (Int) -> Unit): Modifier =
    pointerInput(stage) {
        val threshold = 36.dp.toPx()
        var total = 0f
        var fired = false
        fun reset() { total = 0f; fired = false }
        detectHorizontalDragGestures(
            onDragStart = { reset() },
            onDragEnd = { reset() },
            onDragCancel = { reset() },
            onHorizontalDrag = { _, dragAmount ->
                if (fired) return@detectHorizontalDragGestures
                total += dragAmount
                val target = when {
                    total <= -threshold -> WordDb.STAGE_CET6  // 向左滑 → 右格（六级）
                    total >= threshold -> WordDb.STAGE_CET4   // 向右滑 → 左格（四级）
                    else -> 0
                }
                if (target != 0 && target != stage) {
                    fired = true
                    onSelect(target)
                }
            }
        )
    }

/**
 * 四六级分段控件。
 *
 * 原实现是「选中项瞬间换底色」——手指抬起的同一帧颜色就跳过去了，
 * 没有任何过渡，这是最典型的「生硬」。改成一条**弹簧滑动的指示器**：
 * 底色块在两级之间滑过去，文字颜色同步做颜色过渡。
 *
 * 注意两个子项用**固定宽度**而不是 `weight(1f)`：Row 在未限定宽度时，
 * 带权重的子项会吃掉父级给的全部可用宽度，整个控件会被拉成满屏宽，
 * 把同一行左侧的问候语挤成 0 宽（实测过，问候语直接消失）。
 * 这里两级标签都是两字，固定 52dp 一格，跟随字号放大也放得下。
 */
@Composable
private fun StageSwitch(stage: Int, onSelect: (Int) -> Unit) {
    val items = listOf(WordDb.STAGE_CET4 to "四级", WordDb.STAGE_CET6 to "六级")
    val selectedIndex = items.indexOfFirst { it.first == stage }.coerceAtLeast(0)
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = Motion.spatialSnappy(),
        label = "stagePill"
    )
    // 两个子项共用同一个交互源：只为了把水波纹关掉（滑动的色块本身就是反馈，
    // 再叠一层波纹会显脏），不参与按压缩放。
    val interaction = remember { MutableInteractionSource() }
    val itemWidth = 52.dp

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .stageSwipe(stage, onSelect)
    ) {
        // 指示器层：与内容层一样内缩 3dp。
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = itemWidth * position)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        // 内容层也要左右滑动：光有指示器动、文字原地变色，方向感仍然很弱。
        // 这里让两级标签整体朝切换方向滑一次（四级→六级向右、反之向左），
        // 位移取控件自身宽度的一半 —— 比整屏轻，但在 110dp 宽的小控件上已经足够明确。
        // 外层 Box 的 clip 会把滑出部分裁掉，看上去就是内容在控件里横向翻过一格。
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                val forward = targetState > initialState
                if (forward) {
                    (slideInHorizontally(Motion.spatial()) { it / 2 } + fadeIn(Motion.effects()))
                        .togetherWith(
                            slideOutHorizontally(Motion.spatial()) { -it / 2 } +
                                fadeOut(Motion.effects())
                        )
                } else {
                    (slideInHorizontally(Motion.spatial()) { -it / 2 } + fadeIn(Motion.effects()))
                        .togetherWith(
                            slideOutHorizontally(Motion.spatial()) { it / 2 } +
                                fadeOut(Motion.effects())
                        )
                }
            },
            contentAlignment = Alignment.CenterStart,
            label = "stageSwitch"
        ) { currentStage ->
            Row(modifier = Modifier.padding(3.dp)) {
                items.forEach { (s, label) ->
                    val selected = s == currentStage
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = Motion.effects(),
                        label = "stageText"
                    )
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .clip(RoundedCornerShape(13.dp))
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null,
                                onClick = { if (!selected) onSelect(s) }
                            )
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

private fun greeting(): String {
    // 用 java.time.LocalTime 替代 Calendar：不可变、线程安全，语义也更直接
    val hour = java.time.LocalTime.now().hour
    return when {
        hour in 5..10 -> "早上好，今天也要加油"
        hour in 11..13 -> "中午好，利用碎片时间记几个词"
        hour in 14..17 -> "下午好，来一组闯关吧"
        hour in 18..22 -> "晚上好，完成今天的目标吧"
        else -> "夜深了，注意休息"
    }
}
