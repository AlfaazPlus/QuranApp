package com.quranapp.android.db.searchindex

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions

@Fts5(
    contentEntity = TranslationSearchContentEntity::class,
    contentRowId = "id",
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics", "2"],
    notIndexed = ["slug", "surahNo", "ayahNo"],
)
@Entity(tableName = "translation_search_fts")
data class TranslationSearchFtsEntity(
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "surahNo")
    val surahNo: Int,

    @ColumnInfo(name = "ayahNo")
    val ayahNo: Int,

    @ColumnInfo(name = "text")
    val text: String,
)
