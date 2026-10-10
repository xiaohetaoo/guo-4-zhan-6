package com.hdu.cetvocab.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.ExampleBlock
import com.hdu.cetvocab.ui.components.LevelDots
import com.hdu.cetvocab.ui.components.MnemonicBlock
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.SimilarBlock
import com.hdu.cetvocab.ui.components.SpeakButton
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.WordHeadline
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun StudyScreen(
    vm: AppViewModel,
    speak: (String) -> Unit,
    onFinish: () -> Unit
) {
    val state by vm.study.collectAsState()

    // 统计学习时长
    DisposableEffect(Unit) {
        val start = System.currentTimeMillis()
        onDispose {
            val sec = (System.currentTimeMillis() - start) / 1000
            if (sec > 0) vm.addStudySeconds(sec)
        }
    }

    // 切换单词时自动发音
    LaunchedEffect(state.index, state.finished) {
        val w = state.current
        if (w != null && vm.autoSpeak) speak(w.word)
    }

    if (state.finished) {
        StudySummary(state.knownCount, state.unknownCount, state.newAchievements, onFinish)
        return
    }

    val word = state.current
    if (word == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("这个队列没有单词", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onFinish) { Text("返回") }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---------- 顶部进度 ----------
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onFinish() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${state.index + 1} / ${state.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Tag(
                    text = "认识 ${state.knownCount}",
                    container = LocalAppColors.current.cardMint,
                    contentColor = LocalAppColors.current.success
                )
                Spacer(Modifier.width(6.dp))
                Tag(
                    text = "待加强 ${state.unknownCount}",
                    container = LocalAppColors.current.cardPeach,
                    contentColor = LocalAppColors.current.warning
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = {
                    if (state.total == 0) 0f
                    else (state.index + 1).toFloat() / state.total
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // ---------- 卡片 ----------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Crossfade(
                targetState = state.flipped,
                animationSpec = tween(260),
                label = "card"
            ) { flipped ->
                if (!flipped) CardFront(word, speak, vm, state.favIds.contains(word.id))
                else CardBack(word, speak)
            }
        }

        // ---------- 底部操作 ----------
        Column(modifier = Modifier.padding(16.dp)) {
            if (!state.flipped) {
                Button(
                    onClick = { vm.flipCard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("查看释义与例句", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.answerCurrent(known = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAppColors.current.danger)
                    ) {
                        Text("不认识", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { vm.answerCurrent(known = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.success)
                    ) {
                        Text("认识了", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (state.flipped) "选择后自动进入下一个单词" else "先回忆词义，再翻卡自测",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardFront(
    word: Word,
    speak: (String) -> Unit,
    vm: AppViewModel,
    fav: Boolean
) {
    AppCard(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        padding = 24.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(text = "${com.hdu.cetvocab.data.WordDb.stageLabel(word.stage)} · 第 ${word.unit} 单元")
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (fav) LocalAppColors.current.cardAmber else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { vm.toggleFavCurrent() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (fav) "♥" else "♡",
                    fontSize = 18.sp,
                    color = if (fav) LocalAppColors.current.warning else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word.word,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = word.phonetic,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))
            SpeakButton(text = word.word, onSpeak = speak, size = 56.dp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LevelDots(level = 0)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "点击下方按钮查看释义",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardBack(word: Word, speak: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        WordHeadline(word = word, onSpeak = speak, wordSize = 30)
        Spacer(Modifier.height(14.dp))

        SectionTitle(text = "释义")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (word.pos.isNotBlank()) {
                Tag(text = word.pos, container = LocalAppColors.current.cardPeach, contentColor = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = word.translation,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        if (word.transDetail.isNotBlank() && word.transDetail != word.translation) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                word.transDetail.split("\n").filter { it.isNotBlank() }.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
        if (word.definition.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = word.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (word.sentence.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(text = "例句", accent = LocalAppColors.current.success)
            Spacer(Modifier.height(8.dp))
            ExampleBlock(word = word, onSpeak = speak)
        }

        if (word.roots.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(text = "词根词缀", accent = LocalAppColors.current.warning)
            Spacer(Modifier.height(8.dp))
            Text(
                text = word.roots,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LocalAppColors.current.cardPeach)
                    .padding(12.dp)
            )
        }

        if (word.mnemonic.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(text = "联想记忆", accent = LocalAppColors.current.warning)
            Spacer(Modifier.height(8.dp))
            MnemonicBlock(word)
        }

        if (word.similarList.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(text = "形近词辨析", accent = LocalAppColors.current.danger)
            Spacer(Modifier.height(8.dp))
            SimilarBlock(word)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StudySummary(
    known: Int,
    unknown: Int,
    achievements: List<com.hdu.cetvocab.data.AchievementDef>,
    onFinish: () -> Unit
) {
    val total = known + unknown
    val rate = if (total == 0) 0 else known * 100 / total
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (rate >= 80) "🎉" else "💪",
                fontSize = 46.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "本组学习完成",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "共 $total 个单词 · 认识率 $rate%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            AppCard(padding = 18.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile("认识", known, LocalAppColors.current.success, LocalAppColors.current.cardMint, Modifier.weight(1f))
                    SummaryTile("待加强", unknown, LocalAppColors.current.warning, LocalAppColors.current.cardAmber, Modifier.weight(1f))
                }
            }
            if (achievements.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AppCard(padding = 16.dp) {
                    Text(
                        text = "解锁新成就",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        achievements.take(3).forEach { def ->
                            AchievementBadge(def = def, unlocked = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("完成", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Int,
    tint: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
