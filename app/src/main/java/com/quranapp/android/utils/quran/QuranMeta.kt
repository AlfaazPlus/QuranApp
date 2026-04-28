package com.quranapp.android.utils.quran

object QuranMeta {
    val chapterRange get() = 1..114
    val juzRange get() = 1..30
    val hizbRange get() = 1..60

    fun isChapterValid(chapterNo: Int?) = chapterNo in chapterRange
    fun isJuzValid(juzNo: Int?) = juzNo in juzRange
    fun isHizbValid(hizbNo: Int?) = hizbNo in hizbRange
    
    fun getAyahId(chapterNo: Int, verseNo: Int): Int {
        return chapterNo * 1000 + verseNo
    }

    fun getVerseNoFromAyahId(ayahId: Int): Pair<Int, Int> {
        val chapterNo = ayahId / 1000
        val ayahNo = ayahId % 1000

        return Pair(chapterNo, ayahNo)
    }
}