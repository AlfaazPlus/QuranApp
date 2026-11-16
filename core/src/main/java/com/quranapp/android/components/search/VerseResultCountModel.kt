package com.quranapp.android.components.search

import com.quranapp.android.api.models.translation.TranslationBookInfoModel

class VerseResultCountModel(val bookInfo: TranslationBookInfoModel?) : SearchResultModelBase() {
    @JvmField
    var resultCount = 0
}
