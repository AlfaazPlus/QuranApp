package com.quranapp.android.db.entities.wbw

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "wbw_audio_timing",
    primaryKeys = ["audio_id", "ayah_id", "word_index"],
    indices = [
        Index(value = ["ayah_id", "word_index"], name = "idx_wbw_audio_timing_ayah_word_index")
    ]
)
data class WbwAudioTimingEntity(
    @ColumnInfo(name = "audio_id")
    val audioId: String,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "word_index")
    val wordIndex: Int,
    @ColumnInfo(name = "start_millis")
    val startMillis: Long,
    @ColumnInfo(name = "end_millis")
    val endMillis: Long,
)
