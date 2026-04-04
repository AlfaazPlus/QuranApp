package com.quranapp.android.db.bookmark

import com.quranapp.android.components.bookmark.BookmarkModel

fun BookmarkEntity.toModel(): BookmarkModel {
    return BookmarkModel(
        id = id ?: -1,
        chapterNo = chapterNo ?: -1,
        fromVerseNo = fromVerseNo ?: -1,
        toVerseNo = toVerseNo ?: -1,
        date = dateTime,
        note = note
    )
}

fun BookmarkModel.toEntity(): BookmarkEntity {
    return BookmarkEntity(
        id = id,
        chapterNo = chapterNo,
        fromVerseNo = fromVerseNo,
        toVerseNo = toVerseNo,
        dateTime = date,
        note = note
    )
}