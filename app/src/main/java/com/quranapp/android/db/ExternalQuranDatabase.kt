package com.quranapp.android.db

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.quranapp.android.db.converters.QuranConverters
import com.quranapp.android.db.dao.AtlasWordShapeDao
import com.quranapp.android.db.dao.WbwDao
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.db.entities.wbw.WbwAudioTimingEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity

@Database(
    entities = [
        WbwWordEntity::class,
        WbwAudioTimingEntity::class,
        AtlasBundleEntity::class,
        AtlasWordShapeEntity::class,
    ],
    version = 4,
    exportSchema = true
)
@ColumnTypeConverters(QuranConverters::class)
abstract class ExternalQuranDatabase : RoomDatabase() {
    abstract fun wbwDao(): WbwDao
    abstract fun atlasWordShapeDao(): AtlasWordShapeDao
}
