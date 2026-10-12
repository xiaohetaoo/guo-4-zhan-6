package com.hdu.cetvocab.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.QUIZ_SCOPE_ALL
import com.hdu.cetvocab.data.QUIZ_SCOPE_CONFUSABLE
import com.hdu.cetvocab.data.QUIZ_SCOPE_TRANSLATION
import com.hdu.cetvocab.data.QuizKind
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.data.quizScopeLabel
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.LetterItem
import com.hdu.cetvocab.ui.QuizUiState
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.BulletRow
import com.hdu.cetvocab.ui.components.IconCheck
import com.hdu.cetvocab.ui.components.IconClose
import com.hdu.cetvocab.ui.components.IconStar
import com.hdu.cetvocab.ui.components.MenuRow
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.SpeakButton
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun QuizScreen(
    vm: AppViewModel,
    onExit: () -> Unit,
    onSpeak: (String) -> Unit,
    onSpeakAuto: (String) -> Unit,
    // 排行榜入口随「闯关」页一起呈现：成绩本来就是在闯关里打出来的，
    // 放在「我的」页要绕一次。这里从选关页与结算页都能直达。
    onOpenLeaderboard: () -> Unit
) {
    val state by vm.quiz.collectAsState()
    val learn by vm.learn.collectAsState()

    // 退出确认：答题中途按返回键先问一句，避免误触丢掉整局成绩
    var confirmQuit by remember { mutableStateOf(false) }

    LaunchedEffect(state.active) {
        if (!state.active) vm.refreshLearn()
    }

    // 拦截系统返回键。
    // 此前完全没有拦截：答题中按返回键会直接离开页面，但 _quiz.active 仍为 true，
    // 再进来时计时/状态都停在原处，用户也不知道这一局到底算不算数。
    BackHandler(enabled = state.active || state.finished) {
        if (state.finished) {
            vm.exitQuiz()
            onExit()
        } else {
            confirmQuit = true
        }
    }

    when {
        state.finished -> QuizResult(
            state,
            onOpenLeaderboard = onOpenLeaderboard,
            onAgain = {
                // 两个专题关卡有自己的出题逻辑，不能直接走 startQuiz(unit)，
                // 否则会被当成「全词挑战」重新抽一遍选义/拼写题。
                when (state.unit) {
                    QUIZ_SCOPE_CONFUSABLE -> vm.startConfusableQuiz()
                    QUIZ_SCOPE_TRANSLATION -> vm.startTranslationQuiz()
                    else -> {
                        // 首字母挑战重开时仍按该字母出题；单元 / 全词挑战沿用原来的范围值
                        val letter = state.letter
                        if (letter != null) vm.startLetterQuiz(letter) else vm.startQuiz(state.unit)
                    }
                }
            },
            vm = vm
        )
        state.active -> QuizPlay(
            state,
            vm,
            onSpeak,
            onSpeakAuto,
            // 关闭按钮与系统返回键走同一套确认：
            // 此前返回键会二次确认、点 X 却直接丢掉整局，两条退出路径行为不一致，
            // 用户从 X 退出时完全不知道这一局成绩不作数。
            onQuit = { confirmQuit = true }
        )
        else -> QuizPicker(
            letters = learn.letters,
            stage = learn.stage,
            onPick = { vm.startLetterQuiz(it) },
            onFullStage = { vm.startQuiz(QUIZ_SCOPE_ALL) },
            onConfusable = { vm.startConfusableQuiz() },
            onTranslation = { vm.startTranslationQuiz() },
            onOpenLeaderboard = onOpenLeaderboard
        )
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.quiz_quit_title)) },
            text = { Text(stringResource(R.string.quiz_quit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    vm.exitQuiz()
                }) { Text(stringResource(R.string.action_quit)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) {
                    Text(stringResource(R.string.action_continue_quiz))
                }
            }
        )
    }
}

// ---------------------------------------------------------------- 选择首字母

