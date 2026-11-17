package com.quranapp.android.db.entities.translation

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = "translation_footnotes",
    primaryKeys = ["slug", "ayah_id", "number"],
)
data class TranslationFootnoteEntity(
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "number")
    val number: Int,

    @ColumnInfo(name = "text")
    val text: String,
)
