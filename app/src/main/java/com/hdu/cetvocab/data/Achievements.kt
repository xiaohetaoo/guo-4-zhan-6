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

    val ALL: List<AchievementDef> = listOf(
        AchievementDef(FIRST_STEP, "启程", "学习第一个单词", "seed", 1),
        AchievementDef(LEARN_50, "初露锋芒", "累计学习 50 个单词", "sprout", 1),
        AchievementDef(LEARN_200, "渐入佳境", "累计学习 200 个单词", "tree", 2),
        AchievementDef(LEARN_500, "词海行者", "累计学习 500 个单词", "mountain", 3),
        AchievementDef(STREAK_3, "小有坚持", "连续打卡 3 天", "fire", 1),
        AchievementDef(STREAK_7, "一周不断", "连续打卡 7 天", "fire", 2),
        AchievementDef(STREAK_10, "已连续打卡 10 天", "连续打卡 10 天", "fire", 3),
        AchievementDef(STREAK_30, "月度铁人", "连续打卡 30 天", "fire", 3),
        AchievementDef(QUIZ_FIRST, "初次闯关", "完成第一次单词闯关", "flag", 1),
        AchievementDef(QUIZ_PERFECT, "满分通关", "某次闯关全部答对", "medal", 2),
        AchievementDef(QUIZ_COMBO_10, "连击达人", "单次闯关连对 10 题", "bolt", 3),
        AchievementDef(QUIZ_SCORE_300, "高分猎手", "单次闯关得分达到 300", "crown", 3),
        AchievementDef(MASTER_50, "稳扎稳打", "掌握 50 个单词", "shield", 2),
        AchievementDef(MASTER_200, "词汇奠基", "掌握 200 个单词", "shield", 3),
        AchievementDef(ACCURACY_90, "精准记忆", "累计正确率达到 90%", "target", 2),
        AchievementDef(FAV_20, "生词收藏家", "收藏 20 个生词", "star", 1),
        AchievementDef(GOAL_DONE, "今日达标", "完成一次每日学习目标", "check", 1),
        AchievementDef(NIGHT_OWL, "夜猫子", "在 22:00 之后学习", "moon", 1),
        AchievementDef(EARLY_BIRD, "早起鸟", "在 7:00 之前学习", "sun", 1)
    )

    fun def(code: String): AchievementDef? = ALL.firstOrNull { it.code == code }
}