@Composable
private fun QuizPicker(
    letters: List<LetterItem>,
    stage: Int,
    onPick: (Char) -> Unit,
    onFullStage: () -> Unit,
    onConfusable: () -> Unit,
    onTranslation: () -> Unit,
    onOpenLeaderboard: () -> Unit
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
                text = "${WordDb.stageLabel(stage)}词库 · 每关 10 题 · 选义 / 拼写 / 辨析 / 翻译四种题型",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            AppCard(color = LocalAppColors.current.cardAmber, padding = 14.dp) {
                Text(
                    text = "玩法：答对得分并累积连击，拼写题分值更高。选择下方首字母开始 10 题挑战。",
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
                    Button(onClick = onFullStage) { Text(stringResource(R.string.action_start_challenge)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            TopicCard(
                title = stringResource(R.string.quiz_topic_confusable),
                desc = stringResource(R.string.quiz_topic_confusable_desc),
                container = LocalAppColors.current.cardSky,
                onClick = onConfusable
            )
            Spacer(Modifier.height(10.dp))
            TopicCard(
                title = stringResource(R.string.quiz_topic_translation),
                desc = stringResource(R.string.quiz_topic_translation_desc),
                container = LocalAppColors.current.cardMint,
                onClick = onTranslation
            )
            Spacer(Modifier.height(10.dp))
            AppCard(padding = 6.dp) {
                MenuRow(
                    title = "历次成绩排行榜",
                    subtitle = stringResource(R.string.leaderboard_entry_desc),
                    accent = LocalAppColors.current.cardAmber,
                    onClick = onOpenLeaderboard
                )
            }
        }
        if (letters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.quiz_lexicon_not_ready),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(letters, key = { it.letter }) { item ->
                // 选义题需要 4 个不同选项，词数不足的字母（如 X）无法出题，置灰不可点
                val usable = item.total >= 4
                val accent = when {
                    !usable -> MaterialTheme.colorScheme.onSurfaceVariant
                    item.done -> LocalAppColors.current.success
                    else -> MaterialTheme.colorScheme.primary
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            when {
                                !usable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                item.done -> LocalAppColors.current.cardMint
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = usable) { onPick(item.letter) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${item.letter}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (usable) "${item.learned}/${item.total}" else "词数不足",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 专题关卡入口卡片。
 * 与「全词挑战」共用同一套标题 + 说明 + 按钮的三段式结构，
 * 只是底色按专题区分，方便在首字母网格上方一眼分辨。
 */
@Composable
private fun TopicCard(
    title: String,
    desc: String,
    container: Color,
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
            Button(onClick = onClick) { Text(stringResource(R.string.action_start_challenge)) }
        }
    }
}

// ---------------------------------------------------------------- 答题

@Composable
private fun QuizPlay(
    state: com.hdu.cetvocab.ui.QuizUiState,
    vm: AppViewModel,
    onSpeak: (String) -> Unit,
    onSpeakAuto: (String) -> Unit,
    onQuit: () -> Unit
) {
    val q = state.current ?: return
    val progress = if (state.total == 0) 0f else (state.index + 1).toFloat() / state.total
    val closeLabel = stringResource(R.string.action_close)

    // 拼写题出题时自动朗读。
    // 此前这里直接调 onSpeak，绕过了「自动朗读」开关 —— 用户在设置里关掉后，
    // 闯关拼写题仍然会出声。改走 onSpeakAuto 后开关统一由 Speech 内部判断。
    LaunchedEffect(state.index, q.spelling) {
        if (q.spelling) onSpeakAuto(q.word.word)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(role = Role.Button, onClick = onQuit)
                        .semantics { contentDescription = closeLabel },
                    contentAlignment = Alignment.Center
                ) {
                    IconClose(
                        modifier = Modifier.size(15.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quizScopeLabel(state.stage, state.unit),
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
            when (q.kind) {
                QuizKind.SPELLING -> SpellingQuestion(q, state, vm, onSpeak)
                QuizKind.TRANSLATION -> TranslationQuestion(q, state, vm)
                QuizKind.CONFUSABLE -> ConfusableQuestion(q, state, vm)
                else -> ChoiceQuestion(q, state, vm, onSpeak)
            }

            if (state.revealed) {
                Spacer(Modifier.height(14.dp))
                AppCard(
                    color = if (isCorrect(state, q))
                        LocalAppColors.current.cardMint else LocalAppColors.current.cardPeach,
                    padding = 16.dp
                ) {
                    val ok = isCorrect(state, q)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ok) {
                            IconCheck(
                                modifier = Modifier.size(19.dp),
                                color = LocalAppColors.current.success
                            )
                        } else {
                            IconClose(
                                modifier = Modifier.size(19.dp),
                                color = LocalAppColors.current.danger
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = when (q.kind) {
                                QuizKind.SPELLING ->
                                    if (ok) "拼写正确" else "正确答案：${q.word.word}"
                                QuizKind.TRANSLATION ->
                                    if (ok) "译法全部选对" else "正确译法共 ${q.answerSet.size} 项"
                                QuizKind.CONFUSABLE ->
                                    if (ok) "回答正确" else "正确答案：${q.options.getOrNull(q.correctIndex) ?: q.word.word}"
                                else ->
                                    if (ok) "回答正确" else "正确答案：${q.word.translation}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (q.kind == QuizKind.TRANSLATION) "关键词 ${q.word.word}" else q.word.word,
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

                    if (q.kind == QuizKind.TRANSLATION) {
                        // 翻译题把「可接受译法」逐条列出 —— 一义多译正是这关的训练重点
                        Text(
                            text = "可接受译法：",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        q.answerSet.sorted().forEach { idx ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                IconCheck(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .alignByBaseline()
                                        .padding(end = 6.dp),
                                    color = LocalAppColors.current.success
                                )
                                Text(
                                    text = q.options.getOrNull(idx).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = q.word.translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 辨析题的关键价值在「横向对比」：把四个形近词的释义一起摆出来
                    if (q.kind == QuizKind.CONFUSABLE && q.optionNotes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        q.optionNotes.forEach { note ->
                            BulletRow(
                                text = note,
                                modifier = Modifier.padding(vertical = 2.dp),
                                dotColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (q.analysis.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = q.analysis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 翻译题不再挂词库例句：关键词例句有不少是产品说明书、论文摘要之类的
                    // 碎片，读起来莫名其妙；而「可接受译法」本身已经给出了规范的英文表达，
                    // 再补一句差例句反而是减分。
                    if (q.kind != QuizKind.TRANSLATION && q.word.sentence.isNotBlank()) {
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
                    else when (q.kind) {
                        QuizKind.SPELLING -> vm.submitSpelling()
                        QuizKind.TRANSLATION -> vm.submitTranslation()
                        else -> if (state.selected >= 0) vm.selectOption(state.selected)
                    }
                },
                enabled = state.revealed || when (q.kind) {
                    QuizKind.SPELLING -> true
                    QuizKind.TRANSLATION -> state.selectedSet.isNotEmpty()
                    else -> state.selected >= 0
                },
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
                    } else if (q.kind == QuizKind.TRANSLATION) "提交所选译法"
                    else "提交答案",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 选项行的读屏文案：把「第几项」和内容合成一句话。
 * 不加的话 TalkBack 会把 A、选项文本、勾选框拆成几个节点分别播报，听感支离破碎。
 */
private fun optionLabel(index: Int, text: String): String =
    "选项 ${('A'.code + index).toChar()}：$text"

private fun isSpellingCorrect(state: QuizUiState, q: QuizQuestion): Boolean =
    state.typed.trim().equals(q.word.word.trim(), ignoreCase = true)

/**
 * 统一判分入口。
 * 四种题型的「答对」定义各不相同，散落在 UI 里各写一份极易漏改，
 * 这里收敛成一个函数，结果卡配色与文案都只认它。
 */
private fun isCorrect(state: QuizUiState, q: QuizQuestion): Boolean = when (q.kind) {
    QuizKind.SPELLING -> isSpellingCorrect(state, q)
    // 翻译题要求与可接受译法完全一致：少选漏译、多选误译都算错
    QuizKind.TRANSLATION -> state.selectedSet == q.answerSet
    else -> state.selected == q.correctIndex
}

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
            OptionRow(
                index = i,
                text = opt,
                selected = state.selected == i,
                isAnswer = i == q.correctIndex,
                revealed = state.revealed,
                onClick = { vm.selectOption(i) }
            )
        }
    }
}

/**
 * 单选选项行。
 * 四种题型里「选义」与「易混词辨析」共用这一份实现，
 * 区别只在题干与是否朗读，选项的选中 / 揭晓配色完全一致。
 */
@Composable
private fun OptionRow(
    index: Int,
    text: String,
    selected: Boolean,
    isAnswer: Boolean,
    revealed: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        revealed && isAnswer -> LocalAppColors.current.success.copy(alpha = 0.16f)
        revealed && selected -> LocalAppColors.current.danger.copy(alpha = 0.14f)
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        revealed && isAnswer -> LocalAppColors.current.success
        revealed && selected -> LocalAppColors.current.danger
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            // 这一行此前只是「可点击」，TalkBack 读不出来是第几个选项、有没有选中。
            // 用 selectable + RadioButton 角色补齐角色与状态，并把选项文本并进
            // contentDescription，让整行一次读全。
            .selectable(
                selected = selected,
                enabled = !revealed,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = optionLabel(index, text) }
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
                text = listOf("A", "B", "C", "D").getOrElse(index) { "?" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.weight(1f)
        )
        if (revealed && isAnswer) {
            IconCheck(
                modifier = Modifier.size(18.dp),
                color = LocalAppColors.current.success
            )
        }
        if (revealed && selected && !isAnswer) {
            IconClose(
                modifier = Modifier.size(18.dp),
                color = LocalAppColors.current.danger
            )
        }
    }
}

/**
 * 易混词辨析：给出中文释义，从四个形近词里挑出对应的单词。
 *
 * 题干刻意不朗读、也不展示目标词拼写 —— 一旦放出来这题就等于直接报答案，
 * 训练重点恰恰是「只看释义能否在形近词之间做出区分」。
 */
@Composable
private fun ConfusableQuestion(
    q: QuizQuestion,
    state: QuizUiState,
    vm: AppViewModel
) {
    AppCard(modifier = Modifier.fillMaxWidth(), padding = 22.dp) {
        Tag(
            text = "易混词辨析",
            container = LocalAppColors.current.cardSky,
            contentColor = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "下面哪个单词是这个意思？",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = q.prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        q.options.forEachIndexed { i, opt ->
            OptionRow(
                index = i,
                text = opt,
                selected = state.selected == i,
                isAnswer = i == q.correctIndex,
                revealed = state.revealed,
                onClick = { vm.selectOption(i) }
            )
        }
    }
}

/**
 * 真题句子翻译：给出中文句子，从四个英文译文里选出**全部**可接受译法。
 *
 * 选项左侧用方块而不是 ABCD 圆标 —— 方形 + 勾选态是通用的「多选」视觉语言，
 * 与单选题的字母标形成区分，避免用户误以为只能选一个。
 */
@Composable
private fun TranslationQuestion(
    q: QuizQuestion,
    state: QuizUiState,
    vm: AppViewModel
) {
    AppCard(modifier = Modifier.fillMaxWidth(), padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(
                text = "真题句子翻译",
                container = LocalAppColors.current.cardMint,
                contentColor = LocalAppColors.current.success
            )
            Spacer(Modifier.width(8.dp))
            Tag(
                text = "多选",
                container = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "把下面这句话译成英文，选出所有正确的译法",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = q.prompt,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(20.dp))
        q.options.forEachIndexed { i, opt ->
            MultiOptionRow(
                index = i,
                text = opt,
                checked = i in state.selectedSet,
                isAnswer = i in q.answerSet,
                revealed = state.revealed,
                onClick = { vm.toggleTranslationOption(i) }
            )
        }
    }
}

/** 多选选项行：方形勾选框 + 揭晓后标注「应选 / 不应选」 */
@Composable
private fun MultiOptionRow(
    index: Int,
    text: String,
    checked: Boolean,
    isAnswer: Boolean,
    revealed: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        revealed && isAnswer -> LocalAppColors.current.success.copy(alpha = 0.16f)
        revealed && checked -> LocalAppColors.current.danger.copy(alpha = 0.14f)
        checked -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        revealed && isAnswer -> LocalAppColors.current.success
        revealed && checked -> LocalAppColors.current.danger
        checked -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            // 同 OptionRow：多选行需要 Checkbox 角色，读屏才能播报「已勾选 / 未勾选」
            .selectable(
                selected = checked,
                enabled = !revealed,
                role = Role.Checkbox,
                onClick = onClick
            )
            .semantics { contentDescription = optionLabel(index, text) }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    1.5.dp,
                    if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                IconCheck(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.weight(1f)
        )
        if (revealed && isAnswer && !checked) {
            // 漏选：明确提示「这一项也该选」，比单纯标红更有指导性
            Text(
                text = "漏选",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.success
            )
        }
        if (revealed && checked && !isAnswer) {
            IconClose(
                modifier = Modifier.size(18.dp),
                color = LocalAppColors.current.danger
            )
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
            placeholder = { Text(stringResource(R.string.hint_input_word)) },
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
    onOpenLeaderboard: () -> Unit,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                IconStar(
                    modifier = Modifier.size(40.dp),
                    filled = i < state.stars,
                    color = if (i < state.stars) LocalAppColors.current.gold
                    else MaterialTheme.colorScheme.outlineVariant
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
            Text(stringResource(R.string.action_play_again), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { vm.exitQuiz(); onOpenLeaderboard() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.action_view_leaderboard))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                state.letter != null -> "返回后可继续选择其他首字母"
                state.unit == QUIZ_SCOPE_ALL -> "返回后可重新挑战本级全部词汇"
                else -> "返回后可继续选择其他单元"
            },
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
