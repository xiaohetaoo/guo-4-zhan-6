package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 全应用统一的矢量图标库。
 *
 * ## 为什么要自绘
 * 此前图标来源非常杂，一共混了五套体系：
 * 1. Material 图标字体（底部导航）；
 * 2. 自绘 Canvas（齿轮、喇叭）；
 * 3. Unicode 排版符号（`‹` `›` `✓` `✕` `★` `♥` `♡` `·`）；
 * 4. 汉字当图形（成就徽章的「芽/苗/树/山…」）；
 * 5. **彩色 emoji（`🍃` `🏆` `🎉` `💪` `🔥` `✅` `❌`）**。
 *
 * 其中第 3、5 类是真正的问题：
 * - emoji 由系统 emoji 字体渲染，是**彩色位图**，与 App 的暖色扁平风格完全冲突，
 *   且部分精简系统字体没有 emoji 时会渲染成「豆腐块」；
 * - `‹` `›` 本质是**单书名号**而不是箭头，笔画细、体量小，在按钮里显得虚；
 * - 这些字形同样会被 `TextStyle` 的行高影响，居中不稳定（曾导致索引条色块错位）。
 *
 * 因此这里按几何关系统一自绘，笔画一律圆头圆角，颜色全部走主题色，
 * 彻底摆脱字体依赖，任何机型渲染结果一致。
 */
private fun DrawScope.iconStroke(width: Float): Stroke =
    Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** 图标绘制区域取正方形边长，便于用比例坐标 */
private inline fun DrawScope.square(block: DrawScope.(Float) -> Unit) = block(size.minDimension)

/**
 * 给自绘图标挂无障碍描述。
 *
 * [Canvas] 是纯绘制、没有任何内置语义，TalkBack 默认读不出内容。
 * 这里统一约定：传 null（默认）＝纯装饰图形，TalkBack 直接跳过；
 * 只有在「图标本身承载含义、且旁边没有等价文字」时才传描述，
 * 避免在「图标 + 文字」的组合里被重复朗读两遍。
 */
private fun Modifier.iconSemantics(description: String?): Modifier =
    if (description.isNullOrBlank()) this else semantics { contentDescription = description }

/** 生成正多边形星形路径（points 个角） */
private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path {
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = (-90.0 + i * (360.0 / total)) * PI / 180.0
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** 返回箭头（左）。替代原来的单书名号 `‹` */
@Composable
fun IconBack(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val p = Path().apply {
                moveTo(w * 0.60f, w * 0.20f)
                lineTo(w * 0.33f, w * 0.50f)
                lineTo(w * 0.60f, w * 0.80f)
            }
            drawPath(p, color, style = iconStroke(w * 0.15f))
        }
    }
}

/** 行尾箭头（右）。替代原来的单书名号 `›` */
@Composable
fun IconChevronRight(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val p = Path().apply {
                moveTo(w * 0.40f, w * 0.20f)
                lineTo(w * 0.67f, w * 0.50f)
                lineTo(w * 0.40f, w * 0.80f)
            }
            drawPath(p, color, style = iconStroke(w * 0.15f))
        }
    }
}

/** 关闭。替代 `✕` */
@Composable
fun IconClose(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val sw = w * 0.14f
            val a = w * 0.29f
            val b = w * 0.71f
            drawLine(color, Offset(a, a), Offset(b, b), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color, Offset(b, a), Offset(a, b), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

/** 对勾。替代 `✓` / `✅` */
@Composable
fun IconCheck(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val p = Path().apply {
                moveTo(w * 0.23f, w * 0.53f)
                lineTo(w * 0.42f, w * 0.72f)
                lineTo(w * 0.78f, w * 0.29f)
            }
            drawPath(p, color, style = iconStroke(w * 0.16f))
        }
    }
}

/** 星形。替代 `★`（成绩星级、成就兜底字形） */
@Composable
fun IconStar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = true,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val path = starPath(w / 2f, w / 2f, w * 0.48f, w * 0.205f, 5)
            if (filled) drawPath(path, color) else drawPath(path, color, style = iconStroke(w * 0.09f))
        }
    }
}

/** 心形。替代 `♥` / `♡`（生词本收藏） */
@Composable
fun IconHeart(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = false,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val path = Path().apply {
                moveTo(w * 0.50f, w * 0.85f)
                cubicTo(w * 0.08f, w * 0.58f, w * 0.10f, w * 0.18f, w * 0.33f, w * 0.18f)
                cubicTo(w * 0.43f, w * 0.18f, w * 0.50f, w * 0.27f, w * 0.50f, w * 0.33f)
                cubicTo(w * 0.50f, w * 0.27f, w * 0.57f, w * 0.18f, w * 0.67f, w * 0.18f)
                cubicTo(w * 0.90f, w * 0.18f, w * 0.92f, w * 0.58f, w * 0.50f, w * 0.85f)
                close()
            }
            if (filled) drawPath(path, color) else drawPath(path, color, style = iconStroke(w * 0.10f))
        }
    }
}

