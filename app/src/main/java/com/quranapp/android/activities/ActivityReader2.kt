package com.quranapp.android.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.reader.ReaderScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ
import com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_VERSES
import com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_PAGE
import com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_TRANSLATION
import com.quranapp.android.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.asIntRange
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.Keys.READER_KEY_CHAPTER_NO
import com.quranapp.android.utils.univ.Keys.READER_KEY_JUZ_NO
import com.quranapp.android.utils.univ.Keys.READER_KEY_READER_STYLE
import com.quranapp.android.utils.univ.Keys.READER_KEY_READ_TYPE
import com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS
import com.quranapp.android.utils.univ.Keys.READER_KEY_VERSES
import com.quranapp.android.viewModels.ReaderIntentData
import kotlinx.coroutines.flow.MutableStateFlow

class ActivityReader2 : BaseActivity() {
    val intentFlow = MutableStateFlow<Intent?>(intent)

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContent {
            val intent by intentFlow.collectAsState(initial = this.intent)

            QuranAppTheme {
                ReaderScreen(
                    resolveReaderIntentData(intent)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent
        intentFlow.value = intent
    }

    private fun resolveReaderIntentData(intent: Intent?): ReaderIntentData {
        if (intent == null) return ReaderIntentData.FullChapter(1)

        try {
            validateIntent(intent)
        } catch (e: Exception) {
            Log.saveError(e, "resolveReaderIntentData")
        }

        val readType = intent.getIntExtra(READER_KEY_READ_TYPE, ReaderParams.defaultReadType())

        var initialJuzNo = intent.getIntExtra(READER_KEY_JUZ_NO, 1)
        var initialChapterNo = intent.getIntExtra(READER_KEY_CHAPTER_NO, 1)
        var initVerses = resolveIntentVerseRange(intent)

        val intentData = when (readType) {

            READER_READ_TYPE_JUZ -> {
                ReaderIntentData.FullJuz(initialJuzNo)
            }

            READER_READ_TYPE_VERSES -> {
                if (initVerses == null) {
                    ReaderIntentData.FullChapter(initialChapterNo)
                } else {
                    ReaderIntentData.VerseRange(initialChapterNo, initVerses.asIntRange)
                }
            }

            else -> {
                ReaderIntentData.FullChapter(initialChapterNo)
            }
        }

        return intentData.apply {
            slugs = intent.getStringArrayExtra(READER_KEY_TRANSL_SLUGS)?.toSet()
                ?: ReaderPreferences.getTranslations()

            readerMode = intent.getIntExtra(
                READER_KEY_READER_STYLE,
                ReaderPreferences.getReaderStyle()
            )
        }
    }

    private fun resolveIntentVerseRange(intent: Intent): Pair<Int, Int>? {
        val serializable = intent.getSerializableExtra(READER_KEY_VERSES)

        return when (serializable) {
            is Pair<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                serializable as Pair<Int, Int>
            }

            is IntArray -> {
                serializable[0] to serializable[1]
            }

            else -> null
        }
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

                val verseRange: Pair<Int, Int>
                var splits = secondSeg.split("-")

                verseRange = if (splits.size >= 2) {
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
            intent.putExtra(
                READER_KEY_READER_STYLE,
                if (reading) READER_STYLE_PAGE else READER_STYLE_TRANSLATION
            )
        }
    }

    private fun validateQuranAppIntent(intent: Intent) {
        val requestedTranslSlugs = intent.getStringArrayExtra("translations")
        if (requestedTranslSlugs != null) {
            intent.putExtra(
                READER_KEY_TRANSL_SLUGS,
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
