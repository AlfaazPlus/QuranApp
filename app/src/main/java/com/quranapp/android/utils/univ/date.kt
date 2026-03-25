package com.quranapp.android.utils.univ

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun String.toDate(format: String): Date? {
    val formatter = SimpleDateFormat(format, Locale.ENGLISH)

    return try {
        formatter.parse(this)
    } catch (_: Exception) {
        null
    }
}

fun Date.formatted(format: String? = null): String {
    return formatDateTimeShort(this, format)
}

fun formatDateTimeShort(date: Date, format: String? = null): String {
    return DateFormat
        .format(format ?: "dd MMM, yyyy hh:mm a", date)
        .toString()
}

fun getDateTimeNow(format: String?): String {
    return SimpleDateFormat(format, Locale.ENGLISH).format(Calendar.getInstance().time)
}