package com.quranapp.android.chapterInfo

import com.quranapp.android.db.entities.quran.RevelationType

data class ChapterInfoContentData(
    val chapterNo: Int,
    val chapterName: String,
    val language: String,
    val verseCount: Int,
    val rukuCount: Int,
    val revelationOrder: Int,
    val revelationType: RevelationType,
    val juzNos: List<Int>,
)
