package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.ui.theme.springPress

/** 二级页面统一顶栏（左返回 + 标题 + 右侧自定义） */
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    trailing: @Composable (() -> Unit)? = null
) {
    val backLabel = stringResource(R.string.action_back)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 左右仍是 16；上下从 12 收到 7：返回键的热区从 38 撑到 48（K13 无障碍项），
            // 多出的 10dp 由这里等量吸收，整条顶栏的高度（7+48+7 = 12+38+12 = 62）一点没变。
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 38dp 的返回芯片视觉上保持原样，只把命中区补到 48dp（见 HitArea 的说明）。
        // 与芯片共用同一个 InteractionSource，按压缩放动画才不会丢。
        // align=CenterStart：多出的 10dp 全部朝行内那一侧长，芯片左缘仍停在 16dp 边距上。
        HitArea(
            onClick = onBack,
            interaction = interaction,
            min = 48.dp,
            align = Alignment.CenterStart,
            contentDescription = backLabel
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .springPress(interaction, pressedScale = 0.88f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                IconBack(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        // 原 12dp：热区比芯片宽的 10dp 已经吃掉了一部分间距，
        // 补回来正好让标题停在原来的 x 上（16 + 48 + 2 == 16 + 38 + 12）
        Spacer(Modifier.width(2.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}
