package com.quranapp.android.utils.miscellenous

import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.format.format
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant


object DateUtils {
    const val SYSTEM = "yyyy-MM-dd HH:mm:ss"
    const val USER = "dd-MM-yyyy hh:mm a"
    const val SHORT = "dd MMM, yyyy hh:mm a"
    const val FILENAME = "yyyyMMddHHmmssSSS"

    private val zone get() = TimeZone.currentSystemDefault()

    private val formats = mapOf(
        SYSTEM to DateTimeComponents.Format {
            year(); char('-'); monthNumber(); char('-'); day()
            char(' ')
            hour(); char(':'); minute(); char(':'); second()
        },
        USER to DateTimeComponents.Format {
            day(); char('-'); monthNumber(); char('-'); year()
            char(' ')
            amPmHour(); char(':'); minute(); char(' ')
            amPmMarker("AM", "PM")
        },
        SHORT to DateTimeComponents.Format {
            day(); char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(','); char(' '); year()
            char(' ')
            amPmHour(); char(':'); minute(); char(' ')
            amPmMarker("AM", "PM")
        },
        FILENAME to DateTimeComponents.Format {
            year(); monthNumber(); day()
            hour(); minute(); second()
            secondFraction(3, 3)
        }
    )

    fun now(): Instant =
        Clock.System.now()

    fun parse(value: String, format: String): Instant? = try {
        formats[format]?.parse(value)
            ?.toLocalDateTime()
            ?.toInstant(zone)
    } catch (_: Exception) {
        null
    }

    fun format(date: Instant, format: String = SHORT): String {
        val formatter = formats[format]
            ?: throw IllegalArgumentException("Unsupported format: $format")

        return formatter.format {
            setDateTime(date.toLocalDateTime(zone))
        }
    }

    fun durationFormatted(milliseconds: Long): String {
        if (milliseconds <= 0) return "0:00"

        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return if (hours > 0) {
            "$hours:${minutes.pad()}:${remainingSeconds.pad()}"
        } else {
            "${minutes.pad()}:${remainingSeconds.pad()}"
        }
    }

    private fun Long.pad() = toString().padStart(2, '0')
}
