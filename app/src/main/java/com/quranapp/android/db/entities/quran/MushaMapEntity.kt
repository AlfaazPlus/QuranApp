package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class MushafLineType {
    surah_name,
    ayah,
    basmallah
}

@Entity(
    tableName = "mushaf_map",
    primaryKeys = ["mushaf_id", "page_number", "line_number"],
    foreignKeys = [
        ForeignKey(
            entity = MushafEntity::class,
            parentColumns = ["mushaf_id"],
            childColumns = ["mushaf_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["start_ayah_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["end_ayah_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mushaf_id", "page_number"], name = "idx_pages_lookup"),
        Index(value = ["start_ayah_id"]),
        Index(value = ["end_ayah_id"]),
        Index(value = ["surah_no"])
    ]
)
data class MushafMapEntity(
    @ColumnInfo(name = "mushaf_id")
    val mushafId: Int,

    @ColumnInfo(name = "page_number")
    val pageNumber: Int,

    @ColumnInfo(name = "line_number")
    val lineNumber: Int,

    @ColumnInfo(name = "line_type")
    val lineType: MushafLineType,

    @ColumnInfo(name = "is_centered")
    val isCentered: Boolean,

    @ColumnInfo(name = "start_ayah_id")
    val startAyahId: Int? = null,

    @ColumnInfo(name = "start_word_index")
    val startWordIndex: Int? = null,

    @ColumnInfo(name = "end_ayah_id")
    val endAyahId: Int? = null,

    @ColumnInfo(name = "end_word_index")
    val endWordIndex: Int? = null,

    @ColumnInfo(name = "surah_no")
    val surahNo: Int? = null
)