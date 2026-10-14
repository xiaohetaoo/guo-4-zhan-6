package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.CetVocabApp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.Word
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.AppTopBar
import com.hdu.cetvocab.ui.components.ExampleBlock
import com.hdu.cetvocab.ui.components.IconHeart
import com.hdu.cetvocab.ui.components.LevelDots
import com.hdu.cetvocab.ui.components.MnemonicBlock
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.components.SimilarBlock
import com.hdu.cetvocab.ui.components.SpeakButton
import com.hdu.cetvocab.ui.components.Tag
import com.hdu.cetvocab.ui.components.WordHeadline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hdu.cetvocab.ui.theme.LocalAppColors
import com.hdu.cetvocab.ui.theme.springPress

@Composable
fun WordDetailScreen(
    vm: AppViewModel,
    wordId: Int,
    speak: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { (context.applicationContext as CetVocabApp).repo }

    val word by produceState<Word?>(initialValue = null, wordId) {
        value = withContext(Dispatchers.IO) { repo.wordDb.wordById(wordId) }
    }
    var fav by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(0) }

    LaunchedEffect(wordId) {
        withContext(Dispatchers.IO) {
            val st = repo.userDb.stateOf(wordId)
            fav = st.fav
            level = st.level
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "单词详情",
            // rank 是「按字母排序后的序号」（tools/build_words_db.py 里 rank = i + 1），
            // 不是词频也不是大纲编号；写成「第 N 词」会被读成「第 N 高频词」。
            subtitle = word?.let {
                "${WordDb.stageLabel(it.stage)} · 第 ${it.unit} 单元 · 字母序第 ${it.rank} 位"
            } ?: "",
            onBack = onBack,
            trailing = {
                // 心形是自绘图标、没有内置语义，必须由外层容器给出朗读文本
                val favLabel = stringResource(
                    if (fav) R.string.cd_fav_remove else R.string.cd_fav_add
                )
                val favInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .springPress(favInteraction, pressedScale = 0.86f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (fav) LocalAppColors.current.cardAmber else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = favInteraction,
                            indication = null,
                            role = Role.Button
                        ) {
                            word?.let {
                                fav = !fav
                                vm.toggleFav(it)
                            }
                        }
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
        )

        val w = word
        if (w == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.state_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            AppCard(padding = 20.dp) {
                WordHeadline(word = w, onSpeak = speak, wordSize = 32)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (w.pos.isNotBlank()) {
                        Tag(text = w.pos, container = LocalAppColors.current.cardPeach, contentColor = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Tag(
                        // 不写「大纲词汇」：词表来自 ECDICT 的 cet4/cet6 社区标签并上教材词表，
                        // 并未逐词对照官方大纲，标签措辞与设置页「数据来源」保持同一说法。
                        text = WordDb.stageLabel(w.stage) + "词表",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    LevelDots(level = level)
                }
            }

            Spacer(Modifier.height(14.dp))

            AppCard {
                SectionTitle(text = "释义")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = w.translation,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (w.transDetail.isNotBlank() && w.transDetail != w.translation) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        w.transDetail.split("\n").filter { it.isNotBlank() }.forEach {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
                if (w.definition.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "英文释义",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = w.definition,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (w.sentence.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                AppCard {
                    SectionTitle(text = "例句", accent = LocalAppColors.current.success)
                    Spacer(Modifier.height(10.dp))
                    ExampleBlock(word = w, onSpeak = speak)
                }
            }

            if (w.roots.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                AppCard {
                    SectionTitle(text = "词根词缀", accent = LocalAppColors.current.warning)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = w.roots,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (w.mnemonic.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                AppCard {
                    SectionTitle(text = "联想记忆", accent = LocalAppColors.current.warning)
                    Spacer(Modifier.height(10.dp))
                    MnemonicBlock(w)
                }
            }

            if (w.similarList.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                AppCard {
                    SectionTitle(text = "形近词辨析", accent = LocalAppColors.current.danger)
                    Spacer(Modifier.height(10.dp))
                    SimilarBlock(w)
                }
            }

            Spacer(Modifier.height(14.dp))
            AppCard(color = MaterialTheme.colorScheme.surfaceVariant, padding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeakButton(text = w.word, onSpeak = speak, size = 40.dp, filled = false)
                    Spacer(Modifier.width(10.dp))
                    SpeakButton(text = w.sentence.ifBlank { w.word }, onSpeak = speak, size = 40.dp, filled = false)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "朗读单词 / 例句",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
