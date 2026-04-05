package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

enum class NavigationType {
    juz,
    hizb,
    rub
}

@Entity(
    tableName = "navigation_ranges",
    primaryKeys = ["type", "unit_no", "surah_no"],
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["surah_no"],
            childColumns = ["surah_no"],
        )
    ],
)
data class NavigationRangeEntity(
    @ColumnInfo(name = "type")
    val type: NavigationType,

    @ColumnInfo(name = "unit_no")
    val unitNo: Int,

    @ColumnInfo(name = "surah_no")
    val surahNo: Int,

    @ColumnInfo(name = "start_ayah")
    val startAyah: Int,

    @ColumnInfo(name = "end_ayah")
    val endAyah: Int
)