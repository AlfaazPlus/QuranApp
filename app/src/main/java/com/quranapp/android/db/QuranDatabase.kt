package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranapp.android.db.converters.QuranConverters
import com.quranapp.android.db.dao.ArabicSearchDao
import com.quranapp.android.db.dao.AyahDao
import com.quranapp.android.db.dao.AyahWordDao
import com.quranapp.android.db.dao.MushafDao
import com.quranapp.android.db.dao.NavigationDao
import com.quranapp.android.db.dao.SurahDao
import com.quranapp.android.db.dao.SurahSearchDao
import com.quranapp.android.db.entities.quran.ArabicSearchFtsEntity
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafEntity
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.db.entities.quran.NavigationRangeEntity
import com.quranapp.android.db.entities.quran.ScriptEntity
import com.quranapp.android.db.entities.quran.SurahAliasFtsEntity
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.db.entities.quran.SurahSearchAliasEntity

@Database(
    entities = [
        SurahEntity::class,
        SurahLocalizationEntity::class,
        SurahSearchAliasEntity::class,
        SurahAliasFtsEntity::class,
        AyahEntity::class,
        ScriptEntity::class,
        AyahWordEntity::class,
        NavigationRangeEntity::class,
        MushafEntity::class,
        MushafMapEntity::class,
        ArabicSearchFtsEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(QuranConverters::class)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun arabicSearchDao(): ArabicSearchDao
    abstract fun surahDao(): SurahDao
    abstract fun surahSearchDao(): SurahSearchDao
    abstract fun ayahDao(): AyahDao
    abstract fun ayahWordDao(): AyahWordDao
    abstract fun navigationDao(): NavigationDao
    abstract fun mushafDao(): MushafDao
}