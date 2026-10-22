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

@Database(
    entities = [
        TopicEntity::class,
        TopicLocalizationEntity::class,
        TopicAyahEntity::class,
        RelationshipEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@ColumnTypeConverters(QuranConverters::class, TopicsDbConverters::class)
abstract class TopicsDatabase : RoomDatabase() {
    abstract fun topicsDao(): TopicsDao
}
