package com.hdu.cetvocab.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 主题相关的语义色与卡片底色。
 *
 * 这些颜色无法直接用 Material 的 [androidx.compose.material3.ColorScheme] 表达，
 * 因此单独抽成一套，随深浅色主题切换。浅色实例使用与原设计完全一致的色值，
 * 保证已有界面（浅色）视觉零变化。
 */
data class AppColors(
    // 语义色
    val success: Color,
    val warning: Color,
    val danger: Color,
    val star: Color,
    val fire: Color,
    val gold: Color,
    // 卡片柔和底色
    val cardPeach: Color,
    val cardAmber: Color,
    val cardRose: Color,
    val cardMint: Color,
    val cardSky: Color,
    // 成就徽章金属色 / 未解锁底色
    val tierBronze: Color,
    val tierSilver: Color,
    val tierGold: Color,
    val badgeLockedTint: Color,
    val badgeLockedBg: Color,
    val badgeLockedFg: Color,
    /**
     * 当前是不是深色主题。
     *
     * 需要它是因为「同一套语义色在深浅两套底上要求相反」：段位色在浅色卡片上
     * 必须压暗才够 4.5:1，在深色卡片上又必须提亮才够。只给一组色值，
     * 另一套主题下必然有一头不达标。
     */
    val isDark: Boolean
)

/** 浅色实例：色值与原 [Color.kt] 中的常量一一对应，数值不变 */
val LightAppColors = AppColors(
    success = ColorSuccess,
    warning = ColorWarning,
    danger = ColorDanger,
    star = ColorStar,
    fire = ColorFire,
    gold = ColorGold,
    cardPeach = CardPeach,
    cardAmber = CardAmber,
    cardRose = CardRose,
    cardMint = CardMint,
    cardSky = CardSky,
    tierBronze = Color(0xFFCB8B62),
    tierSilver = Color(0xFFB9A38C),
    tierGold = ColorGold,
    badgeLockedTint = Color(0xFFCBBBB0),
    badgeLockedBg = Color(0xFFE9DED6),
    badgeLockedFg = Color(0xFF9C8B80),
    isDark = false
)

/** 深色实例 */
val DarkAppColors = AppColors(
    success = DarkSuccess,
    warning = DarkWarning,
    danger = DarkDanger,
    star = DarkStar,
    fire = DarkFire,
    gold = DarkGold,
    cardPeach = DarkCardPeach,
    cardAmber = DarkCardAmber,
    cardRose = DarkCardRose,
    cardMint = DarkCardMint,
    cardSky = DarkCardSky,
    tierBronze = DarkTierBronze,
    tierSilver = DarkTierSilver,
    tierGold = DarkTierGold,
    badgeLockedTint = Color(0xFF5A4E44),
    badgeLockedBg = DarkBadgeLockedBg,
    badgeLockedFg = DarkBadgeLockedFg,
    isDark = true
)

/** 语义色 CompositionLocal，默认浅色，避免预览/测试时为空 */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }
