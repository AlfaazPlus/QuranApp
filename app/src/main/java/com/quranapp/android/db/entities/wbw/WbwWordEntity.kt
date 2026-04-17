package com.quranapp.android.db.entities.wbw

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "wbw_words",
    primaryKeys = ["wbw_id", "ayah_id", "word_index"],
    indices = [
        Index(value = ["ayah_id", "wbw_id"], name = "idx_wbw_words_ayah_wbw")
    ]
)
data class WbwWordEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "word_index")
    val wordIndex: Int,
    @ColumnInfo(name = "wbw_id")
    val wbwId: String,
    @ColumnInfo(name = "translation")
    val translation: String?,
    @ColumnInfo(name = "transliteration")
    val transliteration: String?,
)
