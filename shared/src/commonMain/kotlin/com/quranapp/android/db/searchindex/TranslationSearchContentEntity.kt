package com.quranapp.android.db.searchindex

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "translation_search_content",
    indices = [
        Index(value = ["slug", "surahNo", "ayahNo"], unique = true),
    ],
)
data class TranslationSearchContentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "surahNo")
    val surahNo: Int,

    @ColumnInfo(name = "ayahNo")
    val ayahNo: Int,

    @ColumnInfo(name = "text")
    val text: String,
)
