package com.quranapp.android.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranapp.android.components.readHistory.ReadHistoryModel

@Entity(tableName = "read_history")
data class ReadHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "read_type") val readType: Int,
    @ColumnInfo(name = "read_style") val readStyle: Int,
    @ColumnInfo(name = "juz_number") val juzNo: Int,
    @ColumnInfo(name = "chapter_number") val chapterNo: Int,
    @ColumnInfo(name = "from_verse_number") val fromVerseNo: Int,
    @ColumnInfo(name = "to_verse_number") val toVerseNo: Int,
    @ColumnInfo(name = "date") val date: String
)

fun ReadHistory.mapToUiModel(): ReadHistoryModel {
    return ReadHistoryModel(
        id = id,
        readType = readType,
        readerStyle = readStyle,
        juzNo = juzNo,
        chapterNo = chapterNo,
        fromVerseNo = fromVerseNo,
        toVerseNo = toVerseNo,
        date = date
    )
}