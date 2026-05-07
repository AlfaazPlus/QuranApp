package com.quranapp.android.db.entities.topics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "topic_ayahs",
    primaryKeys = ["topic_id", "ayah_id"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_topic_ayahs_ayah", value = ["ayah_id"]),
        Index(name = "idx_topic_ayahs_topic", value = ["topic_id"]),
    ],
)
data class TopicAyahEntity(
    @ColumnInfo(name = "topic_id")
    val topicId: Int,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
)
