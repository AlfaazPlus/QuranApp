package com.quranapp.android.db.converters

import androidx.room3.ColumnTypeConverter
import java.util.Date

class DbConverters {
    @ColumnTypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @ColumnTypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}