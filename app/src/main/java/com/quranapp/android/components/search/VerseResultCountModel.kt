package com.quranapp.android.components.search

import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo

class VerseResultCountModel(val bookInfo: QuranTranslBookInfo?) : SearchResultModelBase() {
    @JvmField
    var resultCount = 0
}
