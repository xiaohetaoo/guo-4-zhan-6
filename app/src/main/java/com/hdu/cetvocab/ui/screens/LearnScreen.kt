package com.hdu.cetvocab.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.UnitItem
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.EmptyHint
import com.hdu.cetvocab.ui.components.LevelDots
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun LearnScreen(
    vm: AppViewModel,
    onOpenUnit: (Int) -> Unit,
    onOpenWord: (Int) -> Unit,
    onStartFav: () -> Unit,
    onStartWrong: () -> Unit,
    onQuizUnit: (Int) -> Unit
) {
    val state by vm.learn.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

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
                        text = "共 ${state.totalWords} 词 · 已学 ${state.learnedWords} 词 · 每单元 50 词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.search(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索单词或中文释义") },
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
                                .clickable { vm.search("") }
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
                EmptyHint("没有找到匹配的单词", hint = "试试只输入前几个字母")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.searchResult, key = { it.id }) { w ->
                        WordRow(w = w, onClick = { onOpenWord(w.id) })
                    }
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
            listOf("单元", "生词本 ${state.favWords.size}", "错词本 ${state.wrongWords.size}")
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
                            .clickable { tab = i }
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
            0 -> UnitList(state.units, onOpenUnit, onQuizUnit)
            1 -> WordListOrEmpty(
                words = state.favWords,
                empty = "还没有收藏的生词",
                hint = "学习时点击 ♥ 即可加入生词本",
                actionLabel = if (state.favWords.isEmpty()) null else "开始复习生词",
                onAction = onStartFav,
                onOpenWord = onOpenWord
            )
            else -> WordListOrEmpty(
                words = state.wrongWords,
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
    onOpenUnit: (Int) -> Unit,
    onQuizUnit: (Int) -> Unit
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
                                text = "第 ${u.unit} 单元",
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
                            text = "${u.learned} / ${u.total} 词",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PillButton(
                            text = "学习",
                            container = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) { onOpenUnit(u.unit) }
                        Spacer(Modifier.height(6.dp))
                        PillButton(
                            text = "闯关",
                            container = LocalAppColors.current.cardPeach,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) { onQuizUnit(u.unit) }
                    }
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
            .clickable { onClick() }
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
                            text = "共 ${words.size} 个单词",
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
            WordRow(w = w, onClick = { onOpenWord(w.id) })
        }
    }
}

@Composable
private fun WordRow(w: Word, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
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
        LevelDots(level = 0)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "›",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
