package com.quranapp.android.db.entities.quran

import androidx.room.*

@Entity(
    tableName = "ayah_words",
    primaryKeys = ["ayah_id", "script_id", "word_index"],
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["ayah_id"],
        ),
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["script_id"],
            childColumns = ["script_id"],
        )
    ],
    indices = [
        Index(value = ["ayah_id", "script_id"], name = "idx_ayah_words_ayah_script")
    ]
)
data class AyahWordEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "script_id")
    val scriptId: Int,

    @ColumnInfo(name = "word_index")
    val wordIndex: Int,

    @ColumnInfo(name = "text")
    val text: String
) {
    @Ignore
    var isLastWordOfAyah: Boolean = false
}