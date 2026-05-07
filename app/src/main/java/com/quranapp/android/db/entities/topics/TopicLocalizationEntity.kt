package com.quranapp.android.db.entities.topics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "topic_localizations",
    primaryKeys = ["topic_id", "lang_code"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(name = "idx_topic_localizations_lang", value = ["lang_code"]),
        Index(name = "idx_topic_localizations_title", value = ["title"]),
    ],
)
data class TopicLocalizationEntity(
    @ColumnInfo(name = "topic_id")
    val topicId: Int,
    @ColumnInfo(name = "lang_code")
    val langCode: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "short_description")
    val shortDescription: String?,
    @ColumnInfo(name = "description")
    val description: String?,
)
