package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranapp.android.db.converters.QuranConverters
import com.quranapp.android.db.dao.AtlasWordShapeDao
import com.quranapp.android.db.dao.WbwDao
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity

@Database(
    entities = [
        WbwWordEntity::class,
        AtlasBundleEntity::class,
        AtlasWordShapeEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(QuranConverters::class)
abstract class ExternalQuranDatabase : RoomDatabase() {
    abstract fun wbwDao(): WbwDao
    abstract fun atlasWordShapeDao(): AtlasWordShapeDao
}
