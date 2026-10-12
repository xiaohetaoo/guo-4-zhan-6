package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.springPress

/**
 * 喇叭按钮：纯 Canvas 绘制，不依赖图标字体或图片资源。
 *
 * 按钮里的喇叭是自绘图形、没有内置语义，因此由外层可点区域统一给出
 * 朗读文本（默认「朗读」），避免 TalkBack 只念出一个无名按钮。
 */
@Composable
fun SpeakButton(
    text: String,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    filled: Boolean = true,
    label: String? = null
) {
    val speakLabel = label ?: stringResource(R.string.action_speak)
    val interaction = remember { MutableInteractionSource() }
    val bg = if (filled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = modifier
            .size(size)
            .springPress(interaction, pressedScale = 0.86f)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = { onSpeak(text) }
            )
            .semantics { contentDescription = speakLabel },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.46f)) {
            val w = this.size.width
            val h = this.size.height
            val horn = Path().apply {
                moveTo(w * 0.02f, h * 0.34f)
                lineTo(w * 0.26f, h * 0.34f)
                lineTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.50f, h * 0.92f)
                lineTo(w * 0.26f, h * 0.66f)
                lineTo(w * 0.02f, h * 0.66f)
                close()
            }
            drawPath(horn, fg)
            drawArc(
                color = fg,
                startAngle = -55f, sweepAngle = 110f, useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.28f),
                size = Size(w * 0.34f, h * 0.44f),
                style = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
            )
            drawArc(
                color = fg,
                startAngle = -55f, sweepAngle = 110f, useCenter = false,
                topLeft = Offset(w * 0.44f, h * 0.12f),
                size = Size(w * 0.54f, h * 0.76f),
                style = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
            )
        }
    }
}

/** 小节标题 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** 单词标题行：单词 + 音标 + 发音 */
@Composable
fun WordHeadline(
    word: Word,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
    wordSize: Int = 34,
    showSpeak: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.word,
                fontSize = wordSize.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word.phonetic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showSpeak) {
            SpeakButton(text = word.word, onSpeak = onSpeak)
        }
    }
}

/** 例句块 */
@Composable
fun ExampleBlock(word: Word, onSpeak: (String) -> Unit, modifier: Modifier = Modifier) {
    if (word.sentence.isBlank()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = word.sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SpeakButton(
                text = word.sentence,
                onSpeak = onSpeak,
                size = 32.dp,
                filled = false
            )
        }
        if (word.sentenceCn.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = word.sentenceCn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 记忆方法块 */
@Composable
fun MnemonicBlock(word: Word, modifier: Modifier = Modifier) {
    if (word.mnemonic.isBlank()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAppColors.current.gold.copy(alpha = 0.13f))
            .padding(14.dp)
    ) {
        word.mnemonic.split("\n").filter { it.isNotBlank() }.forEach { line ->
            BulletRow(
                text = line,
                modifier = Modifier.padding(vertical = 2.dp),
                dotColor = LocalAppColors.current.gold
            )
        }
    }
}

/** 形近词块 */
@Composable
fun SimilarBlock(word: Word, modifier: Modifier = Modifier) {
    val list = word.similarList
    if (list.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAppColors.current.warning.copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        list.forEach { (w, t) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = w,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(112.dp)
                )
                Text(
                    text = t,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 掌握等级小圆点。
 *
 * 圆点是纯色块、没有任何语义，TalkBack 默认完全读不出来；
 * 这里把它折叠成一个可朗读的整体（例如「掌握等级 3/5」），
 * 而不是让读屏器逐个念 5 个无名色块。
 */
@Composable
fun LevelDots(level: Int, modifier: Modifier = Modifier, describe: Boolean = true) {
    val dots = 5
    val lit = level.coerceIn(0, dots)
    val description = stringResource(R.string.cd_level_dots, lit, dots)
    Row(
        modifier = if (describe) modifier.semantics { contentDescription = description } else modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(dots) { i ->
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < lit) LocalAppColors.current.success else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

/** 空状态 */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier, hint: String = "") {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconLeaf(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
