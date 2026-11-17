package com.quranapp.android.db.converters

import androidx.room3.ColumnTypeConverter
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.NavigationType
import com.quranapp.android.db.entities.quran.RevelationType

class QuranConverters {
    @ColumnTypeConverter
    fun fromRevelationType(value: RevelationType?): String? = value?.name

    @ColumnTypeConverter
    fun toRevelationType(value: String?): RevelationType? =
        value?.let { RevelationType.valueOf(it) }

    @ColumnTypeConverter
    fun fromNavigationType(value: NavigationType?): String? = value?.name

    @ColumnTypeConverter
    fun toNavigationType(value: String?): NavigationType? =
        value?.let { NavigationType.valueOf(it) }

    @ColumnTypeConverter
    fun fromMushafLineType(value: MushafLineType?): String? = value?.name

    @ColumnTypeConverter
    fun toMushafLineType(value: String?): MushafLineType? =
        value?.let { MushafLineType.valueOf(it) }
}
