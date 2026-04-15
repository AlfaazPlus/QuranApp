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

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}