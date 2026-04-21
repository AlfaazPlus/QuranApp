package com.quranapp.android.db.searchindex

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_index_meta")
data class TranslationIndexMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,
)
