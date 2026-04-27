package com.quranapp.android.utils.chapterInfo

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.univ.MessageUtils

class ChapterInfoJSInterface(
    private val context: Context,
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
            MessageUtils.showRemovableToast(context, "Could not open references", Toast.LENGTH_LONG)
            return
        }

        onOpenReference(chapterNo, fromVerse, toVerse)
    }
}
