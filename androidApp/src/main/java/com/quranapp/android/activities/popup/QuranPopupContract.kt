package com.quranapp.android.activities.popup

import android.content.Context
import android.content.Intent

object QuranPopupContract {
    const val ACTION_SHOW_POPUP = "com.quranapp.android.action.SHOW_POPUP"
    const val EXTRA_CHAPTER_NUMBER = "chapterNo"
    const val EXTRA_VERSES = "verses"
    const val EXTRA_TRANSLATION_SLUGS = "translationSlugs"
}

fun Context.buildShowQuranPopupIntent(
    chapterNo: Int,
    versesSpec: String,
    translationSlugs: Array<String>? = null,
): Intent = Intent(QuranPopupContract.ACTION_SHOW_POPUP).apply {
    setClass(this@buildShowQuranPopupIntent, PopupQuranActivity::class.java)

    putExtra(QuranPopupContract.EXTRA_CHAPTER_NUMBER, chapterNo)
    putExtra(QuranPopupContract.EXTRA_VERSES, versesSpec)

    translationSlugs?.takeIf { it.isNotEmpty() }?.let {
        putExtra(QuranPopupContract.EXTRA_TRANSLATION_SLUGS, it)
    }
}
