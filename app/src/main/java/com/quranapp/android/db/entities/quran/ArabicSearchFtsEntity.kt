package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(
    tokenizer = "unicode61"
)
@Entity(tableName = "arabic_search")
data class ArabicSearchFtsEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "text")
    val text: String
)