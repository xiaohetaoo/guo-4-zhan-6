package com.hdu.cetvocab.data

/** 成就定义表 */
object Achievements {

    const val FIRST_STEP = "first_step"
    const val LEARN_50 = "learn_50"
    const val LEARN_200 = "learn_200"
    const val LEARN_500 = "learn_500"
    const val STREAK_3 = "streak_3"
    const val STREAK_7 = "streak_7"
    const val STREAK_10 = "streak_10"
    const val STREAK_30 = "streak_30"
    const val QUIZ_FIRST = "quiz_first"
    const val QUIZ_PERFECT = "quiz_perfect"
    const val QUIZ_COMBO_10 = "quiz_combo_10"
    const val QUIZ_SCORE_300 = "quiz_score_300"
    const val MASTER_50 = "master_50"
    const val MASTER_200 = "master_200"
    const val ACCURACY_90 = "accuracy_90"
    const val FAV_20 = "fav_20"
    const val GOAL_DONE = "goal_done"
    const val NIGHT_OWL = "night_owl"
    const val EARLY_BIRD = "early_bird"
    // ---- 专题关卡成就（易混词辨析 / 真题句子翻译）----
    const val CONFUSABLE_FIRST = "confusable_first"
    const val TRANSLATION_FIRST = "translation_first"
    const val CONFUSABLE_PERFECT_5 = "confusable_perfect_5"
    const val TRANSLATION_PERFECT_5 = "translation_perfect_5"
    const val TOPIC_SWEEP = "topic_sweep"

    // ---- 累计学习词数阶梯 ----
    const val LEARN_20 = "learn_20"
    const val LEARN_100 = "learn_100"
    const val LEARN_300 = "learn_300"
    const val LEARN_800 = "learn_800"
    const val LEARN_1000 = "learn_1000"
    const val LEARN_1500 = "learn_1500"
    const val LEARN_2000 = "learn_2000"
    const val LEARN_3000 = "learn_3000"
    const val LEARN_5000 = "learn_5000"

    // ---- 熟悉以上词数阶梯（level >= 3，见 VocabRepository.MASTER_LEVEL）----
    const val MASTER_100 = "master_100"
    const val MASTER_300 = "master_300"
    const val MASTER_500 = "master_500"
    const val MASTER_800 = "master_800"
    const val MASTER_1000 = "master_1000"
    const val MASTER_2000 = "master_2000"

    // ---- 连续打卡阶梯 ----
    const val STREAK_15 = "streak_15"
    const val STREAK_21 = "streak_21"
    const val STREAK_60 = "streak_60"
    const val STREAK_100 = "streak_100"
    const val STREAK_180 = "streak_180"
    const val STREAK_365 = "streak_365"

    // ---- 历史最长连续 ----
    const val BEST_STREAK_7 = "best_streak_7"
    const val BEST_STREAK_30 = "best_streak_30"
    const val BEST_STREAK_100 = "best_streak_100"
    const val BEST_STREAK_365 = "best_streak_365"

    // ---- 闯关次数 ----
    const val QUIZ_5 = "quiz_5"
    const val QUIZ_10 = "quiz_10"
    const val QUIZ_20 = "quiz_20"
    const val QUIZ_50 = "quiz_50"
    const val QUIZ_100 = "quiz_100"

    // ---- 单次闯关得分 ----
    const val QUIZ_SCORE_100 = "quiz_score_100"
    const val QUIZ_SCORE_200 = "quiz_score_200"
    const val QUIZ_SCORE_400 = "quiz_score_400"
    const val QUIZ_SCORE_500 = "quiz_score_500"

    // ---- 连击 ----
    const val QUIZ_COMBO_5 = "quiz_combo_5"
    const val QUIZ_COMBO_20 = "quiz_combo_20"
    const val QUIZ_COMBO_30 = "quiz_combo_30"
    const val QUIZ_COMBO_50 = "quiz_combo_50"

    // ---- 累计学习时长（分钟） ----
    const val MINUTES_30 = "minutes_30"
    const val MINUTES_60 = "minutes_60"
    const val MINUTES_120 = "minutes_120"
    const val MINUTES_300 = "minutes_300"
    const val MINUTES_600 = "minutes_600"
    const val MINUTES_1200 = "minutes_1200"

