package com.hdu.cetvocab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.EXTENSION_QUIZ_SCOPES
import com.hdu.cetvocab.data.QUIZ_SCOPE_ALL
import com.hdu.cetvocab.data.QUIZ_SCOPE_CONFUSABLE
import com.hdu.cetvocab.data.QUIZ_SCOPE_TRANSLATION
import com.hdu.cetvocab.data.QUIZ_SCOPE_WRONG
import com.hdu.cetvocab.data.QuizKind
import com.hdu.cetvocab.data.durationText
import com.hdu.cetvocab.data.percentOf
import com.hdu.cetvocab.data.QuizQuestion
import com.hdu.cetvocab.data.STAGE_QUIZ_COUNT
import com.hdu.cetvocab.data.UNIT_QUIZ_COUNT
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.data.quizScopeLabel
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.LetterItem
import com.hdu.cetvocab.ui.QuizUiState
import com.hdu.cetvocab.ui.RestorableQuiz
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
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.ui.theme.springPress

@Composable
fun QuizScreen(
    vm: AppViewModel,
    onExit: () -> Unit,
    // 拓展资源练习（易混词辨析 / 真题句子翻译）的退出落点：它们从「我的」页进来，
    // 退出要回「我的」。普通闯关用不上这个回调（走 onExit / 排行榜入口）。
    onOpenProfile: () -> Unit,
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

    // 离开闯关页时停表。闯关页本身是底部标签之一，用户可以直接点别的标签切走 ——
    // 这时应用并没有退到后台，MainActivity.onStop 不会触发，若不在这里停表，
    // 用户在别的页面停留的这段时间会被算进「本关用时」，排行榜里的用时因此虚高。
    // 回到本页时由 onAppForeground 之外的路径恢复：见下方 LaunchedEffect。
    DisposableEffect(Unit) {
        onDispose { vm.onQuizScreenExit() }
    }
    LaunchedEffect(state.active, state.finished) {
        if (state.active && !state.finished) vm.onQuizScreenEnter()
    }

    /*
     * 「继续上一局」的数据。进程被系统回收后导航栈会把闯关页恢复出来，
     * 但题目只在内存里，用户回来看到的是选关页、刚才那局无声消失。
     * 这里在选关页顶部挂一条恢复入口 —— 刻意不自动恢复：快照在正常杀进程后
     * 同样存在，自动塞回一局旧题会让用户觉得应用记住了他不想要的东西。
     */
    var restorable by remember { mutableStateOf<RestorableQuiz?>(null) }
    // 快照版本号：点了「继续」但恢复失败时，本局的 active/finished 两个键都不会变，
    // 只靠它们重算会让那张卡片变成「点了没反应的死按钮」。见 AppViewModel.quizSnapshotEpoch。
    val snapshotEpoch by vm.quizSnapshotEpoch.collectAsState()
    LaunchedEffect(state.active, state.finished, snapshotEpoch) {
        restorable = if (!state.active && !state.finished) vm.peekRestorableQuiz() else null
    }

    // 拦截系统返回键。
    // 此前完全没有拦截：答题中按返回键会直接离开页面，但 _quiz.active 仍为 true，
    // 再进来时计时/状态都停在原处，用户也不知道这一局到底算不算数。
    //
    // 拓展练习（易混词辨析 / 真题句子翻译）从「我的」页进来，用的是
    // goTab(QUIZ) + popUpTo(HOME)，返回栈是 [首页, 闯关]：这时 onExit()（popBackStack）
    // 会把人带回**首页**，而结算页的按钮与文案都已经承诺「回我的」。
    // 退出落点必须按关卡类型统一判断，否则同一局游戏三条退出路径指向两个地方。
    val isExtensionQuiz = state.unit in EXTENSION_QUIZ_SCOPES
    // 错题重做从排行榜进来，退出就该回排行榜：那里才显示「还剩几道待重做」。
    // 走 onExit()（popBackStack）会被带回首页，用户得自己再找一遍入口。
    val isRedoQuiz = state.unit == QUIZ_SCOPE_WRONG
    fun quitTarget() {
        when {
            isExtensionQuiz -> onOpenProfile()
            isRedoQuiz -> onOpenLeaderboard()
            else -> onExit()
        }
    }
    BackHandler(enabled = state.active || state.finished) {
        if (state.finished) {
            vm.exitQuiz()
            quitTarget()
        } else {
            confirmQuit = true
        }
    }

    when {
        state.finished -> QuizResult(
            state,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenProfile = onOpenProfile,
            onAgain = {
                // 两个专题关卡有自己的出题逻辑，不能直接走 startQuiz(unit)，
                // 否则会被当成「全词挑战」重新抽一遍选义/拼写题。
                when (state.unit) {
                    QUIZ_SCOPE_CONFUSABLE -> vm.startConfusableQuiz()
                    QUIZ_SCOPE_TRANSLATION -> vm.startTranslationQuiz()
                    // 重做局再开一次 = 按**当前**错题集再练一遍：
                    // 刚才答对的题已经移出去了，这一局只会剩下还没订正对的
                    QUIZ_SCOPE_WRONG -> vm.startWrongRedo()
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
            onOpenLeaderboard = onOpenLeaderboard,
            restorable = restorable,
            onResume = { vm.resumeQuiz(it) }
        )
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = {
                // 拓展练习不叫「闯关」，退出确认的措辞跟着换，
                // 免得在「我的 → 易混词辨析」里点返回却弹出「退出本次闯关？」
                Text(
                    stringResource(
                        if (state.unit in EXTENSION_QUIZ_SCOPES) R.string.practice_quit_title
                        else R.string.quiz_quit_title
                    )
                )
            },
            text = { Text(stringResource(R.string.quiz_quit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    // 拓展练习中途退出同样要回「我的」：只 exitQuiz() 的话人停在闯关选关页，
                    // 而这一局是从「我的 → 易混词辨析 / 真题句子翻译」进来的。
                    vm.exitQuiz()
                    // 中途放弃也要回到「这一局的来处」：拓展练习回「我的」，
                    // 错题重做回排行榜（那里能立刻看到还剩几道）。
                    // 普通闯关留在闯关页，方便直接再开一关。
                    if (isExtensionQuiz || isRedoQuiz) quitTarget()
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
    onOpenLeaderboard: () -> Unit,
    restorable: RestorableQuiz? = null,
    onResume: (RestorableQuiz) -> Unit = {}
) {
    /*
     * 整页滑动：标题 / 玩法 / 全词挑战 / 排行榜入口与首字母网格放在**同一个**滚动容器里，
     * 从顶部一路滑到底不会中途被截断（此前表头是固定的 Column，只有网格在滚）。
     *
     * 实现上用「单个 LazyVerticalGrid + 全宽表头项」，而不是「LazyColumn 里嵌网格」：
     * 后者在 Compose 里是非法的（两个纵向 Lazy 容器嵌套，内层拿不到高度约束，直接崩）。
     * 表头用 GridItemSpan(maxLineSpan) 横跨全部 4 列，视觉上与独占一行毫无区别，
     * 但会跟着网格一起滚走。
     *
     * 两个专题关卡（易混词辨析 / 真题句子翻译）已移到「我的」页的拓展资源练习区，
     * 这里不再出现 —— 它们不参与排行榜排名，与「闯关」的定位不同。
     */
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "picker_header", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = "单词闯关",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    // 「每关10题」改成「每关最多10题」：首字母挑战从该字母的全部单词里抽题，
                    // 小词量字母（实测四级 z 只有 5 个、y 10 个）凑不满 10 道，
                    // 原来的说法会让选关页承诺 10 题、答题页显示「第1/5题」。
                    text = "${WordDb.stageLabel(stage)}词库、每关最多${UNIT_QUIZ_COUNT}题、选义/拼写两种题型",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppCard(color = LocalAppColors.current.cardAmber, padding = 14.dp) {
                    Text(
                        text = "玩法：答对得分并累积连击，拼写题分值更高。" +
                            "选择下方首字母开始挑战。",
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
                                text = "从本级全部单词中随机抽取${STAGE_QUIZ_COUNT}题",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = onFullStage) {
                            Text(stringResource(R.string.action_start_challenge))
                        }
                    }
                }
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
        }

        restorable?.let { r ->
            item(key = "picker_resume", span = { GridItemSpan(maxLineSpan) }) {
                AppCard(color = LocalAppColors.current.cardMint, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "有一局没打完",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "答到第${r.index + 1}题，当前${r.score}分，约${r.remaining}分钟内可继续",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { onResume(r) }) { Text("继续") }
                    }
                }
            }
        }

        if (letters.isEmpty()) {
            item(key = "picker_empty", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.quiz_lexicon_not_ready),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyVerticalGrid
        }

        items(letters, key = { "letter_${it.letter}" }) { item ->
            // 选义题需要 4 个不同选项，词数不足的字母（如 X）无法出题，置灰不可点
            val usable = item.total >= 4
            val cellInteraction = remember { MutableInteractionSource() }
            val accent = when {
                !usable -> MaterialTheme.colorScheme.onSurfaceVariant
                item.done -> LocalAppColors.current.success
                else -> MaterialTheme.colorScheme.primary
            }
            Column(
                modifier = Modifier
                    .springPress(cellInteraction)
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
                    .clickable(
                        interactionSource = cellInteraction,
                        indication = null,
                        enabled = usable
                    ) { onPick(item.letter) }
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
    val closeInteraction = remember { MutableInteractionSource() }

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
                        .springPress(closeInteraction, pressedScale = 0.88f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = onQuit
                        )
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
                        text = "第${state.index + 1}/${state.total}题",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.combo >= 2) {
                    Tag(
                        text = "连击${state.combo}",
                        container = LocalAppColors.current.cardAmber,
                        contentColor = LocalAppColors.current.warning
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Tag(
                    text = "${state.score}分",
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
            /*
             * 每换一题，题干从右侧滑入 + 淡入。
             *
             * 这里用 `key(index)` 重建整棵子树，而不是 AnimatedContent：
             * AnimatedContent 在退场动画期间还会渲染「上一题」，但它读的是实时 state
             * （此时已经是新题的选中项与揭晓态），旧题的选项会串成新题的样子。
             * key 直接把旧子树丢掉，没有这个隐患，代价是少了退场动画——
             * 但题干是「一题接一题」的流，入场足够。
             *
             * **解析卡必须和题干在同一个 key 里。** 它此前挂在 key 外面：
             * nextQuestion() 同时把 index+1 与 revealed=false，退场动画播放的那一帧
             * `q` 已经是下一题，于是卡片会把**下一题的单词、音标、释义和「正确答案」**
             * 直接播给玩家看（拼写题等于送分）。归进同一个 key 后换题即整棵丢弃，
             * 跨题渲染的机会不存在了；相应地退场动画也不会再有机会播放，写 None 说明意图。
             */
            key(state.index) {
                val enter = remember { Animatable(0f) }
                LaunchedEffect(Unit) { enter.animateTo(1f, Motion.spatial()) }
                Column(
                    modifier = Modifier.graphicsLayer {
                        // 弹簧会过冲，alpha 必须夹住；位移过冲则是我们要的「滑过一点再回」。
                        alpha = enter.value.coerceIn(0f, 1f)
                        translationX = (1f - enter.value) * 40.dp.toPx()
                    }
                ) {
                    when (q.kind) {
                        QuizKind.SPELLING -> SpellingQuestion(q, state, vm, onSpeak)
                        QuizKind.TRANSLATION -> TranslationQuestion(q, state, vm)
                        QuizKind.CONFUSABLE -> ConfusableQuestion(q, state, vm)
                        else -> ChoiceQuestion(q, state, vm, onSpeak)
                    }
                }

                // 解析卡：揭晓时向下展开 + 淡入，像把卡片翻开来。
                AnimatedVisibility(
                    visible = state.revealed,
                    enter = fadeIn(Motion.effects()) + expandVertically(Motion.spatial()) { it },
                    // 与题干同属一个 key(index) 子树，换题时整棵被丢弃，退场动画没有机会
                    // 播放；此前它挂在 key 外面，退场那一帧读的是下一题的数据，等于把
                    // 下一题的答案播给玩家看。这里显式写 None 表明「无跨题退场」是有意为之。
                    exit = ExitTransition.None
                ) {
                    // AnimatedVisibility 的插槽按单子项布局，直接塞两个子项会叠在一起，
                    // 这里显式套一层 Column，让「间距 + 解析卡」正常纵向排列。
                    Column {
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
                                        if (ok) "译法全部选对" else "正确译法共${q.answerSet.size}项"
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
                                text = if (q.kind == QuizKind.TRANSLATION) "关键词${q.word.word}" else q.word.word,
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
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(modifier = Modifier.padding(16.dp)) {
            /*
             * 选义 / 易混词辨析题的判分时机与另外两种题型不同：
             * 点选项（OptionRow -> vm.selectOption）当场就判分并揭晓，
             * 因此不存在「选中了但还没提交」这个中间态 ——
             * `state.selected >= 0` 必然蕴含 `state.revealed`。
             *
             * 历史实现在这里写了一个 `else -> vm.selectOption(state.selected)` 分支，
             * 它永远不可达：按钮 enabled 的前提（selected >= 0）成立时 revealed 已为 true，
             * onClick 早在第一个分支就 nextQuestion 了。留着只会让人误以为
             * 这两类题也有「先选后交」的流程。这里显式收敛成「未揭晓时禁用」，
             * 文案也相应改成引导语，而不是一个永远点不动的「提交答案」。
             */
            val canSubmit = when {
                state.revealed -> true
                // 拼写题必须先有输入：此前这里恒为 true，
                // 误点一下「提交答案」就把空格判成错、白白废掉一题
                q.kind == QuizKind.SPELLING -> state.typed.isNotBlank()
                q.kind == QuizKind.TRANSLATION -> state.selectedSet.isNotEmpty()
                else -> false
            }
            Button(
                onClick = {
                    when {
                        state.revealed -> vm.nextQuestion()
                        q.kind == QuizKind.SPELLING -> vm.submitSpelling()
                        q.kind == QuizKind.TRANSLATION -> vm.submitTranslation()
                        else -> Unit
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = when {
                        state.revealed ->
                            if (state.index + 1 >= state.total) "查看成绩" else "下一题"
                        q.kind == QuizKind.TRANSLATION ->
                            if (state.selectedSet.isEmpty()) "请先选择译文" else "提交所选译法"
                        q.kind == QuizKind.SPELLING ->
                            if (state.typed.isBlank()) "请先拼写单词" else "提交答案"
                        else -> "请先选择答案"
                    },
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
    "选项${('A'.code + index).toChar()}：${text}"

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
    // 配色从「瞬间跳变」改成弹簧过渡：选中、判对、判错三种状态的切换
    // 都发生在手指抬起的那一帧，硬切换是最刺眼的「生硬」。
    val container by animateColorAsState(
        targetValue = when {
            revealed && isAnswer -> LocalAppColors.current.success.copy(alpha = 0.16f)
            revealed && selected -> LocalAppColors.current.danger.copy(alpha = 0.14f)
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = Motion.effects(),
        label = "optionContainer"
    )
    val content by animateColorAsState(
        targetValue = when {
            revealed && isAnswer -> LocalAppColors.current.success
            revealed && selected -> LocalAppColors.current.danger
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = Motion.effects(),
        label = "optionContent"
    )
    // 选中时轻微放大，给一个「拿起来」的手感；幅度控制在 2%，
    // 大了会挤压相邻行。
    val scale by animateFloatAsState(
        targetValue = if (selected && !revealed) 1.02f else 1f,
        animationSpec = Motion.spatialSnappy(),
        label = "optionScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            // 放在 padding 之后：缩放只作用于行内容，行占位高度不变，
            // 否则选中时整列选项会跟着上下抖动。
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
        // 行上已经有 selectable（它会合并子节点）+ 本行的 contentDescription，
        // 子节点必须从无障碍树里摘掉，否则 TalkBack 把「选项 A：apple」读完之后
        // 又把合并上来的「A」「apple」各读一遍。
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(content.copy(alpha = 0.16f))
                .clearAndSetSemantics {},
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
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {}
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
    val container by animateColorAsState(
        targetValue = when {
            revealed && isAnswer -> LocalAppColors.current.success.copy(alpha = 0.16f)
            revealed && checked -> LocalAppColors.current.danger.copy(alpha = 0.14f)
            checked -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = Motion.effects(),
        label = "multiContainer"
    )
    val content by animateColorAsState(
        targetValue = when {
            revealed && isAnswer -> LocalAppColors.current.success
            revealed && checked -> LocalAppColors.current.danger
            checked -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = Motion.effects(),
        label = "multiContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (checked && !revealed) 1.02f else 1f,
        animationSpec = Motion.spatialSnappy(),
        label = "multiScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
        // 动画值必须先在这里取 `by` 委托再传给 background()。
        // 写成 `animateColorAsState(...).value` 塞进 background() 的参数位，
        // 等于每帧都强制整行（含文案与勾选图标）跟着底色动画一起重组 ——
        // 本文件其它三处都是 `by`，只有这里漏了。
        val boxColor by animateColorAsState(
            targetValue = if (checked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
            animationSpec = Motion.effects(),
            label = "multiBox"
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(boxColor)
                .border(
                    1.5.dp,
                    if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 勾从 0.6 倍弹到 1 倍，勾选动作才有「咔哒」的确认感
            val checkScale by animateFloatAsState(
                targetValue = if (checked) 1f else 0.6f,
                animationSpec = Motion.spatialSnappy(),
                label = "multiCheck"
            )
            if (checked) {
                IconCheck(
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        },
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
            // 提示的是**字母数**，不是字符串长度：词库里有 22 条带连字符的复合词
            // （father-in-law 13 个字符、11 个字母），照 .length 报数会让人按 13 个
            // 字母去凑，而判分要求连字符原样写出，于是「数字对不上」看着像判分出错。
            text = buildString {
                append("根据中文释义与发音拼写单词（${q.word.word.count { it.isLetter() }}个字母")
                when {
                    '-' in q.word.word -> append("、含连字符")
                    ' ' in q.word.word -> append("、含空格")
                }
                append('）')
            },
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
    onOpenProfile: () -> Unit,
    vm: AppViewModel
) {
    val total = state.total
    val accuracy = percentOf(state.correctCount, total)
    val perfect = total > 0 && state.correctCount == total
    // 拓展资源练习（易混词辨析 / 真题句子翻译）不进排行榜，
    // 结算页的文案、按钮与退出去向都要与普通闯关区分开：
    // 它们从「我的」页进来，退出时就回「我的」——
    // 早先这里调 onExit()（即 popBackStack），而进入用的是 goTab + popUpTo(HOME)，
    // 返回栈是 [首页, 闯关]，于是点「返回」被带回**首页**，与本页文案不符。
    val isExtension = state.unit in EXTENSION_QUIZ_SCOPES
    // 错题重做：同样不进排行榜，但退出去向与拓展练习不同 —— 它是从排行榜进来的，
    // 结束/退出都要回排行榜，那里才有更新后的错题数量。
    val isRedo = state.isRedo
    fun leave() {
        vm.exitQuiz()
        if (isRedo) onOpenLeaderboard() else if (isExtension) onOpenProfile()
    }

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
                // 三星门槛是正确率 90%（见 AppViewModel.finishQuiz 的评星口径），
                // 不等于全对。此前这里对三星一律写「满分通关」，10 题对 9 题也被说成满分，
                // 而真正的满分成就要求 correct == total —— 文案与判分两套口径。
                perfect -> "太棒了，满分通关！"
                state.stars >= 3 -> "很强，只差一点就满分"
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
                            text = "分",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Tag(
                        text = "正确率${accuracy}%",
                        container = if (accuracy >= 80) LocalAppColors.current.cardMint else LocalAppColors.current.cardPeach,
                        contentColor = if (accuracy >= 80) LocalAppColors.current.success else LocalAppColors.current.warning
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultTile("答对", "${state.correctCount}/$total", Modifier.weight(1f))
                ResultTile("最高连击", "${state.maxCombo}", Modifier.weight(1f))
                ResultTile("用时", durationText(state.elapsedMs), Modifier.weight(1f))
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
        AppCard(
            color = if (isExtension || isRedo) LocalAppColors.current.cardSky
            else LocalAppColors.current.cardMint,
            padding = 16.dp
        ) {
            Text(
                text = when {
                    isRedo -> {
                        /*
                         * remainingWrong 由结算时回查错题集得到，-1 是「没数出来」的哨兵值。
                         * 早前这里 `if (remainingWrong >= 0) ... else 0` 把哨兵当成 0，
                         * 于是读库失败也理直气壮地播报「待重做的错题已经全部清完」——
                         * 一句没有依据的成功结论。数不出来就照实说数不出来。
                         */
                        val left = state.remainingWrong
                        when {
                            left == 0 ->
                                "本次是错题重做，${total}题里答对${state.correctCount}题，" +
                                    "待重做的错题已经全部清完。成绩不计入排行榜与成就。"
                            left < 0 ->
                                "本次是错题重做，答对的${state.correctCount}题已移出错题集，" +
                                    "剩余数量这次没统计出来，可回排行榜查看。成绩不计入排行榜与成就。"
                            else ->
                                "本次是错题重做，答对的${state.correctCount}题已移出错题集，" +
                                    "还剩${left}题待重做。成绩不计入排行榜与成就。"
                        }
                    }
                    isExtension -> "本次是拓展资源练习，成绩不计入排行榜排名，可以反复练到全对。"
                    else -> "本次成绩已记入「历次成绩排行榜」，可以随时和自己的历史最好成绩比较。"
                },
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
            onClick = { if (isExtension) leave() else { vm.exitQuiz(); onOpenLeaderboard() } },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                stringResource(
                    if (isExtension) R.string.action_back_practice
                    else R.string.action_view_leaderboard
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                isRedo -> "返回后可在排行榜看到还剩几道待重做"
                isExtension -> "返回后可再次进入拓展练习"
                state.letter != null -> "返回后可继续选择其他首字母"
                state.unit == QUIZ_SCOPE_ALL -> "返回后可重新挑战本级全部词汇"
                else -> "返回后可继续选择其他单元"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            /*
             * 这一行字本身就是「返回」按钮，但它是 labelSmall 的一行文本，
             * 不加处理时命中区只有文字那几个 dp 高，用户点旁边一点就没反应。
             * padding 放在 clickable **之后**：这样内外两层尺寸一致，
             * 空白边距也算命中区（反过来写 padding 在 clickable 之前，那段留白是不响应的）。
             * 再补 role = Role.Button，读屏才会念成「按钮」而不是一句普通说明。
             */
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { leave() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
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
