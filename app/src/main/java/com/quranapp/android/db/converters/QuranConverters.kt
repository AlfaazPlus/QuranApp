package com.quranapp.android.db.converters

import androidx.room.TypeConverter
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.NavigationType
import com.quranapp.android.db.entities.quran.RevelationType

class QuranConverters {
    @TypeConverter
    fun fromRevelationType(value: RevelationType?): String? = value?.name

    @TypeConverter
    fun toRevelationType(value: String?): RevelationType? =
        value?.let { RevelationType.valueOf(it) }

    @TypeConverter
    fun fromNavigationType(value: NavigationType?): String? = value?.name

    @TypeConverter
    fun toNavigationType(value: String?): NavigationType? =
        value?.let { NavigationType.valueOf(it) }

    @TypeConverter
    fun fromMushafLineType(value: MushafLineType?): String? = value?.name

    @TypeConverter
    fun toMushafLineType(value: String?): MushafLineType? =
        value?.let { MushafLineType.valueOf(it) }
}