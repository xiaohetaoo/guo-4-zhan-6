package com.hdu.cetvocab.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hdu.cetvocab.data.Achievements
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AchievementBadge
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.AppTopBar
import com.hdu.cetvocab.ui.components.IconTrophy
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.ui.theme.LocalAppColors

@Composable
fun AchievementsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.profile.collectAsState()
    val unlocked = state.unlocked
    val total = Achievements.ALL.size
    // 只统计「当前版本定义过」的成就码。库里的 code 可能来自手工编辑过的备份，
    // 直接拿 unlocked.size 与 total 比会出现「已解锁 100 / 81 个成就」，
    // 而下面进度条用的 fraction 一旦大于 1 就撞上 fillMaxWidth 的 require 断言。
    val unlockedCount = Achievements.ALL.count { unlocked.containsKey(it.code) }
    val ratio = if (total == 0) 0f
    else (unlockedCount.toFloat() / total).coerceIn(0f, 1f)
    val goldenCount = Achievements.ALL.count { it.tier == 3 && unlocked.containsKey(it.code) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "成就墙",
            subtitle = "已解锁 $unlockedCount / $total 个成就",
            onBack = onBack
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column {
                    AppCard(color = LocalAppColors.current.cardMint, padding = 18.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "成就进度",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "$unlockedCount / $total",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAppColors.current.success
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LocalAppColors.current.success)
                                    )
                                }
                            }
                            Spacer(Modifier.padding(6.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconTrophy(
                                    modifier = Modifier.size(32.dp),
                                    color = LocalAppColors.current.gold
                                )
                                Text(
                                    text = "金色 $goldenCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalAppColors.current.gold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionTitle(text = "全部成就", accent = LocalAppColors.current.gold)
                    Spacer(Modifier.height(10.dp))
                }
            }

            items(Achievements.ALL, key = { it.code }) { def ->
                Column {
                    AchievementBadge(
                        def = def,
                        unlocked = unlocked.containsKey(def.code),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = def.desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
