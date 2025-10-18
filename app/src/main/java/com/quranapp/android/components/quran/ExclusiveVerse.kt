package com.quranapp.android.components.quran

data class ExclusiveVerse(
    val id: Int,
    var title: String,
    var description: String? = null,
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
