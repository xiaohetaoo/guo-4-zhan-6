package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.QuizRecord
import com.hdu.cetvocab.data.quizScopeLabel
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.AppTopBar
import com.hdu.cetvocab.ui.components.EmptyHint
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.StatTile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun LeaderboardScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.profile.collectAsState()
    val list = state.leaderboard

    LaunchedEffect(Unit) { vm.refreshLeaderboard() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "历次成绩排行榜",
            // 用真实总次数，而不是 list.size：
            // 数据源 quizRecords(50) 最多返回 50 条，超过 50 次后「共 N 次」就不准了。
            subtitle = "共 ${state.quizPlayedCount} 次闯关记录 · 按得分从高到低",
            onBack = onBack
        )

        if (list.isEmpty()) {
            EmptyHint("还没有闯关成绩", hint = "去「闯关」页完成一局，成绩会自动记录在这里")
            return
        }

        val best = list.maxOf { it.score }
        val avg = list.sumOf { it.score } / list.size
        val bestAccuracy = list.maxOf { it.accuracy }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "历史最高分",
                        value = "$best",
                        modifier = Modifier.weight(1f),
                        container = LocalAppColors.current.cardAmber,
                        tint = LocalAppColors.current.warning
                    )
                    StatTile(
                        label = "平均分",
                        value = "$avg",
                        modifier = Modifier.weight(1f),
                        container = LocalAppColors.current.cardPeach,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    StatTile(
                        label = "最高正确率",
                        value = "$bestAccuracy",
                        unit = "%",
                        modifier = Modifier.weight(1f),
                        container = LocalAppColors.current.cardMint,
                        tint = LocalAppColors.current.success
                    )
                }
            }

            item {
                AppCard(padding = 16.dp) {
                    SectionTitle(text = "榜单（个人历次成绩）", accent = LocalAppColors.current.gold)
                }
            }

            itemsIndexed(list, key = { _, r -> r.id }) { index, r ->
                RankRow(rank = index + 1, record = r)
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, record: QuizRecord) {
    val medalColor = when (rank) {
        1 -> LocalAppColors.current.tierGold
        2 -> LocalAppColors.current.tierSilver
        3 -> LocalAppColors.current.tierBronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    // 前三名的底色是金属色（金 / 银 / 铜），明度偏高。
    // 此前统一用纯白文字，白压金约 2.1:1、压银约 2.4:1，都低于 WCAG AA 的 4.5:1。
    // 这里按底色的相对亮度自动选深色或浅色文字，深浅两套主题都成立。
    val onMedal = when {
        rank > 3 -> MaterialTheme.colorScheme.onSurfaceVariant
        medalColor.luminance() > 0.45f -> Color(0xFF3A2A20)
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(medalColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onMedal
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quizScopeLabel(record.stage, record.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "正确 ${record.correct}/${record.total} · 正确率 ${record.accuracy}% · " +
                    "连击 ${record.maxCombo} · ${formatTs(record.ts)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${record.score}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${record.durationMs / 1000} 秒",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 时间戳格式化。
 *
 * 用 [DateTimeFormatter]（不可变、线程安全）替代 `SimpleDateFormat`：
 * 后者是可变对象，一旦被共享（或将来有人把它提成字段缓存）就会在并发下产生乱码时间。
 * 这里保留每次新建其实也是安全的，但统一到 java.time 后没有这个隐患。
 */
private val TS_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

private fun formatTs(ts: Long): String = TS_FORMATTER.format(Instant.ofEpochMilli(ts))
