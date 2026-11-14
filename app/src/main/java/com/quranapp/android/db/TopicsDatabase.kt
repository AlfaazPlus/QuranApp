package com.quranapp.android.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters
import com.quranapp.android.db.converters.QuranConverters
import com.quranapp.android.db.converters.TopicsDbConverters
import com.quranapp.android.db.dao.TopicsDao
import com.quranapp.android.db.entities.topics.RelationshipEntity
import com.quranapp.android.db.entities.topics.TopicAyahEntity
import com.quranapp.android.db.entities.topics.TopicEntity
import com.quranapp.android.db.entities.topics.TopicLocalizationEntity

const val TOPICS_DATABASE_NAME = "topics"
const val TOPICS_DATABASE_VERSION = 2

@Database(
    entities = [
        TopicEntity::class,
        TopicLocalizationEntity::class,
        TopicAyahEntity::class,
        RelationshipEntity::class,
    ],
    version = TOPICS_DATABASE_VERSION,
    exportSchema = false
)
@ColumnTypeConverters(QuranConverters::class, TopicsDbConverters::class)
abstract class TopicsDatabase : RoomDatabase() {
    abstract fun topicsDao(): TopicsDao
}
