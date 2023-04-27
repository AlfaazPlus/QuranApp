package com.quranapp.android.components.quran.dua

data class Dua(
    val id: Int,
    var name: String,
    var versesRaw: List<String>,
    /**
     * (chapterNo, fromVerse, toVerse)
     */
    var verses: List<Triple<Int, Int, Int>>,
    var chapters: List<Int>,
    /**
     * To display in recycler view
     */
    var inChapters: String,
)
