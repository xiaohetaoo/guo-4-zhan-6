package com.hdu.cetvocab.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * 内置只读词库。
 * 词库以 SQLite 文件形式随安装包分发，首次启动时从 assets 复制到私有目录，
 * 之后所有查询都在本地完成，全程不需要网络。
 */
class WordDb(private val context: Context) {

    @Volatile
    private var db: SQLiteDatabase? = null

    private fun open(): SQLiteDatabase {
        db?.let { if (it.isOpen) return it }
        val out = File(context.filesDir, DB_NAME)
        val prefs = context.getSharedPreferences("vocab_meta", Context.MODE_PRIVATE)
        val installed = prefs.getInt("words_db_version", 0)
        if (!out.exists() || installed != DB_VERSION) {
            context.assets.open(DB_NAME).use { input ->
                out.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
            }
            prefs.edit().putInt("words_db_version", DB_VERSION).apply()
        }
        val opened = SQLiteDatabase.openDatabase(
            out.path, null, SQLiteDatabase.OPEN_READONLY
        )
        db = opened
        return opened
    }

    private fun queryWords(selection: String, args: Array<String>?, orderBy: String?): List<Word> {
        val list = ArrayList<Word>()
        open().rawQuery(
            "SELECT id,word,phonetic,translation,trans_detail,definition,pos,stage,unit,rank," +
                "sentence,sentence_cn,roots,similar,mnemonic FROM words " +
                "WHERE $selection " + (orderBy?.let { "ORDER BY $it" } ?: ""),
            args
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Word(
                        id = c.getInt(0),
                        word = c.getString(1) ?: "",
                        phonetic = c.getString(2) ?: "",
                        translation = c.getString(3) ?: "",
                        transDetail = c.getString(4) ?: "",
                        definition = c.getString(5) ?: "",
                        pos = c.getString(6) ?: "",
                        stage = c.getInt(7),
                        unit = c.getInt(8),
                        rank = c.getInt(9),
                        sentence = c.getString(10) ?: "",
                        sentenceCn = c.getString(11) ?: "",
                        roots = c.getString(12) ?: "",
                        similar = c.getString(13) ?: "",
                        mnemonic = c.getString(14) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun wordsOfUnit(stage: Int, unit: Int): List<Word> =
        queryWords("stage=? AND unit=?", arrayOf(stage.toString(), unit.toString()), "\"rank\"")

    fun wordsOfStage(stage: Int): List<Word> =
        queryWords("stage=?", arrayOf(stage.toString()), "\"rank\"")

    /** 按首字母取词（字母不分大小写），用于「按首字母闯关」 */
    fun wordsByInitial(stage: Int, letter: Char): List<Word> =
        queryWords(
            "stage=? AND UPPER(substr(word,1,1))=?",
            arrayOf(stage.toString(), letter.uppercaseChar().toString()),
            "\"rank\""
        )

    fun wordById(id: Int): Word? =
        queryWords("id=?", arrayOf(id.toString()), null).firstOrNull()

    fun wordsByIds(ids: List<Int>): List<Word> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        return queryWords(
            "id IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray(),
            null
        )
    }

    fun countOfStage(stage: Int): Int =
        open().rawQuery("SELECT COUNT(*) FROM words WHERE stage=?", arrayOf(stage.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /**
     * 某一级别的全部词 id。
     * 用于按「级别」统计学习进度——学习记录在 user_data.db、词表在 words.db，
     * 是两个独立的 SQLite 库没法 JOIN，只能先取出本级 id 再与学习记录求交集。
     */
    fun idsOfStage(stage: Int): List<Int> =
        open().rawQuery("SELECT id FROM words WHERE stage=?", arrayOf(stage.toString())).use { c ->
            val ids = ArrayList<Int>(c.count)
            while (c.moveToNext()) ids.add(c.getInt(0))
            ids
        }

    fun unitCount(stage: Int): Int =
        open().rawQuery(
            "SELECT COUNT(DISTINCT unit) FROM words WHERE stage=?", arrayOf(stage.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun learnedCountInUnit(stage: Int, unit: Int): Int =
        open().rawQuery(
            "SELECT COUNT(*) FROM words WHERE stage=? AND unit=?", arrayOf(stage.toString(), unit.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun search(keyword: String, limit: Int = 60): List<Word> {
        val k = "%" + keyword.trim() + "%"
        return queryWords("word LIKE ? OR translation LIKE ?", arrayOf(k, k), "word LIMIT $limit")
    }

    /** 随机取词，用于生成干扰项 */
    fun randomWords(stage: Int, limit: Int): List<Word> =
        queryWords("stage=?", arrayOf(stage.toString()), "RANDOM() LIMIT $limit")

    companion object {
        const val DB_NAME = "words.db"
        const val DB_VERSION = 4
        const val STAGE_CET4 = 1
        const val STAGE_CET6 = 2

        fun stageLabel(stage: Int) = if (stage == STAGE_CET6) "六级" else "四级"
    }
}
