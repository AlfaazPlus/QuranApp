package com.quranapp.android.utils.univ

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    const val DATETIME_FORMAT_SYSTEM = "yyyy-MM-dd HH:mm:ss"
    const val DATETIME_FORMAT_USER = "dd-MM-yyyy hh:mm a"
    fun toDate(dateStr: String, format: String): Date? {
        val formatter = SimpleDateFormat(format, Locale.ENGLISH)

        return try {
            formatter.parse(dateStr)
        } catch (e: ParseException) {
            null
        }
    }

    fun format(date: Date, format: String): String {
        return SimpleDateFormat(format, Locale.ENGLISH).format(date)
    }

    fun getDateTimeNow(format: String?): String {
        return SimpleDateFormat(format, Locale.ENGLISH).format(Calendar.getInstance().time)
    }

    @JvmStatic
    val dateTimeNow get() = getDateTimeNow(DATETIME_FORMAT_SYSTEM)
}