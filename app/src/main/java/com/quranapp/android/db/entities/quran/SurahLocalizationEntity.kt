package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "surah_localizations",
    primaryKeys = ["surah_no", "lang_code"],
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["surah_no"],
            childColumns = ["surah_no"],
        )
    ],
    indices = [
        Index(value = ["lang_code"], name = "idx_surah_localizations_lang"),
        Index(value = ["name"], name = "idx_surah_localizations_name")
    ]
)
data class SurahLocalizationEntity(
    @ColumnInfo(name = "surah_no")
    val surahNo: Int,

    @ColumnInfo(name = "lang_code")
    val langCode: String,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "meaning")
    val meaning: String? = null
)