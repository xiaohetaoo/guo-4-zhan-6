package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.AchievementDef
import com.hdu.cetvocab.data.DayStat
import com.hdu.cetvocab.ui.theme.LocalAppColors

/** 统一的大圆角卡片 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    radius: Dp = 22.dp,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(radius))
            .padding(padding),
        content = content
    )
}

/** 环形进度 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp,
    strokeWidth: Dp = 13.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** 数据小卡 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/** 近 7 天学习柱状图 */
@Composable
fun WeekBarChart(
    stats: List<DayStat>,
    goal: Int,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    goalColor: Color = LocalAppColors.current.success
) {
    val maxValue = maxOf(goal, stats.maxOfOrNull { it.learned } ?: 0, 1).toFloat()
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { stat ->
                val frac = (stat.learned / maxValue).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (stat.learned > 0) {
                        Text(
                            text = stat.learned.toString(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((78 * frac).dp.coerceAtLeast(5.dp))
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (stat.learned >= goal) goalColor else barColor)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { stat ->
                Text(
                    text = shortDay(stat.day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun shortDay(day: String): String =
    if (day.length >= 10) day.substring(5).replace('-', '/') else day

/** 成就徽章字符映射（不使用图片资源，保证体积与一致性） */
fun glyphChar(glyph: String): String = when (glyph) {
    "seed" -> "芽"
    "sprout" -> "苗"
    "tree" -> "树"
    "mountain" -> "山"
    "fire" -> "火"
    "flag" -> "旗"
    "medal" -> "章"
    "bolt" -> "电"
    "crown" -> "冠"
    "shield" -> "盾"
    "target" -> "靶"
    "star" -> "星"
    "check" -> "✓"
    "moon" -> "月"
    "sun" -> "日"
    else -> "★"
}

@Composable
fun tierColor(tier: Int): Color = when (tier) {
    3 -> LocalAppColors.current.tierGold
    2 -> LocalAppColors.current.tierSilver
    else -> LocalAppColors.current.tierBronze
}

/** 成就徽章 */
@Composable
fun AchievementBadge(
    def: AchievementDef,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tint = if (unlocked) tierColor(def.tier) else LocalAppColors.current.badgeLockedTint
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (unlocked) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (unlocked) tint.copy(alpha = 0.22f) else LocalAppColors.current.badgeLockedBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyphChar(def.glyph),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) tint else LocalAppColors.current.badgeLockedFg
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = def.title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

/** 小标签 */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor
    )
}

/** 暖色卡片底色列表（跟随深浅色主题） */
val warmCards: List<Color>
    @Composable get() = with(LocalAppColors.current) {
        listOf(cardPeach, cardAmber, cardRose, cardMint, cardSky)
    }
