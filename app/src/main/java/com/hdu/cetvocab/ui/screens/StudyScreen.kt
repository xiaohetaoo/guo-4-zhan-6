package com.hdu.cetvocab.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import com.hdu.cetvocab.ui.components.WordHeadline
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.Motion
import com.hdu.cetvocab.ui.theme.springPress
import kotlin.math.abs

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

    // 学习时长只累计「本页在前台」的时间，具体计时由 ViewModel 维护：
    // 它同时知道应用是否退到了后台，能把切出去的那段时间排除掉。
    // 历史实现是在这里记下进入时刻、onDispose 时一次性结算，
    // 中途切页面 / 退后台的时间会被一起算进去，导致累计时长虚高。
    DisposableEffect(Unit) {
        vm.onStudyScreenEnter()
        onDispose { vm.onStudyScreenExit() }
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
        // ---------- 顶部进度 ----------
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
                            onClick = onFinish
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
                        level = state.currentLevel
                    )
                } else {
                    CardBack(word, speak)
                }
            }
        }

        // ---------- 底部操作 ----------
        Column(modifier = Modifier.padding(16.dp)) {
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
        }
    }
}

@Composable
private fun CardFront(
    word: Word,
    speak: (String) -> Unit,
    vm: AppViewModel,
    fav: Boolean,
    level: Int
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
            Tag(text = "${com.hdu.cetvocab.data.WordDb.stageLabel(word.stage)} · 第 ${word.unit} 单元")
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .springPress(favInteraction, pressedScale = 0.86f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (fav) LocalAppColors.current.cardAmber else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(
                        interactionSource = favInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { vm.toggleFavCurrent() }
                    )
                    .semantics { contentDescription = favLabel },
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
    val rate = if (total == 0) 0 else known * 100 / total
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
