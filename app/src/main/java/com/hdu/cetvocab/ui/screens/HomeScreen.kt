package com.hdu.cetvocab.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartToday: () -> Unit,
    onOpenUnit: (Int) -> Unit,
    onOpenLearn: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenWord: (Int) -> Unit
) {
    val state by vm.home.collectAsState()
    val ov = state.overview
    val progress = if (state.goal <= 0) 0f
    else (ov.todayLearned.toFloat() / state.goal).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
                    label = "累计正确率",
                    value = "${ov.accuracy}",
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

        // ---------- 最近闯关 ----------
        item {
            AppCard {
                SectionTitle(
                    text = "最近闯关成绩",
                    trailing = {
                        Text(
                            text = "排行榜 ›",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenLeaderboard() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                if (state.recentQuizzes.isEmpty()) {
                    Text(
                        text = "还没有闯关记录，去「闯关」挑战一下吧",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    state.recentQuizzes.forEach { r ->
                        QuizRecordRow(r)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ---------- 快捷入口 ----------
        item {
            AppCard {
                SectionTitle(text = "快捷入口")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("词库", "按单元学", LocalAppColors.current.cardMint, Modifier.weight(1f), onOpenLearn)
                    QuickAction("闯关", "单元测验", LocalAppColors.current.cardAmber, Modifier.weight(1f), onOpenQuiz)
                    QuickAction("成就", "已解锁 ${ov.quizPlayed} 次闯关", LocalAppColors.current.cardSky, Modifier.weight(1f), onOpenAchievements)
                }
            }
        }
    }
}

@Composable
private fun QuizRecordRow(r: QuizRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${WordDb.stageLabel(r.stage)} · 第 ${r.unit} 单元",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "正确 ${r.correct}/${r.total} · 正确率 ${r.accuracy}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${r.score}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "分",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
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
