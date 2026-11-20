package com.quranapp.android.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private object LegacyBackupBookmarkJsonKeys {
    const val ID = "id"
    const val CHAPTER_NO = "cn"
    const val FROM_VERSE_NO = "fvn"
    const val TO_VERSE_NO = "tvn"
    const val DATE = "dt"
    const val NOTE = "nt"
}

data class BookmarkModel(
    val id: Long,
    val chapterNo: Int,
    val fromVerseNo: Int,
    val toVerseNo: Int,
    val date: String?,
    var note: String? = null
) {
    companion object {
        fun fromJson(bookmarks: JsonArray?): List<BookmarkModel> {
            val bookmarkList = arrayListOf<BookmarkModel>()

            bookmarks?.forEach { b ->
                val chapterNo =
                    b.jsonObject[LegacyBackupBookmarkJsonKeys.CHAPTER_NO]?.jsonPrimitive?.intOrNull
                        ?: -1
                val fromVerseNo =
                    b.jsonObject[LegacyBackupBookmarkJsonKeys.FROM_VERSE_NO]?.jsonPrimitive?.intOrNull
                        ?: -1
                val toVerseNo =
                    b.jsonObject[LegacyBackupBookmarkJsonKeys.TO_VERSE_NO]?.jsonPrimitive?.intOrNull
                        ?: -1
                val date =
                    b.jsonObject[LegacyBackupBookmarkJsonKeys.DATE]?.jsonPrimitive?.contentOrNull
                val note =
                    b.jsonObject[LegacyBackupBookmarkJsonKeys.NOTE]?.jsonPrimitive?.contentOrNull

                if (chapterNo != -1 && fromVerseNo != -1 && toVerseNo != -1) {
                    bookmarkList.add(
                        BookmarkModel(
                            id = -1,
                            chapterNo = chapterNo,
                            fromVerseNo = fromVerseNo,
                            toVerseNo = toVerseNo,
                            date = date,
                        ).also {
                            it.note = note
                        })
                }
            }

            return bookmarkList
        }
    }
}
