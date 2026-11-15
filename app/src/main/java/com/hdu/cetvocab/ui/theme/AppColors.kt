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
    /**
     * 「成功 / 答对」的**文字专用色**（见 [warningText] 的说明）。
     *
     * [success] 同时承担两种角色：装饰（进度条、圆点、色条、徽章底）与文字
     * （标签文字、数值、选项文字）。装饰只需与底色可辨（3:1），
     * 文字要按正文标准 4.5:1 —— 同一个色值不可能同时满足两边的观感要求。
     *
     * 浅色原值 #5B9A54 当文字用时实测：
     *   cardMint 3.00、surface 3.39、绿16%混底 2.87 —— 全部不达标。
     * 压暗到 #416D3C 后：5.35 / 6.04 / 5.12，全部过线。
     */
    val successText: Color,
    /**
     * 「提醒 / 待加强」的**文字专用色**。
     *
     * 与 [successText] 同一套设计。浅色原值 #E9A03B 当文字用时实测：
     *   cardPeach 1.97、cardAmber 2.05 —— 连大字号的 3:1 都不到，
     * 是全站对比度最差的一处（「待加强」标签、首页「连续打卡」数值、
     * 排行榜「历史最高分」数值都用它）。
     * 压暗到 #936525 后：4.57 / 4.74，过线。
     *
     * 深色那套语义色本身在深底上已经达标（6.5–7.8），
     * 因此深色的 text 槽直接复用原值，不做额外压暗。
     */
    val warningText: Color,
    /**
     * 「认识」按钮的**底色**。
     *
     * 按钮上是白字，白压 #5B9A54 只有 3.39:1、压深色 #7FBF78 只有 2.18:1。
     * 浅色把底压暗到 #4D8347（白字 4.51:1）；
     * 深色保持亮绿底、改用深色字（见 [onSuccessButton]）。
     */
    val successButton: Color,
    /** 「认识」按钮上的文字色，与 [successButton] 配对 */
    val onSuccessButton: Color,
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
    // 文字专用色：见字段注释里的实测对比度
    successText = Color(0xFF416D3C),
    warningText = Color(0xFF936525),
    successButton = Color(0xFF4D8347),
    onSuccessButton = Color(0xFFFFFFFF),
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
    // 深色语义色在深底上本就达标（success 6.52、warning 7.81），文字直接复用
    successText = DarkSuccess,
    warningText = DarkWarning,
    // 深色底是亮绿，白字只有 2.18:1 —— 保持亮绿底、改用深色字（8.08:1）
    successButton = DarkSuccess,
    onSuccessButton = Color(0xFF14210F),
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
