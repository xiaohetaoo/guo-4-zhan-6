package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.UnitItem
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.SpeakButton
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun QuizScreen(
    vm: AppViewModel,
    onExit: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val state by vm.quiz.collectAsState()
    val learn by vm.learn.collectAsState()

    LaunchedEffect(state.active) {
        if (!state.active) vm.refreshLearn()
    }

    when {
        state.finished -> QuizResult(state, onExit, onAgain = { vm.startQuiz(state.unit) }, vm = vm)
        state.active -> QuizPlay(state, vm, onSpeak, onQuit = { vm.exitQuiz() })
        else -> QuizPicker(
            units = learn.units,
            stage = learn.stage,
            onPick = { vm.startQuiz(it) },
            onFullStage = { vm.startQuiz(-1) }
        )
    }
}

// ---------------------------------------------------------------- 选择单元

@Composable
private fun QuizPicker(
    units: List<UnitItem>,
    stage: Int,
    onPick: (Int) -> Unit,
    onFullStage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "单词闯关",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${WordDb.stageLabel(stage)}词库 · 每关 10 题 · 含选义与拼写两种题型",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            AppCard(color = LocalAppColors.current.cardAmber, padding = 14.dp) {
                Text(
                    text = "玩法：答对得分并累积连击，拼写题分值更高。选择下方单元开始 10 题挑战。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            AppCard(color = LocalAppColors.current.cardPeach, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${WordDb.stageLabel(stage)}全词挑战",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "从本级全部单词中随机抽取 30 题",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onFullStage) { Text("开始挑战") }
                }
            }
        }
        if (units.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("词库尚未就绪", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(units, key = { it.unit }) { u ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (u.done) LocalAppColors.current.cardMint else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onPick(u.unit) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${u.unit}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (u.done) LocalAppColors.current.success else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "第 ${u.unit} 单元",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${u.learned}/${u.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 答题

@Composable
private fun QuizPlay(
    state: com.hdu.cetvocab.ui.QuizUiState,
    vm: AppViewModel,
    onSpeak: (String) -> Unit,
    onQuit: () -> Unit
) {
    val q = state.current ?: return
    val progress = if (state.total == 0) 0f else (state.index + 1).toFloat() / state.total

    LaunchedEffect(state.index, q.spelling) {
        if (q.spelling) onSpeak(q.word.word)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onQuit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.unit < 0) "${WordDb.stageLabel(state.stage)} · 全词挑战" else "${WordDb.stageLabel(state.stage)} · 第 ${state.unit} 单元",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "第 ${state.index + 1} / ${state.total} 题",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.combo >= 2) {
                    Tag(
                        text = "连击 ${state.combo}",
                        container = LocalAppColors.current.cardAmber,
                        contentColor = LocalAppColors.current.warning
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Tag(
                    text = "${state.score} 分",
                    container = LocalAppColors.current.cardPeach,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (q.spelling) SpellingQuestion(q, state, vm, onSpeak)
            else ChoiceQuestion(q, state, vm, onSpeak)

            if (state.revealed) {
                Spacer(Modifier.height(14.dp))
                AppCard(
                    color = if (state.selected == q.correctIndex || isSpellingCorrect(state, q))
                        LocalAppColors.current.cardMint else LocalAppColors.current.cardPeach,
                    padding = 16.dp
                ) {
                    Text(
                        text = if (q.spelling) {
                            if (isSpellingCorrect(state, q)) "✅ 拼写正确" else "❌ 正确答案：${q.word.word}"
                        } else {
                            if (state.selected == q.correctIndex) "✅ 回答正确"
                            else "❌ 正确答案：${q.word.translation}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = q.word.word,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (q.word.phonetic.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = q.word.phonetic,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        SpeakButton(
                            text = q.word.word,
                            onSpeak = onSpeak,
                            size = 30.dp,
                            filled = false
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = q.word.translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (q.word.sentence.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = q.word.sentence,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (q.word.sentenceCn.isNotBlank()) {
                            Text(
                                text = q.word.sentenceCn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    if (state.revealed) vm.nextQuestion()
                    else if (q.spelling) vm.submitSpelling()
                    else if (state.selected >= 0) vm.selectOption(state.selected)
                },
                enabled = state.revealed || q.spelling || state.selected >= 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.revealed) {
                        if (state.index + 1 >= state.total) "查看成绩" else "下一题"
                    } else "提交答案",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun isSpellingCorrect(state: com.hdu.cetvocab.ui.QuizUiState, q: QuizQuestion): Boolean =
    state.typed.trim().equals(q.word.word.trim(), ignoreCase = true)

@Composable
private fun ChoiceQuestion(
    q: QuizQuestion,
    state: com.hdu.cetvocab.ui.QuizUiState,
    vm: AppViewModel,
    onSpeak: (String) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(text = "选出正确释义", container = LocalAppColors.current.cardPeach, contentColor = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            SpeakButton(text = q.word.word, onSpeak = onSpeak, size = 36.dp, filled = false)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = q.word.word,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        if (q.word.phonetic.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = q.word.phonetic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        q.options.forEachIndexed { i, opt ->
            val selected = state.selected == i
            val isCorrect = i == q.correctIndex
            val container = when {
                state.revealed && isCorrect -> LocalAppColors.current.success.copy(alpha = 0.16f)
                state.revealed && selected -> LocalAppColors.current.danger.copy(alpha = 0.14f)
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val content = when {
                state.revealed && isCorrect -> LocalAppColors.current.success
                state.revealed && selected -> LocalAppColors.current.danger
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .clickable(enabled = !state.revealed) {
                        vm.selectOption(i)
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(content.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = listOf("A", "B", "C", "D").getOrElse(i) { "?" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = opt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                    modifier = Modifier.weight(1f)
                )
                if (state.revealed && isCorrect) Text("✓", color = LocalAppColors.current.success)
                if (state.revealed && selected && !isCorrect) Text("✕", color = LocalAppColors.current.danger)
            }
        }
    }
}

@Composable
private fun SpellingQuestion(
    q: QuizQuestion,
    state: com.hdu.cetvocab.ui.QuizUiState,
    vm: AppViewModel,
    onSpeak: (String) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(text = "听音拼写", container = LocalAppColors.current.cardAmber, contentColor = LocalAppColors.current.warning)
            Spacer(Modifier.weight(1f))
            SpeakButton(text = q.word.word, onSpeak = onSpeak, size = 44.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = q.word.translation,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "根据中文释义与发音拼写单词（${q.word.word.length} 个字母）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = state.typed,
            onValueChange = { vm.updateTyped(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.revealed,
            singleLine = true,
            placeholder = { Text("输入英文单词") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.submitSpelling() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        if (!state.revealed) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "提示：可点击右上角喇叭重复播放",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------- 结算

@Composable
private fun QuizResult(
    state: com.hdu.cetvocab.ui.QuizUiState,
    onExit: () -> Unit,
    onAgain: () -> Unit,
    vm: AppViewModel
) {
    val total = state.total
    val accuracy = if (total == 0) 0 else state.correctCount * 100 / total
    val seconds = ((state.lastRecord?.durationMs ?: 0L) / 1000).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Text(
                    text = "★",
                    fontSize = 40.sp,
                    color = if (i < state.stars) LocalAppColors.current.gold else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                state.stars >= 3 -> "太棒了，满分通关！"
                state.stars == 2 -> "不错，再冲一次满分"
                state.stars == 1 -> "及格啦，继续巩固"
                else -> "别灰心，多练几遍就好"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))
        AppCard(padding = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "本关得分",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${state.score}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " 分",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Tag(
                        text = "正确率 $accuracy%",
                        container = if (accuracy >= 80) LocalAppColors.current.cardMint else LocalAppColors.current.cardPeach,
                        contentColor = if (accuracy >= 80) LocalAppColors.current.success else LocalAppColors.current.warning
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultTile("答对", "${state.correctCount}/$total", Modifier.weight(1f))
                ResultTile("最高连击", "${state.maxCombo}", Modifier.weight(1f))
                ResultTile("用时", "${seconds}s", Modifier.weight(1f))
            }
        }

        if (state.newAchievements.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            AppCard(padding = 16.dp) {
                SectionTitle(text = "解锁新成就", accent = LocalAppColors.current.gold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.newAchievements.take(3).forEach { def ->
                        AchievementBadge(def = def, unlocked = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AppCard(color = LocalAppColors.current.cardMint, padding = 16.dp) {
            Text(
                text = "本次成绩已记入「历次成绩排行榜」，可以随时和自己的历史最好成绩比较。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("再来一局", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { vm.exitQuiz(); onExit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("查看排行榜")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (state.unit < 0) "返回后可重新挑战本级全部词汇" else "返回后可继续选择其他单元",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { vm.exitQuiz() }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResultTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
