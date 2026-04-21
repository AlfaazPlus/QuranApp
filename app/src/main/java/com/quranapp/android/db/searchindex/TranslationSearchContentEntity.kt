package com.quranapp.android.db.searchindex

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
