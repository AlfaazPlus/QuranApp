package com.quranapp.android.db.entities.translation

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "translation_books")
data class TranslationBookEntity(
    @PrimaryKey
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "lang_code")
    val langCode: String,

    @ColumnInfo(name = "lang_name")
    val langName: String,

    @ColumnInfo(name = "book_name", defaultValue = "''")
    val bookName: String = "",

    @ColumnInfo(name = "author_name", defaultValue = "''")
    val authorName: String = "",

    @ColumnInfo(name = "display_name", defaultValue = "''")
    val displayName: String = "",

    @ColumnInfo(name = "version")
    val version: Long,
)
