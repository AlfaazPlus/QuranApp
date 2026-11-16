package com.quranapp.android.components.quran

data class ExclusiveVerse(
    val id: Int,
    var title: String,
    var description: String? = null,
    var versesRaw: Set<String>,
    /**
     * (chapterNo, fromVerse, toVerse)
     */
    var verses: Set<Triple<Int, Int, Int>>,
    var chapters: Set<Int>,
    /**
     * To display in recycler view
     */
    var inChapters: String,
)
