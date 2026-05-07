package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 3,
    exportSchema = false
)
@TypeConverters(QuranConverters::class, TopicsDbConverters::class)
abstract class TopicsDatabase : RoomDatabase() {
    abstract fun topicsDao(): TopicsDao
}
