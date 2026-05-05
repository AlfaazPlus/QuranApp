package com.quranapp.android.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.screens.reader.ReaderScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ActivityReader : BaseActivity() {
    private val readerVm: ReaderViewModel by viewModels()
    val intentFlow = MutableStateFlow<Intent?>(null)

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(isTaskRoot) {
            override fun handleOnBackPressed() {
                launchMainActivity()
                finish()
            }
        })

        intentFlow.value = intent

        setContent {
            val currentIntent by intentFlow.collectAsState()

            QuranAppTheme {
                ReaderScreen(
                    resolveReaderLaunchParams(currentIntent)
                    // ReaderLaunchParams(ReaderIntentData.FullChapter(1))
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        readerVm.saveReadHistory()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        intentFlow.value = intent
    }

    private fun resolveReaderLaunchParams(intent: Intent?): ReaderLaunchParams {
        if (intent == null) return ReaderLaunchParams(ReaderIntentData.FullChapter(1))

        try {
            validateIntent(intent)
        } catch (e: Exception) {
            Log.saveError(e, "resolveReaderLaunchParams")
        }

        return ReaderLaunchParams.fromIntent(intent)
    }

    private fun validateIntent(intent: Intent) {
        val action = intent.action

        if (Intent.ACTION_VIEW == action) {
            val url = intent.data ?: return

            if (url.host.equals("quran.com", ignoreCase = true)) {
                validateQuranComIntent(intent, url)
            }
        } else if (INTENT_ACTION_OPEN_READER.equals(intent.action, ignoreCase = true)) {
            validateQuranAppIntent(intent)
        }

        intent.action = null
    }

    private fun validateQuranComIntent(intent: Intent, url: Uri) {
        val pathSegments = url.pathSegments

        if (pathSegments.size >= 2) {
            val firstSeg = pathSegments[0]
            val secondSeg = pathSegments[1]

            if (firstSeg.equals("juz", ignoreCase = true)) {
                val juzNo = secondSeg.toInt()
                intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo))
            } else {
                val chapterNo = firstSeg.toInt()

                val splits = secondSeg.split("-")

                val verseRange = if (splits.size >= 2) {
                    Pair(splits[0].toInt(), splits[1].toInt())
                } else {
                    val verseNo = splits[0].toInt()
                    Pair(verseNo, verseNo)
                }

                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange))
            }
        } else if (pathSegments.size == 1) {
            var splits = pathSegments[0].split(":")
            val chapterNo = splits[0].toInt()

            if (splits.size >= 2) {
                splits = splits[1].split("-")

                val verseRange: Pair<Int, Int> = if (splits.size >= 2) {
                    Pair(splits[0].toInt(), splits[1].toInt())
                } else {
                    val verseNo = splits[0].toInt()
                    Pair(verseNo, verseNo)
                }

                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange))
            } else {
                intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo))
            }
        }

        if (url.queryParameterNames.contains("reading")) {
            val reading = url.getBooleanQueryParameter("reading", false)
            val mode = if (reading) ReaderMode.Reading else ReaderMode.VerseByVerse
            intent.putExtra(ReaderLaunchParams.EXTERNAL_KEY_READER_MODE, mode.value)
        }
    }

    private fun validateQuranAppIntent(intent: Intent) {
        val requestedTranslSlugs = intent.getStringArrayExtra("translations")
        if (requestedTranslSlugs != null) {
            intent.putExtra(
                ReaderLaunchParams.EXTERNAL_KEY_TRANSL_SLUGS,
                requestedTranslSlugs.toCollection(sortedSetOf())
            )
        }

        if (intent.getBooleanExtra("isJuz", false)) {
            val juzNo = intent.getIntExtra("juzNo", -1)
            intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo))
        } else {
            val chapterNo = intent.getIntExtra("chapterNo", -1)
            val verses = intent.getIntArrayExtra("verses")
            val verseNo = intent.getIntExtra("verseNo", -1)

            when {
                verses != null -> {
                    intent.putExtras(
                        ReaderFactory.prepareVerseRangeIntent(
                            chapterNo,
                            verses[0],
                            verses[1]
                        )
                    )
                }

                verseNo != -1 -> {
                    intent.putExtras(
                        ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo)
                    )
                }

                else -> {
                    intent.putExtras(
                        ReaderFactory.prepareChapterIntent(chapterNo)
                    )
                }
            }
        }
    }
}
