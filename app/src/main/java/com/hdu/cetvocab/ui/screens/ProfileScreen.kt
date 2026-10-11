package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.Achievements
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.StatTile
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.WeekBarChart
import com.hdu.cetvocab.ui.theme.LocalAppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenAchievements: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by vm.profile.collectAsState()
    val ov = state.overview
    val unlockedCount = state.unlocked.size

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "当前阶段：${WordDb.stageLabel(state.stage)} · 每日目标 ${state.goal} 词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconBox { onOpenSettings() }
            }
        }

        // ---------- 连续打卡主卡 ----------
        item {
            AppCard(color = LocalAppColors.current.cardRose, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${ov.streakDays}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "连续天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (ov.streakDays >= 10) "已连续打卡 ${ov.streakDays} 天 🔥"
                            else "连续打卡 ${ov.streakDays} 天",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "历史最长连续 ${ov.bestStreak} 天 · 累计学习 ${ov.totalMinutes} 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(text = "已学 ${ov.totalLearned} 词")
                            Tag(
                                text = "掌握 ${ov.mastered} 词",
                                container = LocalAppColors.current.cardMint,
                                contentColor = LocalAppColors.current.success
                            )
                        }
                    }
                }
            }
        }

        // ---------- 统计 ----------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "累计正确率",
                    value = "${ov.accuracy}",
                    unit = "%",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardSky,
                    tint = MaterialTheme.colorScheme.primary
                )
                StatTile(
                    label = "闯关次数",
                    value = "${ov.quizPlayed}",
                    unit = "次",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardAmber,
                    tint = LocalAppColors.current.warning
                )
                StatTile(
                    label = "最高分",
                    value = "${ov.bestScore}",
                    modifier = Modifier.weight(1f),
                    container = LocalAppColors.current.cardPeach,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------- 近 14 天 ----------
        item {
            AppCard {
                SectionTitle(text = "近 14 天学习量")
                Spacer(Modifier.height(14.dp))
                WeekBarChart(stats = state.weekStats, goal = state.goal)
            }
        }

        // ---------- 成就 ----------
        item {
            AppCard {
                SectionTitle(
                    text = "成就（$unlockedCount / ${Achievements.ALL.size}）",
                    accent = LocalAppColors.current.gold,
                    trailing = {
                        Text(
                            text = "全部 ›",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenAchievements() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Achievements.ALL.take(4).forEach { def ->
                        AchievementBadge(
                            def = def,
                            unlocked = state.unlocked.containsKey(def.code),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---------- 入口 ----------
        item {
            AppCard {
                MenuRow("历次成绩排行榜", "查看每一次闯关的得分与排名", LocalAppColors.current.cardAmber, onOpenLeaderboard)
                Spacer(Modifier.height(10.dp))
                MenuRow("成就墙", "解锁 $unlockedCount 个成就，共 ${Achievements.ALL.size} 个", LocalAppColors.current.cardMint, onOpenAchievements)
                Spacer(Modifier.height(10.dp))
                MenuRow("设置", "每日目标、发音、数据管理", LocalAppColors.current.cardSky, onOpenSettings)
            }
        }

        item {
            AppCard(color = MaterialTheme.colorScheme.surfaceVariant, padding = 16.dp) {
                Text(
                    text = "本应用完全离线运行：词库、例句与助记内容全部内置，" +
                        "发音使用系统语音引擎，未申请网络权限，学习数据仅保存在本机。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IconBox(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GearIcon(
            modifier = Modifier.size(21.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 自绘齿轮图标。
 *
 * 原先这里用的是 Unicode 字符「⚙」，但该字形的具体长相取决于系统字体——
 * 在多数 Android 字体里它被画成「圆环 + 一圈小尖刺」，看着像船舵或太阳而不是齿轮。
 * 改为按几何关系手绘：8 个梯形齿 + 中心圆孔，形状稳定且与矢量图标一致。
 */
@Composable
private fun GearIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension / 2f      // 齿顶圆半径
        val bodyR = outerR * 0.76f               // 齿根圆半径
        val holeR = outerR * 0.30f               // 中心孔半径
        val teeth = 8
        val step = (2.0 * PI / teeth).toFloat()  // 每个齿占的圆心角
        val topHalf = step * 0.17f               // 齿顶半角
        val rootHalf = step * 0.30f              // 齿根半角

        fun polar(r: Float, a: Float) = Offset(cx + r * cos(a), cy + r * sin(a))
        fun deg(a: Float) = (a * 180.0 / PI).toFloat()

        val body = Rect(cx - bodyR, cy - bodyR, cx + bodyR, cy + bodyR)

        val path = Path().apply {
            // 外轮廓与中心孔互不重叠，用 EvenOdd 即可把孔挖空
            fillType = PathFillType.EvenOdd
            for (i in 0 until teeth) {
                val c = i * step
                val p1 = polar(bodyR, c - rootHalf)
                val p2 = polar(outerR, c - topHalf)
                val p3 = polar(outerR, c + topHalf)
                val p4 = polar(bodyR, c + rootHalf)
                if (i == 0) moveTo(p1.x, p1.y) else lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                // 齿根用圆弧连到下一个齿，比直线更接近真实齿轮
                arcTo(
                    rect = body,
                    startAngleDegrees = deg(c + rootHalf),
                    sweepAngleDegrees = deg(step - 2f * rootHalf),
                    forceMoveTo = false
                )
            }
            close()
            addOval(Rect(cx - holeR, cy - holeR, cx + holeR, cy + holeR))
        }
        drawPath(path, color)
    }
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
