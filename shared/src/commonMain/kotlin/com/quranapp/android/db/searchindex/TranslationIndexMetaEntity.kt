package com.quranapp.android.db.searchindex

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "translation_index_meta")
data class TranslationIndexMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,
)
