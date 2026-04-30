package com.quranapp.android.db.searchindex

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(
    contentEntity = TranslationSearchContentEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"]
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
