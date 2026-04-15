package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranapp.android.db.interfaces.SurahMethods

enum class RevelationType {
    meccan,
    medinan
}

@Entity(
    tableName = "surahs",
    indices = [
        Index(value = ["revelation_type"], name = "idx_surahs_revelation_type"),
        Index(value = ["revelation_order"], name = "idx_surahs_order")
    ]
)
data class SurahEntity(
    @PrimaryKey
    @ColumnInfo(name = "surah_no")
    val surahNo: Int,

    @ColumnInfo(name = "ayah_count")
    val ayahCount: Int,

    @ColumnInfo(name = "revelation_order")
    val revelationOrder: Int,

    @ColumnInfo(name = "rukus_count")
    val rukusCount: Int,

    @ColumnInfo(name = "revelation_type")
    val revelationType: RevelationType
) : SurahMethods {
    override fun isVerseValid(verseNo: Int): Boolean {
        return isVerseRangeValid(verseNo, verseNo)
    }

    override fun isVerseRangeValid(fromVerse: Int, toVerse: Int): Boolean {
        return fromVerse <= toVerse && fromVerse in 1..ayahCount && toVerse in 1..ayahCount
    }
}