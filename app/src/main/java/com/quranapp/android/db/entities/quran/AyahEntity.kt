package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["surah_no"],
            childColumns = ["surah_no"],
        )
    ],
    indices = [
        Index(value = ["surah_no"], name = "idx_ayahs_surah"),
        Index(value = ["surah_no", "ayah_no"], unique = true, name = "idx_ayahs_surah_ayah"),
        Index(value = ["juz_no"], name = "idx_ayahs_juz"),
        Index(value = ["hizb_no"], name = "idx_ayahs_hizb"),
        Index(value = ["rub_no"], name = "idx_ayahs_rub"),
        Index(
            value = ["juz_no", "hizb_no", "rub_no", "manzil_no", "ruku_no", "surah_no", "ayah_no"],
            name = "idx_ayahs_juz_hizb_rub"
        )
    ]
)
data class AyahEntity(
    /**
     * ayahId = surahNo * 1000 + ayahNo
     */
    @PrimaryKey
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "surah_no")
    val surahNo: Int,

    @ColumnInfo(name = "ayah_no")
    val ayahNo: Int,

    @ColumnInfo(name = "juz_no")
    val juzNo: Int,

    @ColumnInfo(name = "hizb_no")
    val hizbNo: Int,

    @ColumnInfo(name = "rub_no")
    val rubNo: Int,

    @ColumnInfo(name = "manzil_no")
    val manzilNo: Int,

    @ColumnInfo(name = "ruku_no")
    val rukuNo: Int,

    @ColumnInfo(name = "sajdah_type", defaultValue = "0")
    val sajdahType: Int?
)