package com.quranapp.android.components.bookmark

import android.content.Context
import com.quranapp.android.R
import com.quranapp.android.utils.univ.DateUtils
import java.text.MessageFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*

class BookmarkModel(
    val id: Long,
    val chapterNo: Int,
    val fromVerseNo: Int,
    val toVerseNo: Int,
    val date: String?
) {
    var note: String? = null

    fun getFormattedDate(ctx: Context): String? {
        val oldFormatter = DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT_SYSTEM)
        try {
            val oldDate = oldFormatter.parse(date)
            val year = oldDate.get(ChronoField.YEAR)
            val month = oldDate.get(ChronoField.MONTH_OF_YEAR)
            val day = oldDate.get(ChronoField.DAY_OF_MONTH)

            val cal = Calendar.getInstance()
            return if (cal.get(Calendar.YEAR) == year) {
                if (cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.DAY_OF_MONTH) == day) {
                    ctx.getString(
                        R.string.strMsgBookmarkDateToday,
                        DateTimeFormatter.ofPattern("hh:mm a").format(oldDate)
                    )
                } else {
                    val datePart1 = DateTimeFormatter.ofPattern("dd MMM").format(oldDate)
                    val datePart2 = DateTimeFormatter.ofPattern("hh:mm a").format(oldDate)
                    ctx.getString(R.string.strMsgBookmarkDate, datePart1, datePart2)
                }
            } else {
                val datePart1 = DateTimeFormatter.ofPattern("dd MMM yyyy").format(oldDate)
                val datePart2 = DateTimeFormatter.ofPattern("hh:mm a").format(oldDate)
                ctx.getString(R.string.strMsgBookmarkDate, datePart1, datePart2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun copy(): BookmarkModel {
        return BookmarkModel(id, chapterNo, fromVerseNo, toVerseNo, date).apply {
            note = this@BookmarkModel.note
        }
    }

    override fun toString(): String {
        return MessageFormat.format("(VERSE: chapterNo: {0}, verseNo: {1})", chapterNo, fromVerseNo)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BookmarkModel) return false

        return id == other.id &&
            chapterNo == other.chapterNo &&
            fromVerseNo == other.fromVerseNo &&
            toVerseNo == other.toVerseNo &&
            date == other.date &&
            note == other.note
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + chapterNo
        result = 31 * result + fromVerseNo
        result = 31 * result + toVerseNo
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }
}
