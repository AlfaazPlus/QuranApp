package com.quranapp.android.activities.popup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.ReaderFactory

class PopupQuranActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    override fun getLayoutResource(): Int = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val data = intent.toQuickReferenceData()

        setContent {
            QuranAppTheme {
                QuickReference(
                    data = data,
                    onOpenInReader = { ch, range ->
                        ReaderFactory.startVerseRange(
                            this@PopupQuranActivity,
                            ch,
                            range.first,
                            range.last,
                        )

                        finish()
                    },
                    onClose = { finish() },
                )
            }
        }
    }
}

/**
 * Maps popup intent extras to [QuickReferenceData], matching in-app QuickReference verse modes.
 */
private fun Intent.toQuickReferenceData(): QuickReferenceData {
    val chapterNo =
        getIntExtra(QuranPopupContract.EXTRA_CHAPTER_NUMBER, 1).coerceIn(QuranMeta.chapterRange)
    val slugs = getStringArrayExtra(QuranPopupContract.EXTRA_TRANSLATION_SLUGS)?.toSet() ?: emptySet()

    return QuickReferenceData(
        slugs = slugs,
        chapterNo = chapterNo,
        verses = getStringExtra(QuranPopupContract.EXTRA_VERSES) ?: "",
        parsedVerses = null,
    )
}
