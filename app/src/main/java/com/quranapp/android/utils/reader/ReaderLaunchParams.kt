package com.quranapp.android.utils.reader

import android.content.Intent
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.utils.reader.ReaderLaunchParams.Companion.fromIntent

enum class ReadType(val value: String) {
    Chapter("chapter"),
    Juz("juz"),
    Hizb("hizb");

    companion object {
        fun fromValue(value: String?): ReadType {
            return entries.find { it.value == value } ?: Chapter
        }

        fun fromLegacyInt(type: Int): ReadType = when (type) {
            0x5 -> Juz
            else -> Chapter
        }
    }
}

sealed class ReaderIntentData {
    open val initialVerse: ChapterVersePair? get() = null

    data class FullChapter(
        val chapterNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullJuz(
        val juzNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullHizb(
        val hizbNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class MushafPage(
        val mushafId: Int,
        val pageNo: Int,
        val fallbackChapterNo: Int = 0,
        val fallbackVerseNo: Int = 0,
    ) : ReaderIntentData()
}

data class ReaderLaunchParams(
    val data: ReaderIntentData,
    val readerMode: ReaderMode? = null,
    val slugs: Set<String>? = null,
) {
    fun toIntent(): Intent {
        val d = data

        return Intent().apply {
            when (d) {
                is ReaderIntentData.FullChapter -> {
                    putExtra(KEY_READ_TYPE, ReadType.Chapter.value)
                    putExtra(KEY_CHAPTER_NO, d.chapterNo)
                    d.initialVerse?.let {
                        putExtra(KEY_INITIAL_VERSE_CHAPTER, it.chapterNo)
                        putExtra(KEY_INITIAL_VERSE_NO, it.verseNo)
                    }
                }

                is ReaderIntentData.FullJuz -> {
                    putExtra(KEY_READ_TYPE, ReadType.Juz.value)
                    putExtra(KEY_JUZ_NO, d.juzNo)
                    d.initialVerse?.let {
                        putExtra(KEY_INITIAL_VERSE_CHAPTER, it.chapterNo)
                        putExtra(KEY_INITIAL_VERSE_NO, it.verseNo)
                    }
                }

                is ReaderIntentData.FullHizb -> {
                    putExtra(KEY_READ_TYPE, ReadType.Hizb.value)
                    putExtra(KEY_HIZB_NO, d.hizbNo)
                    d.initialVerse?.let {
                        putExtra(KEY_INITIAL_VERSE_CHAPTER, it.chapterNo)
                        putExtra(KEY_INITIAL_VERSE_NO, it.verseNo)
                    }
                }

                is ReaderIntentData.MushafPage -> {
                    putExtra(KEY_MUSHAF_ID, d.mushafId)
                    putExtra(KEY_RESTORE_PAGE, d.pageNo)
                    putExtra(KEY_CHAPTER_NO, d.fallbackChapterNo)
                    putExtra(KEY_FALLBACK_VERSE, d.fallbackVerseNo)
                }
            }

            readerMode?.let { putExtra(KEY_READER_MODE, it.value) }
            slugs?.let { putExtra(KEY_TRANSL_SLUGS, it.toTypedArray()) }
        }
    }

    companion object {
        private const val KEY_READ_TYPE = "reader.read_type"
        private const val KEY_CHAPTER_NO = "reader.chapter_no"
        private const val KEY_JUZ_NO = "reader.juz_no"
        private const val KEY_HIZB_NO = "reader.hizb_no"
        private const val KEY_MUSHAF_ID = "reader.mushaf_id"
        private const val KEY_RESTORE_PAGE = "reader.restore_page"
        private const val KEY_FALLBACK_VERSE = "reader.fallback_verse"
        private const val KEY_INITIAL_VERSE_CHAPTER = "reader.initial_verse_chapter"
        private const val KEY_INITIAL_VERSE_NO = "reader.initial_verse_no"

        private const val KEY_READER_MODE = "reader.mode"
        private const val KEY_TRANSL_SLUGS = "reader.translation_slugs"

        /** Exposed for external intent builders (deep links, app intents) that add extras before [fromIntent]. */
        const val EXTERNAL_KEY_READER_MODE = KEY_READER_MODE
        const val EXTERNAL_KEY_TRANSL_SLUGS = KEY_TRANSL_SLUGS

        fun fromIntent(intent: Intent): ReaderLaunchParams {
            val mushafId = intent.getIntExtra(KEY_MUSHAF_ID, -1)
            val restorePage = intent.getIntExtra(KEY_RESTORE_PAGE, -1)

            if (mushafId > 0 && restorePage > 0) {
                return ReaderLaunchParams(
                    data = ReaderIntentData.MushafPage(
                        mushafId = mushafId,
                        pageNo = restorePage,
                        fallbackChapterNo = intent.getIntExtra(KEY_CHAPTER_NO, 1),
                        fallbackVerseNo = intent.getIntExtra(KEY_FALLBACK_VERSE, 1),
                    ),
                    readerMode = intent.getStringExtra(KEY_READER_MODE)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { ReaderMode.fromValue(it) },
                    slugs = intent.getStringArrayExtra(KEY_TRANSL_SLUGS)?.toSet(),
                )
            }

            val readType = ReadType.fromValue(intent.getStringExtra(KEY_READ_TYPE))

            val initialVerseChapter = intent.getIntExtra(KEY_INITIAL_VERSE_CHAPTER, -1)
            val initialVerseNo = intent.getIntExtra(KEY_INITIAL_VERSE_NO, -1)
            val initialVerse = if (initialVerseChapter > 0 && initialVerseNo > 0) {
                ChapterVersePair(initialVerseChapter, initialVerseNo)
            } else null

            val data = when (readType) {
                ReadType.Juz -> ReaderIntentData.FullJuz(
                    juzNo = intent.getIntExtra(KEY_JUZ_NO, 1),
                    initialVerse = initialVerse,
                )

                ReadType.Hizb -> ReaderIntentData.FullHizb(
                    hizbNo = intent.getIntExtra(KEY_HIZB_NO, 1),
                    initialVerse = initialVerse,
                )

                ReadType.Chapter -> ReaderIntentData.FullChapter(
                    chapterNo = intent.getIntExtra(KEY_CHAPTER_NO, 1),
                    initialVerse = initialVerse,
                )
            }

            return ReaderLaunchParams(
                data = data,
                readerMode = intent.getStringExtra(KEY_READER_MODE)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { ReaderMode.fromValue(it) },
                slugs = intent.getStringArrayExtra(KEY_TRANSL_SLUGS)?.toSet(),
            )
        }
    }
}
