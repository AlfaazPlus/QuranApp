package com.quranapp.android.db.tafsir

import android.provider.BaseColumns

class QuranTafsirContract private constructor() {
    object QuranTafsirEntry : BaseColumns {
        const val TABLE_NAME: String = "Tafsir"

        /**
         * <chapter_no>:<verse_no>
         */
        const val _ID: String = "id"
        const val COL_TAFSIR_KEY: String = "tafsir_key"
        const val COL_CHAPTER_NO: String = "chapter_no"
        const val COL_FROM_VERSE_NO: String = "from_verse_no"
        const val COL_TO_VERSE_NO: String = "to_verse_no"
        const val COL_TEXT: String = "text"
        const val COL_VERSION: String = "version"
        const val COL_LAST_UPDATED: String = "last_updated"
    }
}