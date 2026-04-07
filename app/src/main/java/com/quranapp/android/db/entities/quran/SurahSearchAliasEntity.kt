package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "surah_search_aliases",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["surah_no"],
            childColumns = ["surah_no"]
        )
    ],
    indices = [Index(value = ["surah_no", "lang_code", "alias"], unique = true)]
)
data class SurahSearchAliasEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "alias")
    val alias: String,

    @ColumnInfo(name = "lang_code")
    val langCode: String,

    @ColumnInfo(name = "surah_no")
    val surahNo: Int
)