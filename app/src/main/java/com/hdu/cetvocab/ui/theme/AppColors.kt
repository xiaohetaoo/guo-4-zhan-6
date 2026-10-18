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
     * 熟练度圆点里**未点亮**的那几格的颜色（见 `WordSections.LevelDots`）。
     *
     * 单独开一个色槽，而不是复用 [androidx.compose.material3.ColorScheme.outlineVariant]：
     * 后者是「描边/分隔线」的色槽，取值极淡（浅色 #F2E2D7、深色 #3A322B），
     * 放在卡片底上只有约 1.1–1.3:1 —— 而 LevelDots 的全部信息量就在
     * 「点亮的格子 vs 未点亮的格子」这个对比上，空格看不清等于组件失效。
     * 非文本图形要求 3:1，这两个值实测在 surface 与 surfaceVariant 两种底上都过线。
     */
    val levelDotOff: Color,
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
    /*
     * 浅色主题的成就金属色**同时用作文字色**（成就详情弹层的「金色成就」标签、
     * 徽章上的字形），所以必须按正文标准（4.5:1）取值，不能只求「好看的金银色」。
     *
     * 原先取的是 CB8B62 / B9A38C / D9A441 —— 用作底色时好看，但作文字色时
     * 在弹层底（#FFFFFF）上只有 2.83 / 2.42 / 2.25:1，在徽章底（tint 22% 混合）
     * 上更是 2.08 / 1.84 / 1.72:1，远低于 4.5:1。
     * 而深色那一套（D89B72 / C3B0A0 / E0B45A）在深底上是 6.79 / 7.70 / 8.32:1 全部达标 ——
     * 也就是说这是一处**只在浅色下成立**的可读性缺陷。
     *
     * 现在压暗到实测 ≥4.5:1（on #FFFFFF / #FFF0E6 两种底色都过线）：
     *   铜 #8A5A38 = 5.83 / 5.24   银 #6E5F50 = 6.15 / 5.52   金 #8A6410 = 5.37 / 4.82
     * 金属感由「低明度暖色」保留，观感仍是铜/银/金三档。
     */
    tierBronze = Color(0xFF8A5A38),
    tierSilver = Color(0xFF6E5F50),
    tierGold = Color(0xFF8A6410),
    badgeLockedTint = Color(0xFFCBBBB0),
    badgeLockedBg = Color(0xFFE9DED6),
    // 锁定徽章的字形也是文字：原先 #9C8B80 在 #E9DED6 上只有 2.47:1，
    // 22sp 粗体虽属「大字」档（3:1）也没过，压到 #6E5F50 得 4.65:1。
    badgeLockedFg = Color(0xFF6E5F50),
    // 未点亮圆点：见字段注释，两种底上都要 ≥3:1
    levelDotOff = Color(0xFF97867A),
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
    // 深色原值 #7C6F64 在 #2E2721 上是 3.02:1 —— 刚好卡在 3:1 线上、零余量。
    // 提到 #A89A8C 得 5.37:1，大字与正文都稳妥。
    badgeLockedFg = Color(0xFFA89A8C),
    // 未点亮圆点：见字段注释（深色 #847667 = 3.15 / 3.66:1）
    levelDotOff = Color(0xFF847667),
    isDark = true
)

/** 语义色 CompositionLocal，默认浅色，避免预览/测试时为空 */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }
