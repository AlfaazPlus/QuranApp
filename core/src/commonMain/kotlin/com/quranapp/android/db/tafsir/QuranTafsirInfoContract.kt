package com.quranapp.android.db.tafsir

class QuranTafsirInfoContract private constructor() {
    object QuranTafsirInfoEntry {
        const val TABLE_NAME: String = "QuranTafsirBookInfo"
        const val COL_TAFSIR_KEY: String = "tafsir_key"
        const val COL_LANG_CODE: String = "lang_code"
        const val COL_LANG_NAME: String = "lang_name"
        const val COL_BOOK_NAME: String = "book_name"
        const val COL_AUTHOR_NAME: String = "author_name"
        const val COL_IS_DOWNLOADED: String = "is_downloaded"
        const val COL_LAST_UPDATED: String = "last_updated"
    }
}