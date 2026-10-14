package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.Achievements
import com.hdu.cetvocab.data.Tier
import com.hdu.cetvocab.data.UNIT_QUIZ_COUNT
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.IconChevronRight
import com.hdu.cetvocab.ui.components.IconFlame
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.TopicCard
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.springPress
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConfusable: () -> Unit,
    onOpenTranslation: () -> Unit
) {
    val state by vm.profile.collectAsState()
    val ov = state.overview
    // 只数「当前版本定义过」的成就码：库里若混入未知 code（手改过的备份），
    // 直接取 size 会显示成「成就（100 / 81）」
    val unlockedCount = Achievements.ALL.count { state.unlocked.containsKey(it.code) }

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
        // 之前段位塞在同一张卡最右侧，把中间"标题 + Tag 行"挤得只剩 ~150dp，
        // 标题里挤进 5+ 汉字即换行（「连续打卡 0 天」被拆成「连续打卡 0」与
        // 「天」两行），Tag 行也偶发渲染成竖条状。这里把连续打卡与段位拆成
        // 两张独立卡片，整页节奏与首页的「学习 / 成就」更接近。
        item {
            AppCard(color = LocalAppColors.current.cardRose, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${ov.streakDays}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            // 原为「连续天」三个字，是「连续打卡 N 天」被截断后的残句，
                            // 而同一行右边已经把完整句子写出来了。
                            text = "连续打卡",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // maxLines = 1：过去被 TierBadge 挤到宽度不足时，
                            // 长标题会在此换行，让「连续打卡 X 天」碎成两行。
                            Text(
                                text = "连续打卡 ${ov.streakDays} 天",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (ov.streakDays >= 10) {
                                Spacer(Modifier.width(5.dp))
                                IconFlame(
                                    modifier = Modifier.size(15.dp),
                                    color = LocalAppColors.current.fire
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "历史最长连续 ${ov.bestStreak} 天 · " +
                                "累计学习 ${ov.totalMinutes} 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(text = "已学 ${ov.totalLearned} 词")
                            // 统计口径写「熟悉以上」：这一档按 level >= 3（熟悉）起算，
                            // 而 levelLabel 的第 4 档才叫「掌握」，写「掌握 N 词」会和等级标签打架。
                            Tag(
                                text = "熟悉以上 ${ov.mastered} 词",
                                container = LocalAppColors.current.cardMint,
                                contentColor = LocalAppColors.current.success
                            )
                        }
                    }
                }
            }
        }

        // ---------- 段位独立卡 ----------
        item {
            TierCard(learnedWords = ov.totalLearned)
        }

        // ---------- 成就 ----------
        item {
            AppCard {
                SectionTitle(
                    text = "成就（$unlockedCount / ${Achievements.ALL.size}）",
                    accent = LocalAppColors.current.gold,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { onOpenAchievements() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "全部",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconChevronRight(
                                modifier = Modifier
                                    .padding(start = 1.dp)
                                    .size(13.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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

        // ---------- 拓展资源练习 ----------
        // 「易混词辨析」与「真题句子翻译」原先挂在闯关页。两者都是专项巩固性质，
        // 且不参与排行榜排名，和「闯关」的定位并不一致，因此挪到成就卡下方，
        // 归入「拓展资源练习」——成就与拓展练习放在一起，也符合「练成就」的动线。
        item {
            Column {
                // 不传 accent：用主题默认主色。原先想用 cardSky，
                // 但那是 #EAF1FA 的极淡蓝，当 4dp 色条压在暖色背景上几乎看不见。
                SectionTitle(text = stringResource(R.string.profile_extension_title))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.profile_extension_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            TopicCard(
                title = stringResource(R.string.quiz_topic_confusable),
                desc = stringResource(R.string.quiz_topic_confusable_desc, UNIT_QUIZ_COUNT),
                container = LocalAppColors.current.cardSky,
                actionLabel = stringResource(R.string.action_start_practice),
                onClick = onOpenConfusable
            )
        }

        item {
            TopicCard(
                title = stringResource(R.string.quiz_topic_translation),
                desc = stringResource(R.string.quiz_topic_translation_desc, UNIT_QUIZ_COUNT),
                container = LocalAppColors.current.cardMint,
                actionLabel = stringResource(R.string.action_start_practice),
                onClick = onOpenTranslation
            )
        }

        // ---------- 入口 ----------
        // 「历次成绩排行榜」与「设置」两个入口都不再单列在「我的」页：
        // 排行榜移到闯关页（成绩本就是在闯关里打出来的，放在这里要绕一次），
        // 设置保留在页面右上角的齿轮图标。重复入口会让这一页越来越长。
        item {
            Text(
                text = "本应用完全离线运行：词库、例句与联想记忆内容全部内置，" +
                    "发音使用系统语音引擎，未申请网络权限，学习数据仅保存在本机。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun IconBox(onClick: () -> Unit) {
    // 齿轮是 Canvas 自绘、没有任何语义，必须显式给出朗读文本，
    // 否则 TalkBack 只会念出一个无名的「按钮」。
    val settingsLabel = stringResource(R.string.title_settings)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .springPress(interaction, pressedScale = 0.88f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = settingsLabel },
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

/**
 * 段位卡片：与"连续打卡主卡"分开独立成一张卡片。
 *
 * 之前段位挤在主卡右上、把中间列挤窄，导致「连续打卡 X 天」标题换行、
 * Tag 行偶发渲染成竖条样块。把两件事拆开后：
 * - 主卡专心展示连续打卡，圆环 + 标题 + 副文 + Tag 三件套都宽裕，不会再换行；
 * - 段位卡独占一张，更接近成就卡的视觉节奏，方便日后加"段位历史"等扩展内容。
 *
 * 内容：
 * - 左侧：段位大字（30sp）+ "已学 X 词" 副文；
 * - 右侧：距下一段位还差多少词，配一条 120dp 的细进度条让进度可视化。
 */
@Composable
private fun TierCard(learnedWords: Int) {
    val tier = Tier.of(learnedWords)
    val color = tierColor(tier)
    val next = tier.next
    val progress = tier.progressToNext(learnedWords)
    AppCard(color = color.copy(alpha = 0.10f), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.label,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "已学 $learnedWords 词",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                if (next != null) {
                    Text(
                        text = "距 ${next.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "还差 ${tier.wordsToNext(learnedWords)} 词",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = color,
                        trackColor = color.copy(alpha = 0.18f),
                        modifier = Modifier
                            .width(120.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    Text(
                        text = "已达最高段位",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        color = color,
                        trackColor = color.copy(alpha = 0.18f),
                        modifier = Modifier
                            .width(120.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

/** 段位配色（青铜→王者依次递进） */
private fun tierColor(tier: Tier): Color = when (tier) {
    Tier.BRONZE -> Color(0xFFB87333)
    Tier.SILVER -> Color(0xFF8E9AA6)
    Tier.GOLD -> Color(0xFFD9A406)
    Tier.DIAMOND -> Color(0xFF3FA9F5)
    Tier.KING -> Color(0xFFE2703A)
}

