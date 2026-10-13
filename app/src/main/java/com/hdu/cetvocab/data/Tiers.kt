package com.hdu.cetvocab.data

/**
 * 学习段位：按累计已学单词数划分。
 *
 * 阈值覆盖四级（4750 词）与六级（6248 词）两个阶段的常见学习进度：
 * 刚上手是青铜；学完大半本四级才够钻石；王者需要接近整本六级词表。
 */
enum class Tier(val label: String, val minWords: Int) {
    BRONZE("青铜", 0),
    SILVER("白银", 100),
    GOLD("黄金", 500),
    DIAMOND("钻石", 1500),
    KING("王者", 3000);

    /** 下一段位；已是最高段位时为 null */
    val next: Tier? get() = values().getOrNull(ordinal + 1)

    /** 距离下一段位还差多少词；已是最高段位时为 0 */
    fun wordsToNext(learnedWords: Int): Int =
        next?.let { (it.minWords - learnedWords).coerceAtLeast(0) } ?: 0

    /**
     * 在「当前段位 → 下一段位」区间内的进度比例（0..1）。
     *
     * 用于段位卡片底部那条进度条。已是最高段位时返回 1f 让条填满。
     * 青铜是最低段、没有上一段，进度从 0 词起算；其它段位的起点是上一段的 minWords。
     */
    fun progressToNext(learnedWords: Int): Float {
        val nextTier = next ?: return 1f
        val prevMin = values().getOrNull(ordinal - 1)?.minWords ?: 0
        val span = (nextTier.minWords - prevMin).coerceAtLeast(1)
        return ((learnedWords - prevMin).toFloat() / span).coerceIn(0f, 1f)
    }

    companion object {
        /**
         * 按已学词数取段位。
         *
         * 用 `lastOrNull` 而不是 `last`：`last { }` 在没有任何元素满足条件时会抛
         * `NoSuchElementException`。虽然入参由词数统计得出、正常不会为负，
         * 但这里返回 0 而不是崩溃更稳妥。
         */
        fun of(learnedWords: Int): Tier =
            values().lastOrNull { learnedWords >= it.minWords } ?: BRONZE
    }
}
