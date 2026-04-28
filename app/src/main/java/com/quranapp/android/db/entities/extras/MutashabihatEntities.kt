package com.quranapp.android.db.entities.extras

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranapp.android.db.entities.quran.AyahEntity

@Entity(
    tableName = "mutashabihat_phrases",
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["source_ayah_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
data class MutashabihatPhraseEntity(
    @PrimaryKey
    @ColumnInfo(name = "phrase_id")
    val phraseId: Int,
    @ColumnInfo(name = "surahs_count")
    val surahsCount: Int,
    @ColumnInfo(name = "ayahs_count")
    val ayahsCount: Int,
    @ColumnInfo(name = "occurrence_count")
    val occurrenceCount: Int,
    @ColumnInfo(name = "source_ayah_id")
    val sourceAyahId: Int,
    @ColumnInfo(name = "source_word_from")
    val sourceWordFrom: Int,
    @ColumnInfo(name = "source_word_to")
    val sourceWordTo: Int,
)

@Entity(
    tableName = "mutashabihat_phrase_ayah",
    primaryKeys = ["phrase_id", "ayah_id"],
    foreignKeys = [
        ForeignKey(
            entity = MutashabihatPhraseEntity::class,
            parentColumns = ["phrase_id"],
            childColumns = ["phrase_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["ayah_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(name = "idx_mutashabihat_phrase_ayah_ayah", value = ["ayah_id"]),
        Index(
            name = "idx_mutashabihat_phrase_ayah_ayah_list_order",
            value = ["ayah_id", "in_ayah_order"],
        ),
    ],
)
data class MutashabihatPhraseAyahEntity(
    @ColumnInfo(name = "phrase_id")
    val phraseId: Int,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    /** JSON: `[[from,to], ...]` 0-based inclusive. */
    @ColumnInfo(name = "word_ranges")
    val wordRanges: String,
    @ColumnInfo(name = "in_ayah_order")
    val inAyahOrder: Int?,
)
