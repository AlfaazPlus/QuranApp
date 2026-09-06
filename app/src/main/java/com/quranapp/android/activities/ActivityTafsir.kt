package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.tafsir.TafsirReaderScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.TafsirReaderEvent
import com.quranapp.android.viewModels.TafsirReaderViewModel

class ActivityTafsir : BaseActivity() {

    private val viewModel: TafsirReaderViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initContent(intent)
    }

    override fun getLayoutResource() = 0

    override fun onActivityInflated(
        activityView: View,
        savedInstanceState: Bundle?
    ) {
        setContent {
            QuranAppTheme {
                TafsirReaderScreen()
            }
        }

        TafsirManager.prepare(this, false) {
            initContent(intent)
        }
    }

    private fun initContent(intent: Intent) {
        val tafsirKey = intent.getStringExtra("tafsirKey")
        val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, 1)
        val verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, 1)

        if (chapterNo < 1 || verseNo < 1) {
            finish()
            return
        }

        viewModel.onEvent(
            TafsirReaderEvent.Init(
                tafsirKey,
                chapterNo,
                verseNo,
            ),
        )
    }
}
