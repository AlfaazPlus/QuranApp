package com.quranapp.android.utils.quran

object QuranMeta {
    val chapterRange get() = 1..114
    val juzRange get() = 1..30
    val hizbRange get() = 1..60

    fun isChapterValid(chapterNo: Int?) = chapterNo in chapterRange
    fun isJuzValid(juzNo: Int?) = juzNo in juzRange
    fun isHizbValid(hizbNo: Int?) = hizbNo in hizbRange
}