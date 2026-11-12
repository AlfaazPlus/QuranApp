package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.activities.reference.ActivityExclusiveVerses.Companion.intent
import com.quranapp.android.compose.screens.shareimage.ShareAsImageScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.univ.Keys

class ActivityShareAsImage : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, 1)
        val verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, 1)

        setContent {
            QuranAppTheme {
                ShareAsImageScreen(
                    chapterNo = chapterNo,
                    verseNo = verseNo,
                )
            }
        }
    }
}
