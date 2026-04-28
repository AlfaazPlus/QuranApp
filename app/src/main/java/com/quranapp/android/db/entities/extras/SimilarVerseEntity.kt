package com.quranapp.android.db.entities.extras

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.quranapp.android.db.entities.quran.AyahEntity


@Entity(
    tableName = "similar_verses",
    primaryKeys = ["source_ayah_id", "matched_ayah_id"],
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["source_ayah_id"],
        ),
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["ayah_id"],
            childColumns = ["matched_ayah_id"],
        ),
    ],
    indices = [
        Index(name = "idx_similar_verses_matched_ayah", value = ["matched_ayah_id"]),
        Index(
            name = "idx_similar_verses_source_score",
            value = ["source_ayah_id", "score"],
            orders = [Index.Order.ASC, Index.Order.DESC],
        ),
    ],
)
data class SimilarVerseEntity(
    @ColumnInfo(name = "source_ayah_id")
    val sourceAyahId: Int,
    @ColumnInfo(name = "matched_ayah_id")
    val matchedAyahId: Int,
    @ColumnInfo(name = "matched_words_count")
    val matchedWordsCount: Int,
    @ColumnInfo(name = "coverage")
    val coverage: Int,
    @ColumnInfo(name = "score")
    val score: Int,
    /** JSON: `[[from,to], ...]` 0-based inclusive. */
    @ColumnInfo(name = "match_words")
    val matchWords: String,
)