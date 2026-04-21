package com.quranapp.android.db.relations

data class SearchResultVerseRow(
    val surahNo: Int,
    val ayahNo: Int,
)

data class SearchResultSearchRow(
    val slug: String,
    val surahNo: Int,
    val ayahNo: Int,
    val text: String,
)