package com.quranapp.android.utils.reader

import android.content.Intent
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.reader.ReaderMode

fun ReaderLaunchParams.toIntent(): Intent = Intent().apply {
    when (val d = this@toIntent.data) {
        is ReaderIntentData.FullChapter -> {
            putExtra(ReaderLaunchParams.KEY_READ_TYPE, ReadType.Chapter.value)
            putExtra(ReaderLaunchParams.KEY_CHAPTER_NO, d.chapterNo)
            d.initialVerse?.putInto(this)
        }

        is ReaderIntentData.FullJuz -> {
            putExtra(ReaderLaunchParams.KEY_READ_TYPE, ReadType.Juz.value)
            putExtra(ReaderLaunchParams.KEY_JUZ_NO, d.juzNo)
            d.initialVerse?.putInto(this)
        }

        is ReaderIntentData.FullHizb -> {
            putExtra(ReaderLaunchParams.KEY_READ_TYPE, ReadType.Hizb.value)
            putExtra(ReaderLaunchParams.KEY_HIZB_NO, d.hizbNo)
            d.initialVerse?.putInto(this)
        }

        is ReaderIntentData.MushafPage -> {
            putExtra(ReaderLaunchParams.KEY_MUSHAF_CODE, d.mushafCode)
            putExtra(ReaderLaunchParams.KEY_MUSHAF_VARIANT, d.mushafVariant?.value)
            putExtra(ReaderLaunchParams.KEY_RESTORE_PAGE, d.pageNo)
            d.initialVerse?.putInto(this)
        }
    }
    readerMode?.let { putExtra(ReaderLaunchParams.KEY_READER_MODE, it.value) }
    slugs?.let { putExtra(ReaderLaunchParams.KEY_TRANSL_SLUGS, it.toTypedArray()) }
    mainHistoryId?.let { putExtra(ReaderLaunchParams.KEY_MAIN_HISTORY_ID, it) }
}

fun ReaderLaunchParams.Companion.fromIntent(intent: Intent): ReaderLaunchParams {
    val mainHistoryId = intent.getLongExtra(KEY_MAIN_HISTORY_ID, -1L).takeIf { it > 0L }
    val mushafCode = intent.getStringExtra(KEY_MUSHAF_CODE)
    val mushafVariant = QuranScriptVariant.fromValue(intent.getStringExtra(KEY_MUSHAF_VARIANT))
    val restorePage = intent.getIntExtra(KEY_RESTORE_PAGE, -1)

    if (mushafCode != null && restorePage > 0) {
        return ReaderLaunchParams(
            data = ReaderIntentData.MushafPage(
                mushafCode = mushafCode,
                mushafVariant = mushafVariant,
                pageNo = restorePage,
                initialVerse = intent.readInitialVerse(),
            ),
            readerMode = intent.readReaderMode(),
            slugs = intent.getStringArrayExtra(KEY_TRANSL_SLUGS)?.toSet(),
            mainHistoryId = mainHistoryId,
        )
    }

    val readType = ReadType.fromValue(intent.getStringExtra(KEY_READ_TYPE))
    val initialVerse = intent.readInitialVerse()

    val data = when (readType) {
        ReadType.Juz -> ReaderIntentData.FullJuz(intent.getIntExtra(KEY_JUZ_NO, 1), initialVerse)
        ReadType.Hizb -> ReaderIntentData.FullHizb(intent.getIntExtra(KEY_HIZB_NO, 1), initialVerse)
        ReadType.Chapter -> ReaderIntentData.FullChapter(
            intent.getIntExtra(KEY_CHAPTER_NO, 1),
            initialVerse
        )
    }

    return ReaderLaunchParams(
        data = data,
        readerMode = intent.readReaderMode(),
        slugs = intent.getStringArrayExtra(KEY_TRANSL_SLUGS)?.toSet(),
        mainHistoryId = mainHistoryId,
    )
}

private fun ChapterVersePair.putInto(intent: Intent) {
    intent.putExtra(ReaderLaunchParams.KEY_INITIAL_VERSE_CHAPTER, chapterNo)
    intent.putExtra(ReaderLaunchParams.KEY_INITIAL_VERSE_NO, verseNo)
}

private fun Intent.readInitialVerse(): ChapterVersePair? {
    val chapter = getIntExtra(ReaderLaunchParams.KEY_INITIAL_VERSE_CHAPTER, -1)
    val verse = getIntExtra(ReaderLaunchParams.KEY_INITIAL_VERSE_NO, -1)
    return if (chapter > 0 && verse > 0) ChapterVersePair(chapter, verse) else null
}

private fun Intent.readReaderMode(): ReaderMode? =
    getStringExtra(ReaderLaunchParams.KEY_READER_MODE)
        ?.takeIf { it.isNotEmpty() }
        ?.let { ReaderMode.fromValue(it) }
