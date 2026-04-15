package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = SurahSearchAliasEntity::class)
@Entity(tableName = "surah_search_aliases_fts")
data class SurahAliasFtsEntity(
    @ColumnInfo(name = "alias")
    val alias: String,
    @ColumnInfo(name = "lang_code")
    val langCode: String,
    @ColumnInfo(name = "surah_no")
    val surahNo: Int
)