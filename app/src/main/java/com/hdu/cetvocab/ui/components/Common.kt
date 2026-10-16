package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.AchievementDef
import com.hdu.cetvocab.data.DayStat
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.springPress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    // 徽章字形统一用汉字：原先这里是「✓」和「★」两个排版符号，
    // 与周围汉字字形风格不一致（笔画细、体量小），改为「达 / 星」。
    "check" -> "达"
    "moon" -> "月"
    "sun" -> "日"
    // 专题关卡徽章：辨析用「辨」、翻译用「笔译」的「译」、跨专题收尾用「关」
    "eye" -> "辨"
    "pen" -> "译"
    "gate" -> "关"
    else -> "星"
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
    // 徽章有按压回弹：点下去整枚缩到 94%，松手弹回，替代偏重的默认水波纹。
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .springPress(interaction)
            .clip(RoundedCornerShape(18.dp))
            .background(if (unlocked) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                else Modifier
            )
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
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 成就详情弹层：点成就墙或「我的」页上的任意徽章弹出。
 *
 * 之所以需要它：成就墙是三列网格，每格只有约 94dp 宽，
 * 「作答 200 题以上且正确率达到 98%」这类长说明被压成两行加省略号，
 * 用户看得到徽章却看不到完整的达成条件，也看不到自己是什么时候解锁的。
 * 这里给的是同一份数据的完整版：档位、完整条件、解锁时间（或「尚未解锁」）。
 */
@Composable
fun AchievementDetailDialog(def: AchievementDef, unlockedTs: Long?, onDismiss: () -> Unit) {
    val unlocked = unlockedTs != null
    val tint = if (unlocked) tierColor(def.tier) else LocalAppColors.current.badgeLockedTint
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) tint.copy(alpha = 0.22f)
                            else LocalAppColors.current.badgeLockedBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = glyphChar(def.glyph),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) tint else LocalAppColors.current.badgeLockedFg
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (def.tier) { 3 -> "金色成就" 2 -> "银色成就" else -> "铜色成就" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) tint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        title = {
            Text(
                text = def.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "达成条件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                // 这里不限行数：弹层的全部意义就是把网格里被省略号裁掉的那句说完整
                Text(
                    text = def.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (unlocked) "解锁时间" else "当前状态",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = unlockedTs?.let { ts ->
                        // 时间格式化失败（极端时钟值、ROM 缺时区数据）不该让弹层崩掉，
                        // 退化成一句「已解锁」，状态本身还是对的
                        runCatching {
                            Instant.ofEpochMilli(ts)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
                        }.getOrDefault("已解锁")
                    } ?: "尚未解锁，达成后会自动记录在这里",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unlocked) LocalAppColors.current.success
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

/**
 * 通用入口行：左侧小色点 + 标题 / 副标题 + 右侧箭头。
 *
 * 原来这是「我的」页内部的私有实现，排行榜入口搬到闯关页后两处都要用，
 * 因此提到公共组件里，保证两页的行高、圆角、箭头尺寸完全一致。
 */
@Composable
fun MenuRow(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .springPress(interaction)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(12.dp))
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
        IconChevronRight(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 专题练习入口卡片：标题 + 说明 + 右侧按钮的三段式结构。
 *
 * 原先这是闯关页的私有实现（易混词辨析 / 真题句子翻译两张卡）。
 * 两个专题搬到「我的」页的拓展资源练习区后，闯关页不再需要它，
 * 但「我的」页还要用同一套版式，因此提到公共组件里，
 * 保证两处的行高、圆角、按钮尺寸完全一致。
 *
 * [actionLabel] 由调用方传入：闯关页用「开始挑战」，
 * 拓展练习区用「开始练习」，避免在组件内部写死文案。
 */
@Composable
fun TopicCard(
    title: String,
    desc: String,
    container: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    AppCard(color = container, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onClick) { Text(actionLabel) }
        }
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

/**
 * 圆点项目符号行。
 *
 * 原先用字符 `·` 当项目符号，但那个字符用的是默认正文样式（15sp / 行高 23sp），
 * 而同一行的正文是 bodySmall（12sp / 行高 18sp）——两套行高不同、基线不一致，
 * 实测圆点会明显沉到基线附近（偏低约 4dp）。
 *
 * 这里改成自绘小圆点，并按正文行高把它压到**首行文字的光学中心**：
 * 行高经 `toDp()` 换算，会跟随系统字体缩放一起变大，不会因用户调大字号而错位。
 *
 * 注意：`TextStyle.lineHeight` 可能是 `TextUnit.Unspecified`（例如调用方传入了
 * 只设了字号、没设行高的自定义样式），此时 `toDp()` 会得到 NaN，圆点会被
 * 摆到错误位置。这里做两级兜底：行高 → 字号 × 1.4 → 固定 18dp。
 */
@Composable
fun BulletRow(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodySmall
) {
    val dotSize = 5.dp
    val lineDp = with(LocalDensity.current) {
        val lineHeight = style.lineHeight
        val fontSize = style.fontSize
        when {
            lineHeight != TextUnit.Unspecified && lineHeight.value > 0f -> lineHeight.toDp()
            fontSize != TextUnit.Unspecified && fontSize.value > 0f -> (fontSize * 1.4f).toDp()
            else -> 18.dp
        }
    }
    val dotTop = ((lineDp - dotSize) / 2).coerceAtLeast(0.dp)
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        IconDot(
            modifier = Modifier
                .padding(top = dotTop, end = 8.dp)
                .size(dotSize),
            color = dotColor
        )
        Text(text = text, style = style, color = textColor)
    }
}