/** 奖杯。替代 emoji `🏆` */
@Composable
fun IconTrophy(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            // 杯身：上宽下窄的碗
            val cup = Path().apply {
                moveTo(w * 0.28f, w * 0.13f)
                lineTo(w * 0.72f, w * 0.13f)
                lineTo(w * 0.68f, w * 0.38f)
                cubicTo(w * 0.65f, w * 0.55f, w * 0.35f, w * 0.55f, w * 0.32f, w * 0.38f)
                close()
            }
            drawPath(cup, color)
            // 左右把手
            drawArc(
                color = color, startAngle = 90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.13f, w * 0.16f), size = Size(w * 0.24f, w * 0.26f),
                style = iconStroke(w * 0.075f)
            )
            drawArc(
                color = color, startAngle = -90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.63f, w * 0.16f), size = Size(w * 0.24f, w * 0.26f),
                style = iconStroke(w * 0.075f)
            )
            // 杯柄与底座
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.455f, w * 0.53f),
                size = Size(w * 0.09f, w * 0.17f),
                cornerRadius = CornerRadius(w * 0.045f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.30f, w * 0.72f),
                size = Size(w * 0.40f, w * 0.13f),
                cornerRadius = CornerRadius(w * 0.065f)
            )
        }
    }
}

/** 叶子。替代 emoji `🍃`（空状态） */
@Composable
fun IconLeaf(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            // 注意：三次贝塞尔的横向极值由控制点决定（t=0.5 处控制点权重各 0.375），
            // 控制点给得太保守叶片会瘦成一条缝，这里按左右极值 ≈ 0.155 / 0.845 反推控制点。
            val leaf = Path().apply {
                moveTo(w * 0.50f, w * 0.93f)
                cubicTo(w * 0.10f, w * 0.74f, w * -0.02f, w * 0.26f, w * 0.50f, w * 0.07f)
                cubicTo(w * 1.02f, w * 0.26f, w * 0.90f, w * 0.74f, w * 0.50f, w * 0.93f)
                close()
            }
            drawPath(leaf, color, style = iconStroke(w * 0.085f))
            // 主叶脉
            drawLine(
                color = color,
                start = Offset(w * 0.50f, w * 0.90f),
                end = Offset(w * 0.50f, w * 0.12f),
                strokeWidth = w * 0.055f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** 火苗。替代 emoji `🔥`（连续打卡） */
@Composable
fun IconFlame(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val flame = Path().apply {
                moveTo(w * 0.50f, w * 0.07f)
                cubicTo(w * 0.60f, w * 0.26f, w * 0.82f, w * 0.42f, w * 0.82f, w * 0.62f)
                cubicTo(w * 0.82f, w * 0.81f, w * 0.68f, w * 0.93f, w * 0.50f, w * 0.93f)
                cubicTo(w * 0.32f, w * 0.93f, w * 0.18f, w * 0.81f, w * 0.18f, w * 0.62f)
                cubicTo(w * 0.18f, w * 0.50f, w * 0.24f, w * 0.42f, w * 0.30f, w * 0.34f)
                cubicTo(w * 0.32f, w * 0.42f, w * 0.37f, w * 0.45f, w * 0.41f, w * 0.42f)
                cubicTo(w * 0.45f, w * 0.38f, w * 0.45f, w * 0.24f, w * 0.50f, w * 0.07f)
                close()
            }
            drawPath(flame, color)
        }
    }
}

/** 向上箭头。替代 emoji `💪`（继续加油） */
@Composable
fun IconArrowUp(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            val sw = w * 0.15f
            drawLine(
                color = color,
                start = Offset(w * 0.50f, w * 0.82f),
                end = Offset(w * 0.50f, w * 0.24f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            val head = Path().apply {
                moveTo(w * 0.26f, w * 0.48f)
                lineTo(w * 0.50f, w * 0.24f)
                lineTo(w * 0.74f, w * 0.48f)
            }
            drawPath(head, color, style = iconStroke(sw))
        }
    }
}

/** 四角星芒。替代 emoji `🎉`（目标达成等轻量庆祝） */
@Composable
fun IconSparkle(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        square { w ->
            drawPath(starPath(w * 0.46f, w * 0.46f, w * 0.34f, w * 0.115f, 4), color)
            drawPath(starPath(w * 0.83f, w * 0.80f, w * 0.16f, w * 0.055f, 4), color)
            drawPath(starPath(w * 0.84f, w * 0.20f, w * 0.12f, w * 0.042f, 4), color)
        }
    }
}

/**
 * 列表项目符号。
 *
 * 原先用字符 `·`（U+00B7）当项目符号：它的行高继承自正文样式，
 * 而同一行的正文用的是更小行高，两者基线不一致，圆点会偏下。
 * 自绘小圆点后与首行文字稳定对齐。
 */
@Composable
fun IconDot(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Canvas(modifier.iconSemantics(contentDescription)) {
        drawCircle(color = color, radius = size.minDimension * 0.5f)
    }
}

