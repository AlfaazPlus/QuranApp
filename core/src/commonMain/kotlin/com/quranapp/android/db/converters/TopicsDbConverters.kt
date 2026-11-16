package com.quranapp.android.db.converters

import androidx.room3.ColumnTypeConverter
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags

class TopicsDbConverters {
    @ColumnTypeConverter
    fun fromRelationshipType(value: RelationshipType?): String? = value?.dbValue

    @ColumnTypeConverter
    fun toRelationshipType(value: String?): RelationshipType? =
        value?.let { RelationshipType.fromDbValue(it) }

    @ColumnTypeConverter
    fun fromTopicFlags(value: TopicFlags?): Int? = value?.dbValue

    @ColumnTypeConverter
    fun toTopicFlags(value: Int?): TopicFlags? =
        value?.let { TopicFlags.fromDbValue(it) }
}
