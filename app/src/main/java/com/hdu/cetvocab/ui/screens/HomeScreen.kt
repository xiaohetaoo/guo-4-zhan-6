package com.hdu.cetvocab.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.ProgressRing
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.StatTile
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.WeekBarChart
import com.hdu.cetvocab.ui.theme.LocalAppColors
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartToday: () -> Unit
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
    val stageAnim = remember { Animatable(1f) }
    val seenStage = remember { mutableIntStateOf(state.stage) }
    LaunchedEffect(state.stage) {
        if (state.stage != seenStage.intValue) {
            seenStage.intValue = state.stage
            stageAnim.snapTo(0f)
            stageAnim.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
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
            AppCard(color = LocalAppColors.current.cardPeach, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = progress, diameter = 116.dp, strokeWidth = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ov.todayLearned}",
                                fontSize = 30.sp,
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
                    Spacer(Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (progress >= 1f) "今日目标已达成 🎉" else "今日学习目标",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "待复习 ${state.dueCount} 词 · 队列 ${state.queueCount} 词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onStartToday,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.queueCount == 0) "今日已完成" else "开始今日学习",
                                fontWeight = FontWeight.SemiBold
                            )
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
                    label = "已掌握",
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
                    Tag(text = "日均 ${
                        if (state.weekStats.isEmpty()) 0
                        else state.weekStats.sumOf { it.learned } / state.weekStats.size
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

@Composable
private fun StageSwitch(stage: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        listOf(WordDb.STAGE_CET4 to "四级", WordDb.STAGE_CET6 to "六级").forEach { (s, label) ->
            val selected = s == stage
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { if (!selected) onSelect(s) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h in 5..10 -> "早上好，今天也要加油"
        h in 11..13 -> "中午好，利用碎片时间记几个词"
        h in 14..17 -> "下午好，来一组闯关吧"
        h in 18..22 -> "晚上好，完成今天的目标吧"
        else -> "夜深了，注意休息"
    }
}
