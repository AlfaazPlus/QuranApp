package com.quranapp.android.db.entities.quran

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5

@Fts5(
    contentEntity = SurahSearchAliasEntity::class,
    contentRowId = "id",
    notIndexed = ["lang_code", "surah_no"],
)
@Entity(tableName = "surah_search_aliases_fts")
data class SurahAliasFtsEntity(
    @ColumnInfo(name = "alias")
    val alias: String,
    @ColumnInfo(name = "lang_code")
    val langCode: String,
    @ColumnInfo(name = "surah_no")
    val surahNo: Int
)