    // ---- 收藏 ----
    const val FAV_50 = "fav_50"
    const val FAV_100 = "fav_100"
    const val FAV_200 = "fav_200"

    // ---- 正确率 ----
    const val ACCURACY_95 = "accuracy_95"
    const val ACCURACY_98 = "accuracy_98"

    // ---- 单日学习量 ----
    const val DAILY_50 = "daily_50"
    const val DAILY_100 = "daily_100"

    // ---- 时段 ----
    const val MIDNIGHT = "midnight"
    const val NOON_STUDY = "noon_study"

    // ---- 专题关卡全对次数 ----
    const val CONFUSABLE_PERFECT_10 = "confusable_perfect_10"
    const val CONFUSABLE_PERFECT_20 = "confusable_perfect_20"
    const val TRANSLATION_PERFECT_10 = "translation_perfect_10"
    const val TRANSLATION_PERFECT_20 = "translation_perfect_20"

    val ALL: List<AchievementDef> = listOf(
        AchievementDef(FIRST_STEP, "启程", "学习第一个单词", "seed", 1),
        AchievementDef(LEARN_50, "初露锋芒", "累计学习 50 个单词", "sprout", 1),
        AchievementDef(LEARN_200, "渐入佳境", "累计学习 200 个单词", "tree", 2),
        AchievementDef(LEARN_500, "词海行者", "累计学习 500 个单词", "mountain", 3),
        AchievementDef(STREAK_3, "小有坚持", "连续打卡 3 天", "fire", 1),
        AchievementDef(STREAK_7, "一周不断", "连续打卡 7 天", "fire", 2),
        // 标题一律用短名：成就墙是 3 列网格、标题 maxLines=1，
        // 写成完整句子（原为「已连续打卡 10 天」）会被省略号截断。
        AchievementDef(STREAK_10, "十日不辍", "连续打卡 10 天", "fire", 3),
        AchievementDef(STREAK_30, "月度铁人", "连续打卡 30 天", "fire", 3),
        AchievementDef(QUIZ_FIRST, "初次闯关", "完成第一次单词闯关", "flag", 1),
        AchievementDef(QUIZ_PERFECT, "满分通关", "某次闯关全部答对", "medal", 2),
        AchievementDef(QUIZ_COMBO_10, "连击达人", "单次闯关连对 10 题", "bolt", 3),
        AchievementDef(QUIZ_SCORE_300, "高分猎手", "单次闯关得分达到 300", "crown", 3),
        // 「掌握」在 levelLabel 里是第 4 档，而这些成就按 `level >= 3`（熟悉）统计，
        // 所以成就文案一律写「熟悉以上」，和首页/「我的」页的统计位同一口径。
        AchievementDef(MASTER_50, "稳扎稳打", "熟悉以上 50 个单词", "shield", 2),
        AchievementDef(MASTER_200, "词汇奠基", "熟悉以上 200 个单词", "shield", 3),
        // 正确率成就必须写明作答数下限：题目太少时正确率没有统计意义，
        // 90% 档同样要求满 50 题，只有一档没写会让用户以为「答 5 题全对」就能解锁。
        AchievementDef(ACCURACY_90, "精准记忆", "作答 50 题以上且正确率达到 90%", "target", 2),
        AchievementDef(FAV_20, "生词收藏家", "收藏 20 个生词", "star", 1),
        AchievementDef(GOAL_DONE, "今日达标", "完成一次每日学习目标", "check", 1),
        AchievementDef(NIGHT_OWL, "夜猫子", "在 22:00 之后学习", "moon", 1),
        // 判定是 `hour in 1..6`（1:00–6:59），零点那一档属于「深夜苦读」，
        // 原先写「7:00 之前」把 0:00–0:59 也算进来了，文案比代码宽。
        AchievementDef(EARLY_BIRD, "早起鸟", "清晨 1:00 - 7:00 学习", "sun", 1),
        // 专题关卡：先给「第一次完成」的低门槛成就做引导，
        // 再用「累计 5 次全对」做长期目标，最后用一个跨专题的收尾成就串起来。
        AchievementDef(CONFUSABLE_FIRST, "明辨秋毫", "完成一次易混词辨析闯关", "eye", 1),
        AchievementDef(TRANSLATION_FIRST, "初译登场", "完成一次真题句子翻译闯关", "pen", 1),
        AchievementDef(CONFUSABLE_PERFECT_5, "火眼金睛", "易混词辨析累计 5 次全对", "eye", 2),
        AchievementDef(TRANSLATION_PERFECT_5, "译海拾贝", "真题句子翻译累计 5 次全对", "pen", 2),
        AchievementDef(TOPIC_SWEEP, "三关通吃", "全词挑战、辨析、翻译各完成一次", "gate", 3),

        // ---- 累计学习词数阶梯 ----
        // 低门槛（20 词）先给一个「启程后有回响」的即时反馈，后面按 100/300/800…
        // 拉开间距，避免前期连着跳成就、后期长时间断档。
        AchievementDef(LEARN_20, "初出茅庐", "累计学习 20 个单词", "seed", 1),
        AchievementDef(LEARN_100, "百词初成", "累计学习 100 个单词", "tree", 1),
        AchievementDef(LEARN_300, "三百之数", "累计学习 300 个单词", "tree", 2),
        AchievementDef(LEARN_800, "八百征程", "累计学习 800 个单词", "tree", 2),
        AchievementDef(LEARN_1000, "千词加冕", "累计学习 1000 个单词", "mountain", 3),
        AchievementDef(LEARN_1500, "披荆斩棘", "累计学习 1500 个单词", "mountain", 3),
        AchievementDef(LEARN_2000, "两千在握", "累计学习 2000 个单词", "mountain", 3),
        AchievementDef(LEARN_3000, "三千世界", "累计学习 3000 个单词", "mountain", 3),
        AchievementDef(LEARN_5000, "登峰造极", "累计学习 5000 个单词", "crown", 3),

        // ---- 熟悉以上词数阶梯 ----
        AchievementDef(MASTER_100, "百炼成钢", "熟悉以上 100 个单词", "shield", 2),
        AchievementDef(MASTER_300, "炉火纯青", "熟悉以上 300 个单词", "shield", 3),
        AchievementDef(MASTER_500, "根基深厚", "熟悉以上 500 个单词", "shield", 3),
        AchievementDef(MASTER_800, "运用自如", "熟悉以上 800 个单词", "shield", 3),
        AchievementDef(MASTER_1000, "千词在握", "熟悉以上 1000 个单词", "crown", 3),
        AchievementDef(MASTER_2000, "词汇大师", "熟悉以上 2000 个单词", "crown", 3),

        // ---- 连续打卡阶梯 ----
        AchievementDef(STREAK_15, "半月不断", "连续打卡 15 天", "fire", 2),
        AchievementDef(STREAK_21, "习惯养成", "连续打卡 21 天", "fire", 2),
        AchievementDef(STREAK_60, "两月长跑", "连续打卡 60 天", "fire", 3),
        AchievementDef(STREAK_100, "百日筑基", "连续打卡 100 天", "fire", 3),
        AchievementDef(STREAK_180, "半年不辍", "连续打卡 180 天", "fire", 3),
        AchievementDef(STREAK_365, "一年之约", "连续打卡 365 天", "crown", 3),

        // ---- 历史最长连续（断签之后仍可追溯的高光记录）----
        AchievementDef(BEST_STREAK_7, "七日之约", "历史最长连续 7 天", "fire", 1),
        AchievementDef(BEST_STREAK_30, "月度回顾", "历史最长连续 30 天", "fire", 2),
        AchievementDef(BEST_STREAK_100, "百日回首", "历史最长连续 100 天", "fire", 3),
        AchievementDef(BEST_STREAK_365, "一年之期", "历史最长连续 365 天", "crown", 3),

        // ---- 闯关次数 ----
        AchievementDef(QUIZ_5, "小试牛刀", "累计闯关 5 次", "flag", 1),
        AchievementDef(QUIZ_10, "身经十战", "累计闯关 10 次", "flag", 2),
        AchievementDef(QUIZ_20, "二十而立", "累计闯关 20 次", "flag", 2),
        AchievementDef(QUIZ_50, "闯关老手", "累计闯关 50 次", "medal", 3),
        AchievementDef(QUIZ_100, "身经百战", "累计闯关 100 次", "crown", 3),

        // ---- 单次闯关得分 ----
        AchievementDef(QUIZ_SCORE_100, "小有所成", "单次闯关得分达到 100", "medal", 1),
        AchievementDef(QUIZ_SCORE_200, "更上一层", "单次闯关得分达到 200", "medal", 2),
        AchievementDef(QUIZ_SCORE_400, "势如破竹", "单次闯关得分达到 400", "crown", 3),
        AchievementDef(QUIZ_SCORE_500, "一骑绝尘", "单次闯关得分达到 500", "crown", 3),

        // ---- 连击 ----
        AchievementDef(QUIZ_COMBO_5, "连击初成", "单次闯关连对 5 题", "bolt", 1),
        AchievementDef(QUIZ_COMBO_20, "势不可挡", "单次闯关连对 20 题", "bolt", 2),
        AchievementDef(QUIZ_COMBO_30, "所向披靡", "单次闯关连对 30 题", "bolt", 3),
        AchievementDef(QUIZ_COMBO_50, "无人能挡", "单次闯关连对 50 题", "crown", 3),

        // ---- 累计学习时长 ----
        AchievementDef(MINUTES_30, "小试长跑", "累计学习 30 分钟", "sun", 1),
        AchievementDef(MINUTES_60, "整点学习", "累计学习 60 分钟", "sun", 1),
        AchievementDef(MINUTES_120, "持之以恒", "累计学习 120 分钟", "sun", 2),
        AchievementDef(MINUTES_300, "厚积薄发", "累计学习 300 分钟", "mountain", 2),
        AchievementDef(MINUTES_600, "水滴石穿", "累计学习 600 分钟", "mountain", 3),
        AchievementDef(MINUTES_1200, "绳锯木断", "累计学习 1200 分钟", "crown", 3),

        // ---- 收藏 ----
        AchievementDef(FAV_50, "收藏达人", "收藏 50 个生词", "star", 2),
        AchievementDef(FAV_100, "百宝箱", "收藏 100 个生词", "star", 2),
        AchievementDef(FAV_200, "收藏大家", "收藏 200 个生词", "star", 3),

        // ---- 正确率（题目太少时正确率没有统计意义，因此设作答数下限）----
        AchievementDef(ACCURACY_95, "精准无误", "作答 100 题以上且正确率达到 95%", "target", 3),
        AchievementDef(ACCURACY_98, "百发百中", "作答 200 题以上且正确率达到 98%", "target", 3),

        // ---- 单日学习量 ----
        AchievementDef(DAILY_50, "日进五十", "单日学习 50 个单词", "check", 2),
        AchievementDef(DAILY_100, "日进百词", "单日学习 100 个单词", "check", 3),

        // ---- 时段 ----
        AchievementDef(MIDNIGHT, "深夜苦读", "在 0:00 - 2:00 之间学习", "moon", 2),
        AchievementDef(NOON_STUDY, "午间充电", "在 12:00 - 13:00 之间学习", "sun", 1),

        // ---- 专题关卡全对次数 ----
        AchievementDef(CONFUSABLE_PERFECT_10, "明察秋毫", "易混词辨析累计 10 次全对", "eye", 3),
        AchievementDef(CONFUSABLE_PERFECT_20, "一眼定真", "易混词辨析累计 20 次全对", "eye", 3),
        AchievementDef(TRANSLATION_PERFECT_10, "译林高手", "真题句子翻译累计 10 次全对", "pen", 3),
        AchievementDef(TRANSLATION_PERFECT_20, "译道大成", "真题句子翻译累计 20 次全对", "pen", 3)
    )

    fun def(code: String): AchievementDef? = ALL.firstOrNull { it.code == code }
}
