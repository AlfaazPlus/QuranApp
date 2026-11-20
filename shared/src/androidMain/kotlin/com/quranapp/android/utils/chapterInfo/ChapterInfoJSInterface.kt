package com.quranapp.android.utils.chapterInfo

import android.webkit.JavascriptInterface
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.quran.QuranMeta

class ChapterInfoJSInterface(
    private val verseCount: Int,
    private val onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
) {
    @JavascriptInterface
    fun openReference(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        if (!QuranMeta.isChapterValid(chapterNo)
            || fromVerse < 1 || toVerse < 1
            || fromVerse > toVerse
            || fromVerse > verseCount
            || toVerse > verseCount
        ) {
            Platform.messages.show("Could not open references", long = true)
            return
        }

        onOpenReference(chapterNo, fromVerse, toVerse)
    }
}
