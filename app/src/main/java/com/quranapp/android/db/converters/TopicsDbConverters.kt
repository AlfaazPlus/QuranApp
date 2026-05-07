package com.quranapp.android.db.converters

import androidx.room.TypeConverter
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.NavigationType
import com.quranapp.android.db.entities.quran.RevelationType

class TopicsDbConverters {
    @TypeConverter
    fun fromRelationshipType(value: RelationshipType?): String? = value?.dbValue

    @TypeConverter
    fun toRelationshipType(value: String?): RelationshipType? =
        value?.let { RelationshipType.fromDbValue(it) }

    @TypeConverter
    fun fromTopicFlags(value: TopicFlags?): Int? = value?.dbValue

    @TypeConverter
    fun toTopicFlags(value: Int?): TopicFlags? =
        value?.let { TopicFlags.fromDbValue(it) }
}
