package com.hdu.cetvocab.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 触摸热区：把一个小控件的可点范围补足到至少 [min] 见方，而**不改变它自己的视觉尺寸**。
 *
 * 应用里有一批 30~38dp 的图标芯片（返回、关闭、收藏、单元胶囊、清除、朗读），
 * Material 指南的底线是 48dp、Google 无障碍检查会逐条报出来。
 * 直接把芯片放大到 48dp 是最省事的写法，但它会牵动每一行的节奏：
 * 顶栏变高、卡片变厚、一行里的文字与图标不再按原比例对齐，
 * 等于为了通过一次检查把已定稿的版式全部推翻重排。
 *
 * 这里把「画多大」和「点多大」拆开：外层是一个居中的透明 Box，
 * 只负责接收点击与朗读语义；内层照常绘制原有的圆形/圆角芯片。
 * 于是手指的命中区变成 [min]，屏幕上看到的仍然是那颗小芯片。
 *
 * 代价要说清楚：外层比内层宽出来的部分**会占布局**，
 * 所以套上它的那一行必须把相邻的内边距按同样的量收回去
 * （左右各 (min - 原尺寸)/2），否则整页会跟着长高、图标会往右挪。
 * 各页的对应调整都写在调用点注释里，改的时候两处要一起改。
 *
 * [align] 决定芯片贴在热区的哪一边。贴着行首的按钮（返回、关闭）要用
 * `CenterStart`、贴着行尾的要用 `CenterEnd`：默认居中会让芯片整体往内挪
 * (min - 原尺寸)/2 个 dp，原本对齐在 16dp 边距上的图标就错位了；
 * 贴边放置则让多出来的命中区只朝行内空隙那一侧长，图标位置一个像素都不变。
 *
 * [interaction] 由调用方传入并与内层的按压动画共用同一个实例：
 * 点击发生在外层，若内层用的是另一个 InteractionSource，
 * 按下时芯片就不会缩放，反馈会整个丢掉。
 */
@Composable
fun HitArea(
    onClick: () -> Unit,
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
    min: Dp = 48.dp,
    align: Alignment = Alignment.Center,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = min, minHeight = min)
            .clip(RoundedCornerShape(min / 3f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            // 自绘图标没有内置语义，朗读文本统一由热区这一层给出，
            // 内层不再重复声明，避免 TalkBack 念两遍
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        contentAlignment = align
    ) { content() }
}
