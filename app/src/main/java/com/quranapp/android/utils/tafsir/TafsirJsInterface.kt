package com.quranapp.android.utils.tafsir

import android.webkit.JavascriptInterface
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.ActivityTafsir
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareSingleVerseIntent
import com.quranapp.android.utils.univ.Codes
import com.quranapp.android.utils.univ.Keys

class TafsirJsInterface(private val activity: ActivityTafsir) {
    @JavascriptInterface
    fun goToVerse() {
        var intent = prepareSingleVerseIntent(activity.chapterNo, activity.verseNo)

        val callingActivity = activity.callingActivity
        if (callingActivity != null && ActivityReader::class.java.name == callingActivity.className) {
            activity.setResult(Codes.OPEN_REFERENCE_RESULT_CODE, intent)
            activity.finish()
        } else {
            intent = intent.setClass(activity, ActivityReader::class.java)
            activity.startActivity(intent)
        }
    }


    @JavascriptInterface
    fun previousTafsir() {
        if (activity.verseNo == 1) {
            return
        }

        val intent = activity.intent.also {
            it.putExtra(Keys.READER_KEY_VERSE_NO, activity.verseNo - 1)
            it.action = null
        }

        activity.startActivity(intent)
    }

    @JavascriptInterface
    fun nextTafsir() {
        if (activity.verseNo == activity.mQuranMetaRef.get()!!.getChapterVerseCount(activity.chapterNo)) {
            return
        }

        val intent = activity.intent.also {
            it.putExtra(Keys.READER_KEY_VERSE_NO, activity.verseNo + 1)
            it.action = null
        }

        activity.startActivity(intent)
    }

    @JavascriptInterface
    fun goToTop() {
        activity.scrollToTop()
    }
}