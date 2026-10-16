package com.hdu.cetvocab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.UnitItem
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.EmptyHint
import com.hdu.cetvocab.ui.components.IconBack
import com.hdu.cetvocab.ui.components.IconChevronRight
import com.hdu.cetvocab.ui.components.LevelDots
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.Motion
import kotlinx.coroutines.launch

@Composable
fun LearnScreen(
    vm: AppViewModel,
    onOpenUnit: (Int) -> Unit,
    onOpenWord: (Int) -> Unit,
    onStartFav: () -> Unit,
    onStartWrong: () -> Unit
) {
    val state by vm.learn.collectAsState()
    // 用 rememberSaveable：切到后台被系统回收后重建时，用户仍停在原来那个分段
    // 和「全部单词」浏览态，而不是被悄悄弹回单元列表。
    var tab by rememberSaveable { mutableStateOf(0) }
    var showingAllWords by rememberSaveable { mutableStateOf(false) }

    /*
     * 级别一变就退出「全部单词」浏览态。
     *
     * showingAllWords 是 saveable 的，而跨级别刷新时 ViewModel 会刻意把
     * allWords 清空（留着四级的词表显示在六级页面上是数据错误）。两者一叠加：
     * 在词库页进了「预览全部」→ 回首页切到另一级别 → 再进词库页，
     * 看到的就是「全部 0 个单词」+ 空列表 + 空字母条，而唯一的入口卡片
     * 又被 AnimatedVisibility(!showingAllWords) 藏了起来 —— 用户只能退到首页重来。
     *
     * 必须只在「真的换了级别」时重置：LaunchedEffect 在首次组合也会跑一次，
     * 无条件重置会让进程被回收后重建时刚恢复出来的 tab / 浏览态又被抹掉，
     * 等于用一个体验问题换另一个。
     *
     * 两个「假的变更」要分开挡：
     * · state.loading 期间 state.stage 是快照的**默认级别**（四级），六级用户
     *   进词库页时它会从 1 变成 2，这不是用户切换；
     * · 首次认值（lastStage 还是 -1）也不算切换，否则同一件事会再触发一次。
     * 初值刻意用 -1 而不是 state.stage：用当前值当初始值就等于把
     * 「冷启动那一刻的默认级别」记成用户的选择，六级用户每次进词库页
     * 都会被上面那段逻辑抹掉刚恢复出来的浏览态。
     */
    var lastStage by rememberSaveable { mutableStateOf(-1) }
    LaunchedEffect(state.stage, state.loading) {
        if (state.loading) return@LaunchedEffect
        if (lastStage < 0) {
            lastStage = state.stage
            return@LaunchedEffect
        }
        if (lastStage != state.stage) {
            lastStage = state.stage
            showingAllWords = false
            tab = 0
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---------- 顶部：阶段 + 搜索 ----------
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${WordDb.stageLabel(state.stage)}词库",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        // 不再写死「每单元 50 词」：末单元只装得下余数（实测四级末单元 48 词），
                        // 写死会与下面的单元列表对不上。改为直接报真实单元数。
                        text = "共${state.totalWords}词、已学${state.learnedWords}词、" +
                            "${state.units.size}个单元",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // 进入「预览全部」后这张卡本身就没意义了 —— 用户已经在预览全部里，
            // 提示「点浏览全部去看全词表」是让人绕出去。
            // 搜索框仍保留，用户可以从这里直接搜某个单词跳到详情。
            AnimatedVisibility(
                visible = !showingAllWords,
                // 高度塌陷要有一个明确方向：卡片从底部消失、从顶部冒出，
                // 配合淡入淡出，比单纯 fade 更像「抽走一张纸」。
                enter = fadeIn(Motion.effects()) +
                    expandVertically(Motion.spatial(), expandFrom = Alignment.Top),
                exit = fadeOut(Motion.effects()) +
                    shrinkVertically(Motion.spatial(), shrinkTowards = Alignment.Top),
                label = "previewAllCard"
            ) {
                Column {
                    AppCard(color = LocalAppColors.current.cardPeach, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "预览全部单词",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "像好友列表一样按字母快速浏览${state.totalWords}个单词",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = {
                                showingAllWords = true
                                vm.previewAllWords()
                            }) {
                                Text(stringResource(R.string.action_browse_all))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.search(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.hint_search)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        Text(
                            text = "清除",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(role = Role.Button) { vm.search("") }
                                .padding(end = 12.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // ---------- 搜索结果 ----------
        if (state.query.isNotBlank()) {
            if (state.searchResult.isEmpty()) {
                EmptyHint("没有找到匹配的单词", hint = "换个关键词试试，释义里的中文也能搜到")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 命中数等于 WordDb.SEARCH_LIMIT 时后面还有词条没显示出来
                    // （搜常见字母极易触发）。不写这一行，用户会以为整个级别只有这 60 个词。
                    item(key = "search_count") {
                        Text(
                            text = if (state.searchTruncated)
                                "匹配较多，按字母序显示前${WordDb.SEARCH_LIMIT}个单词，输入更完整的拼写可缩小范围"
                            else "匹配${state.searchResult.size}个单词",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }
                    items(state.searchResult, key = { it.id }) { w ->
                        WordRow(w = w, level = state.levels[w.id] ?: 0, onClick = { onOpenWord(w.id) })
                    }
                }
            }
            return
        }

        if (showingAllWords) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { showingAllWords = false }
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBack(
                        modifier = Modifier.size(15.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "返回词库",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "全部${state.allWords.size}个单词",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.allWordsLoading) {
                EmptyHint("正在加载全部单词…")
            } else {
                // 分组结果必须缓存：LazyColumn 滚动会频繁重组，几千个词每次重新分组会明显掉帧
                val groups = remember(state.allWords) {
                    state.allWords
                        .groupBy { it.word.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
                        .toSortedMap()
                }
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                // 每个字母分组在列表中的起始下标（每组 = 1 个字母头 + N 个词条）
                val letterStarts = remember(groups) {
                    val map = LinkedHashMap<String, Int>()
                    var idx = 0
                    groups.forEach { (letter, words) ->
                        map[letter] = idx
                        idx += words.size + 1
                    }
                    map
                }
                val letters = letterStarts.keys.toList()
                // key 必须是 letterStarts 而不是 letters：侧边字母条算「当前高亮哪个字母」
                // 读的是**下标表**，而重新装载词表后字母集合往往一模一样（还是那 26 个字母）、
                // 每组词数却变了。用 letters 当 key 会让 derivedStateOf 一直持有旧的
                // 下标表，滚动时高亮按老位置跳，指到错误的字母上。
                val currentLetter by remember(letterStarts) {
                    derivedStateOf {
                        val idx = listState.firstVisibleItemIndex
                        var cur = letters.firstOrNull()
                        for (l in letters) {
                            if ((letterStarts[l] ?: 0) <= idx) cur = l else break
                        }
                        cur
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 34.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        groups.forEach { (letter, words) ->
                            item(key = "letter-$letter") {
                                Text(
                                    text = letter,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(words, key = { "all-${it.id}" }) { word ->
                                WordRow(
                                    w = word,
                                    level = state.levels[word.id] ?: 0,
                                    onClick = { onOpenWord(word.id) }
                                )
                            }
                        }
                    }
                    LetterIndexBar(
                        letters = letters,
                        current = currentLetter,
                        onPick = { letter ->
                            letterStarts[letter]?.let { idx ->
                                scope.launch { listState.scrollToItem(idx) }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            return
        }

        // ---------- 分段 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp)
        ) {
            listOf("单元", "生词本${state.favWords.size}", "错词本${state.wrongWords.size}")
                .forEachIndexed { i, label ->
                    val selected = tab == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { tab = i }
                            )
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
        }

        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> UnitList(state.units, onOpenUnit)
            1 -> WordListOrEmpty(
                words = state.favWords,
                levels = state.levels,
                empty = "还没有收藏的生词",
                hint = "学习时点击右上角的心形按钮即可加入生词本",
                actionLabel = if (state.favWords.isEmpty()) null else "开始复习生词",
                onAction = onStartFav,
                onOpenWord = onOpenWord
            )
            else -> WordListOrEmpty(
                words = state.wrongWords,
                levels = state.levels,
                empty = "暂时没有错词记录",
                hint = "答错的单词会自动进入这里",
                actionLabel = if (state.wrongWords.isEmpty()) null else "开始强化错词",
                onAction = onStartWrong,
                onOpenWord = onOpenWord
            )
        }
    }
}

@Composable
private fun UnitList(
    units: List<UnitItem>,
    onOpenUnit: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(units, key = { it.unit }) { u ->
            AppCard(
                padding = 14.dp,
                color = if (u.done) LocalAppColors.current.cardMint else MaterialTheme.colorScheme.surface
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (u.done) LocalAppColors.current.success.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${u.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (u.done) LocalAppColors.current.success else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "第${u.unit}单元",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (u.done) {
                                Spacer(Modifier.width(6.dp))
                                Tag(
                                    text = "已学完",
                                    container = LocalAppColors.current.success.copy(alpha = 0.16f),
                                    contentColor = LocalAppColors.current.success
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${u.fromWord} ~ ${u.toWord}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(7.dp))
                        LinearProgressIndicator(
                            progress = { u.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (u.done) LocalAppColors.current.success else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${u.learned}/${u.total}词",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    PillButton(
                        text = "学习",
                        container = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) { onOpenUnit(u.unit) }
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun WordListOrEmpty(
    words: List<Word>,
    levels: Map<Int, Int>,
    empty: String,
    hint: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onOpenWord: (Int) -> Unit
) {
    if (words.isEmpty()) {
        EmptyHint(empty, hint = hint)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (actionLabel != null) {
            item {
                AppCard(color = LocalAppColors.current.cardRose, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "共${words.size}个单词",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = actionLabel,
                            container = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = onAction
                        )
                    }
                }
            }
        }
        items(words, key = { it.id }) { w ->
            WordRow(w = w, level = levels[w.id] ?: 0, onClick = { onOpenWord(w.id) })
        }
    }
}

@Composable
private fun WordRow(w: Word, level: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = w.word,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (w.phonetic.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = w.phonetic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = w.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        // 掌握度圆点此前硬编码 0，列表里永远显示「未学」，
        // 用户无法一眼分辨生词熟词。等级由 LearnUiState.levels 下发。
        LevelDots(level = level)
        Spacer(Modifier.width(6.dp))
        IconChevronRight(
            modifier = Modifier.size(15.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 右侧 A–Z 快速跳转条（通讯录样式）。
 * 点击字母把列表滚到该字母分组；滚动时高亮当前所在字母。
 */
@Composable
private fun LetterIndexBar(
    letters: List<String>,
    current: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * 26 个字母 × 16dp 加内边距约 426dp：在小屏上放得下，一旦用户开了系统字体放大
     * （1.3 倍时行高跟着涨）或设备本身只有 5 寸，底部几个字母会被父容器直接裁掉 ——
     * 被裁掉的字母既看不见也点不到，等于 Z 开头的词无法快速跳转。
     * 这里给条本身一个可滚容器：装得下时表现与原来完全一致，装不下时才能滚。
     */
    Column(
        modifier = modifier
            .padding(end = 3.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val active = letter == current
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 18.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onPick(letter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    // 必须显式收紧行高：只写 fontSize 会继承 Material3 的 24sp 行高，
                    // 字形会被顶到 16dp 色块的下半部，导致高亮块看起来没对齐。
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
