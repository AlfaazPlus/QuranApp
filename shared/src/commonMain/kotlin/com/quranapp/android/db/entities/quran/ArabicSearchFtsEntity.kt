package com.quranapp.android.db.entities.quran

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions

@Fts5(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    notIndexed = ["ayah_id"]
)
@Entity(tableName = "arabic_search")
data class ArabicSearchFtsEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "text")
    val text: String
)