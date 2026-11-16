package com.quranapp.android.components.readHistory

class ReadHistoryModel(
    val id: Long,
    val readType: Int,
    val readerStyle: Int,
    val juzNo: Int,
    val chapterNo: Int,
    val fromVerseNo: Int,
    val toVerseNo: Int,
    val date: String?
) {

    fun copy(): ReadHistoryModel {
        return ReadHistoryModel(
            id,
            readType,
            readerStyle,
            juzNo,
            chapterNo,
            fromVerseNo,
            toVerseNo,
            date
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadHistoryModel) return false

        if (id != other.id) return false
        if (readType != other.readType) return false
        if (readerStyle != other.readerStyle) return false
        if (juzNo != other.juzNo) return false
        if (chapterNo != other.chapterNo) return false
        if (fromVerseNo != other.fromVerseNo) return false
        if (toVerseNo != other.toVerseNo) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + readType
        result = 31 * result + readerStyle
        result = 31 * result + juzNo
        result = 31 * result + chapterNo
        result = 31 * result + fromVerseNo
        result = 31 * result + toVerseNo
        result = 31 * result + (date?.hashCode() ?: 0)
        return result
    }
}
