package com.hdu.cetvocab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.percentOf
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.ExampleBlock
import com.hdu.cetvocab.ui.components.IconArrowUp
import com.hdu.cetvocab.ui.components.IconClose
import com.hdu.cetvocab.ui.components.IconHeart
import com.hdu.cetvocab.ui.components.IconTrophy
import com.hdu.cetvocab.ui.components.LevelDots
import com.hdu.cetvocab.ui.components.MnemonicBlock
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.SimilarBlock
import com.hdu.cetvocab.ui.components.SpeakButton
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.TutorialAnchor
import com.hdu.cetvocab.ui.components.TutorialOverlay
import com.hdu.cetvocab.ui.components.TutorialStep
import com.hdu.cetvocab.ui.components.tutorialAnchor
import com.hdu.cetvocab.ui.components.WordHeadline
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.ui.theme.springPress
import kotlin.math.abs
import com.hdu.cetvocab.ui.components.HitArea

@Composable
fun StudyScreen(
    vm: AppViewModel,
    speak: (String) -> Unit,
    speakAuto: (String) -> Unit,
    onFinish: () -> Unit
) {
    val state by vm.study.collectAsState()
    val closeLabel = stringResource(R.string.action_close)
    val closeInteraction = remember { MutableInteractionSource() }

    /*
     * 新手教程的五个定位锚点。
     *
     * 顺序与 AppViewModel.TUTORIAL_STEP_COUNT 的注释一一对应：
     * 0 顶部进度 → 1 单词主卡 → 2 发音键 → 3 收藏键 → 4 底部按钮组。
     *
     * 这里把锚点建成一个列表而不是五个具名变量：
     * 教程层按 tutorialStep 下标取，新增一步时只改这一处与那份步骤文案，
     * 不会出现「加了步骤但忘了少写一个锚点」这类错位。
     */
    val anchors = remember {
        List(vm.tutorialStepCount) { TutorialAnchor() }
    }
    val tutorialSteps = remember(anchors) { buildTutorialSteps(anchors) }

    // 学习时长只累计「本页在前台」的时间，具体计时由 ViewModel 维护：
    // 它同时知道应用是否退到了后台，能把切出去的那段时间排除掉。
    // 历史实现是在这里记下进入时刻、onDispose 时一次性结算，
    // 中途切页面 / 退后台的时间会被一起算进去，导致累计时长虚高。
    DisposableEffect(Unit) {
        vm.onStudyScreenEnter()
        onDispose { vm.onStudyScreenExit() }
    }

    // 拦截系统返回键，统一走 onFinish。
    // 此前没有拦截：按返回键会直接 pop，但 vm.finishStudy() 不会被调用 ——
    // 学习态残留、首页/词库/我的三页数据都不刷新，
    // 下次进入学习页还会看到上一次「本组学习完成」的结算页。
    // 页面顶部的关闭按钮走的是 onFinish，两条退出路径现在行为一致。
    BackHandler { onFinish() }

    // 切换单词时自动发音：走 speakAuto 入口，内部统一遵守「自动朗读」开关
    LaunchedEffect(state.index, state.finished) {
        val w = state.current
        if (w != null) speakAuto(w.word)
    }

    if (state.finished) {
        StudySummary(state.knownCount, state.unknownCount, state.newAchievements, onFinish)
        return
    }

    val word = state.current
    if (word == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.study_empty_queue),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onFinish) { Text(stringResource(R.string.action_back)) }
            }
        }
        return
    }

    /*
     * 整页装在一个 Box 里：学习内容在底层，新手教程的遮罩叠在顶层。
     *
     * 教程必须盖住顶栏、卡片与底栏三者，而它们是 Column 的兄弟节点 ——
     * 只有把教程层摆在与 Column 同级的后一个位置，才能真的盖住它们。
     * 反过来若把 TutorialOverlay 塞进 Column 内部，它只会占据剩余空间，
     * 顶栏与底栏会露在遮罩外面，高亮圈的坐标也会跟着错位。
     */
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // ---------- 顶部进度 ----------
        // 关闭键热区从 34 撑到 44（K13），上下内边距从 12 收到 7 等量吸收，
        // 这一栏仍是 58dp；芯片贴行首（CenterStart）+ 间距 12→2，标题位置不变。
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 7.dp)
                // 教程第 0 步讲「这是第几个词、哪个级别」，框住整条进度栏
                .then(
                    if (state.tutorialActive) Modifier.tutorialAnchor(anchors[0]) else Modifier
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HitArea(
                    onClick = onFinish,
                    interaction = closeInteraction,
                    min = 44.dp,
                    align = Alignment.CenterStart,
                    contentDescription = closeLabel
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .springPress(closeInteraction, pressedScale = 0.88f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        IconClose(
                            modifier = Modifier.size(15.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
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
                    // 与底部按钮同名，也和结算页的两块统计同名。
                    text = "认识${state.knownCount}",
                    container = LocalAppColors.current.cardMint,
                    contentColor = LocalAppColors.current.success
                )
                Spacer(Modifier.width(6.dp))
                Tag(
                    text = "待加强${state.unknownCount}",
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
            /*
             * 翻卡用「绕 Y 轴真翻转」，不用 Crossfade。
             *
             * Crossfade 是正反两面同时淡入淡出，中途两张卡各半透明地叠在一起——
             * 既不像翻卡，又会因为两面高度不同而在叠合瞬间抖动。
             *
             * 这里让角度从 0° 连续走到 180°：
             *   0°～90°   显示正面，rotationY = -角度（正面往外转）
             *   90°～180° 显示背面，rotationY = 180 - 角度（背面从侧立转正）
             * 两面在 90° 处都是「侧立」状态（投影宽度为 0），所以换面的那一帧
             * 用户看不到任何跳变，不需要额外的淡入淡出来遮。
             */
            val flipAngle = remember { Animatable(0f) }
            var lastIndex by remember { mutableIntStateOf(state.index) }
            LaunchedEffect(state.index, state.flipped) {
                if (state.index != lastIndex) {
                    // 换词一定回到正面，而且直接落位、不播翻卡——
                    // 否则每答一题都要先「翻回来」再显示下一个词，白等 200 多毫秒。
                    lastIndex = state.index
                    flipAngle.snapTo(0f)
                } else {
                    flipAngle.animateTo(if (state.flipped) 180f else 0f, Motion.flip())
                }
            }
            // 侧立时轻微缩一下，模拟卡片被捏起来翻过去，落位时再回到原尺寸。
            val edge = (1f - abs(flipAngle.value - 90f) / 90f).coerceIn(0f, 1f)
            val dip = 1f - 0.04f * edge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = if (flipAngle.value <= 90f) -flipAngle.value
                        else 180f - flipAngle.value
                        // 不设相机距离的话，绕 Y 轴旋转会退化成「横向压扁」，毫无立体感。
                        // 14 × 像素密度是手机屏幕上比较自然的透视强度。
                        cameraDistance = 14f * density
                        scaleX = dip
                        scaleY = dip
                    }
            ) {
                if (flipAngle.value <= 90f) {
                    CardFront(
                        word = word,
                        speak = speak,
                        vm = vm,
                        fav = state.favIds.contains(word.id),
                        level = state.currentLevel,
                        // 教程第 2 步框发音键、第 3 步框收藏键；
                        // 传 null 时这两个控件不挂定位修饰符，正常使用时零开销。
                        wordBlockAnchor = if (state.tutorialActive) anchors[1] else null,
                        speakAnchor = if (state.tutorialActive) anchors[2] else null,
                        favAnchor = if (state.tutorialActive) anchors[3] else null
                    )
                } else {
                    CardBack(word, speak)
                }
            }
        }

        // ---------- 底部操作 ----------
        Column(
            modifier = Modifier
                .padding(16.dp)
                // 教程最后一步讲「点这里查看释义，再选认识或待加强」，框住整组按钮
                .then(
                    if (state.tutorialActive) {
                        Modifier.tutorialAnchor(anchors[vm.tutorialStepCount - 1])
                    } else Modifier
                )
        ) {
            // 「查看释义」与「认识 / 待加强」两组按钮的切换也走弹簧：
            // 原先是一帧之内直接换掉，跟上面翻卡的动画节奏完全脱节。
            AnimatedContent(
                targetState = state.flipped,
                transitionSpec = {
                    (fadeIn(Motion.effects()) +
                        slideInVertically(Motion.spatialSnappy()) { it / 3 })
                        .togetherWith(
                            fadeOut(Motion.effects()) +
                                slideOutVertically(Motion.spatialSnappy()) { -it / 3 }
                        )
                },
                label = "studyActions"
            ) { flipped ->
                if (!flipped) {
                    Button(
                        onClick = { vm.flipCard() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(stringResource(R.string.study_reveal), fontWeight = FontWeight.SemiBold)
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
                            Text(stringResource(R.string.study_unknown), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { vm.answerCurrent(known = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.success)
                        ) {
                            Text(stringResource(R.string.study_known), fontWeight = FontWeight.SemiBold)
                        }
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
            /*
             * 返回上一个词。
             *
             * 只在「已经答过至少一个」且不在结算页时出现 —— 第一个词没有上一个，
             * 结算页则已经落库并刷新了首页（见 AppViewModel.stepBack 注释）。
             * 用无底色的文字按钮而不是整块按钮：它是次要操作，
             * 不该和下方 52dp 的主按钮抢注意力。
             *
             * 位置放在这一行而不是顶栏进度条里：顶栏整条是教程第 0 步的锚点，
             * 往里加控件会把已经验收过的热区与布局尺寸改掉（v1.4.7 的教训）。
             *
             * 高度给足 44dp 可点区。interaction 记得自己开一个 ——
             * 复用关闭键那个 source 会让两处的涟漪同时亮。
             */
            if (vm.canStepBack) {
                val backInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(
                            interactionSource = backInteraction,
                            indication = LocalIndication.current,
                            onClick = { vm.stepBack() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "← 返回上一个词",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }

        /*
         * 新手教程遮罩。放在 Column 之后（同一个 Box 的第二个子节点），
         * 才能盖住顶栏、卡片与底栏三者，高亮洞按锚点上报的坐标挖出来。
         *
         * 只在 tutorialStep 非 null 时渲染；跳过后整个 Composable 直接返回，
         * 不留下空转的 Canvas 或额外的手势拦截层。
         */
        val tutorialStep = state.tutorialStep
        TutorialOverlay(
            step = tutorialStep?.let { tutorialSteps.getOrNull(it) },
            stepIndex = tutorialStep ?: 0,
            stepCount = vm.tutorialStepCount,
            onNext = { vm.tutorialNext() },
            onSkip = { vm.tutorialSkip() }
        )
    }
}

/**
 * 新手教程的五步文案 + 各自要框住的锚点。
 *
 * 写作口径（与全站一致，改文案时照这个来）：
 * - 说「你会看到什么、点一下会发生什么」，不解释实现（不出现「队列」「SM-2」）；
 * - 每步只讲一件事，句子控制在两行内 —— 气泡是盖在卡片上的，太长会挡住被讲解的控件；
 * - 不复述界面上的字（「这里有一个叫『查看释义』的按钮」是废话），而是讲**为什么**；
 * - 五步走完用户就该知道：这里是第几个词、这张卡能干什么、点哪里开始背。
 *
 * [anchors] 的下标必须与 [com.hdu.cetvocab.ui.AppViewModel] 的 TUTORIAL_STEP_COUNT
 * 注释里的顺序一致：0 顶部进度 / 1 单词卡 / 2 发音键 / 3 收藏键 / 4 底部按钮组。
 */
private fun buildTutorialSteps(
    anchors: List<TutorialAnchor>
): List<TutorialStep> = listOf(
    TutorialStep(
        title = "这是今天的第几个词",
        body = "顶部写着本组进度：一共几个词、已经过掉几个。" +
            "左边那个叉随时可以退出，进度会自动保存。",
        target = anchors.getOrNull(0)
    ),
    TutorialStep(
        title = "先自己想，再翻面",
        body = "正面只有单词和音标，先在心里回忆词义；" +
            "背面是释义、例句、词根词缀和联想记忆，帮你记牢它。",
        // 只框「单词 + 音标」这一块，不框整张卡。
        //
        // 原先框整张卡，实测洞从 y≈363 一直到 2311（屏幕 2560），
        // 上下只剩 91dp / 62dp —— 气泡本身约 100dp 高，两边都塞不下，
        // 只能压在卡片下半部，正好盖住这一步要讲的熟练度圆点与「查看释义」。
        // 教程盖住自己正在讲的东西，等于没讲。缩小到这一步真正指的内容之后，
        // 下方腾出大半屏，气泡就落在洞外了。
        target = anchors.getOrNull(1)
    ),
    TutorialStep(
        title = "不确定怎么读就点它",
        body = "喇叭使用系统自带的离线语音，不联网也能读。" +
            "想让它读慢一点，可以到设置里调语速。",
        target = anchors.getOrNull(2)
    ),
    TutorialStep(
        title = "重要的词收藏起来",
        body = "点这颗心把词加进生词本，之后在词库页可以只复习生词。" +
            "卡片底部那排小圆点是这个词的熟练度。",
        target = anchors.getOrNull(3)
    ),
    TutorialStep(
        title = "回忆完就翻卡自评",
        body = "点「查看释义」翻到背面，然后如实选择「认识」或「待加强」。" +
            "选「待加强」的词会排得更早再出现一次。",
        target = anchors.getOrNull(4)
    )
)

@Composable
private fun CardFront(
    word: Word,
    speak: (String) -> Unit,
    vm: AppViewModel,
    fav: Boolean,
    level: Int,
    /** 教程第 1 步要框的「单词 + 音标」块；null 表示不在教程里 */
    wordBlockAnchor: TutorialAnchor? = null,
    /** 教程第 2 步要框的发音键；null 表示不在教程里 */
    speakAnchor: TutorialAnchor? = null,
    /** 教程第 3 步要框的收藏键；null 表示不在教程里 */
    favAnchor: TutorialAnchor? = null
) {
    val favLabel = stringResource(
        if (fav) R.string.cd_fav_remove else R.string.cd_fav_add
    )
    val favInteraction = remember { MutableInteractionSource() }
    AppCard(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        padding = 24.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(text = "${com.hdu.cetvocab.data.WordDb.stageLabel(word.stage)}、第${word.unit}单元")
            Spacer(Modifier.weight(1f))
            // 收藏芯片 38dp 太小（K13）。热区补到 44dp、贴右对齐，
            // 芯片仍停在卡片右缘；这一行上面有 weight(1f) 的弹性空隙，
            // 多出的 6dp 由它吸收，卡片里其它内容不动。
            //
            // 教程锚点挂在外层**显式尺寸**的 Box 上，理由同下面那颗喇叭：
            // wrapContentSize 在这里不起作用（父 Row 不给子项宽度约束），
            // 实测会报出 44×300dp 的竖长空框。尺寸必须与 HitArea 的 min 一致。
            val favSize = 44.dp
            Box(Modifier.size(favSize)) {
                HitArea(
                    onClick = { vm.toggleFavCurrent() },
                    interaction = favInteraction,
                    min = favSize,
                    align = Alignment.CenterEnd,
                    contentDescription = favLabel,
                    // 教程第 3 步框这颗心形
                    modifier = if (favAnchor != null) {
                        Modifier.tutorialAnchor(favAnchor)
                    } else Modifier
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .springPress(favInteraction, pressedScale = 0.86f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (fav) LocalAppColors.current.cardAmber
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconHeart(
                            modifier = Modifier.size(19.dp),
                            filled = fav,
                            color = if (fav) LocalAppColors.current.warning
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /*
             * 教程第 1 步框「单词 + 音标」这一块，**不含**下面的发音键。
             *
             * 三点说明：
             * ① 不框整张卡：整张卡从 y≈363 到 2311（屏高 2560），上下只剩
             *    91dp / 62dp，而气泡本身约 100dp 高 —— 两边都塞不下，只能压在
             *    卡片下半部，正好盖住这一步要讲的熟练度圆点与「查看释义」。
             *    缩小到真正指的内容之后，下方腾出大半屏，气泡落在洞外。
             * ② 不包含发音键：那是第 2 步（anchors[2]）的目标，两个洞互相包含
             *    会让第 1 步看起来「什么都没缩」，也会让第 2 步的聚焦感消失。
             * ③ 挂在显式尺寸的 Column 外层 Box 上，理由同下面那颗喇叭 ——
             *    fillMaxWidth 的容器会被父级撑满，锚点会报出整行宽。
             */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (wordBlockAnchor != null) {
                    Modifier.tutorialAnchor(wordBlockAnchor)
                } else Modifier
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
            }
            Spacer(Modifier.height(20.dp))
            /*
             * 锚点必须挂在**显式尺寸**的 Box 上。
             *
             * 这里踩过两次坑，记下来免得再犯：
             * ① 直接把 tutorialAnchor 传给 SpeakButton 不行 —— 它内部是
             *    `modifier.size(min)` 再套一层 size(size) 的圆，而这个 modifier 是画在
             *    内层圆上的，锚点却挂到了外层 Box 上，上报的是整条槽位。
             * ② 外面包一层 `Modifier.wrapContentSize()` 也不行 —— 实测仍然报 78×198dp。
             *    原因是父级 Column 的 horizontalAlignment = CenterHorizontally
             *    **只影响摆放位置、不约束子项宽度**，子项拿到的最大约束仍是整列宽；
             *    wrapContentSize 在「已被撑满」的测量里没有可收缩的信息。
             *
             * 真正可靠的做法是给 Box 一个确定的 size：约束一确定，
             * boundsInRoot() 报出来的就是这颗 56dp 的圆，与视觉完全一致。
             * 这个尺寸必须与下面 SpeakButton 的 size 保持一致，改一处要连另一处一起改。
             */
            val speakSize = 56.dp
            Box(Modifier.size(speakSize)) {
                SpeakButton(
                    text = word.word,
                    onSpeak = speak,
                    size = speakSize,
                    modifier = if (speakAnchor != null) {
                        Modifier.tutorialAnchor(speakAnchor)
                    } else Modifier
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 此前这里硬编码 0，用户在学习卡正面永远看不到自己对这个词的掌握程度
            LevelDots(level = level)
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
    val rate = percentOf(known, total)
    // 结算页此前是「学完最后一题瞬间硬切」，没有任何过渡。
    // 这里给它一个「从 92% 弹到 100% + 淡入」的入场，收束感更完整。
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, Motion.spatial()) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // 弹簧会轻微过冲，alpha 必须夹回 1，否则会触发一次无谓的亮度跳变。
                alpha = enter.value.coerceIn(0f, 1f)
                val s = 0.92f + 0.08f * enter.value
                scaleX = s
                scaleY = s
            }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (rate >= 80) {
                IconTrophy(
                    modifier = Modifier.size(62.dp),
                    color = LocalAppColors.current.gold
                )
            } else {
                IconArrowUp(
                    modifier = Modifier.size(62.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "本组学习完成",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "共${total}个单词、认识${rate}%",
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
                Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold)
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
