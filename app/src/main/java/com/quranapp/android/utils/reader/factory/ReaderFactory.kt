package com.quranapp.android.utils.reader.factory

import android.content.Context
import android.content.Intent
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.activities.ActivityTafsir
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER
import com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.Keys.KEY_REFERENCE_VERSE_MODEL

object ReaderFactory {
    @JvmStatic
    fun startEmptyReader(context: Context) {
        context.startActivity(Intent().setClass(context, ActivityReader::class.java))
    }

    @JvmStatic
    fun startJuz(context: Context, juzNo: Int) {
        context.startActivity(prepareJuzIntent(juzNo).setClass(context, ActivityReader::class.java))
    }

    @JvmStatic
    fun startChapter(context: Context, chapterNo: Int) {
        context.startActivity(
            prepareChapterIntent(chapterNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startChapter(
        context: Context,
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ) {
        context.startActivity(
            prepareChapterIntent(translSlugs, saveTranslChanges, chapterNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startVerse(context: Context, chapterNo: Int, verseNo: Int) {
        context.startActivity(
            prepareSingleVerseIntent(chapterNo, verseNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, fromVerse: Int, toVerse: Int) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, fromVerse, toVerse).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, range: Pair<Int, Int>) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, range).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun prepareJuzIntent(juzNo: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 5)
        intent.putExtra(Keys.READER_KEY_JUZ_NO, juzNo)
        return intent
    }

    @JvmStatic
    fun prepareChapterIntent(chapterNo: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 3)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        return intent
    }

    @JvmStatic
    fun prepareChapterIntent(
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 3)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_TRANSL_SLUGS, translSlugs)
        intent.putExtra(Keys.READER_KEY_SAVE_TRANSL_CHANGES, saveTranslChanges)
        return intent
    }

    @JvmStatic
    fun prepareSingleVerseIntent(chapterNo: Int, verseNo: Int): Intent {
        return prepareVerseRangeIntent(chapterNo, verseNo, verseNo)
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, fromVerse: Int, toVerse: Int): Intent {
        return prepareVerseRangeIntent(chapterNo, Pair(fromVerse, toVerse))
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, range: Pair<Int, Int>): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 4)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSES, range)
        return intent
    }

    /**
     * This function creates intent for reader verse range using intArray instead of Pair
     * which will be used in [ShortcutUtils][com.quranapp.android.utils.others.ShortcutUtils], because shortcut uses
     * persistable bundle which doesn't support Pair.
     */
    fun prepareVerseRangeIntentForShortcut(chapterNo: Int, fromVerse: Int, toVerse: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 4)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSES, intArrayOf(fromVerse, toVerse))
        return intent
    }

    @JvmStatic
    fun startReferenceVerse(
        context: Context,
        showChapterSugg: Boolean,
        title: String,
        desc: String,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ) {
        val intent = prepareReferenceVerseIntent(
            showChapterSugg,
            title,
            desc,
            translSlug,
            chapters,
            verses
        )
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }

    @JvmStatic
    fun startReferenceVerse(context: Context, referenceVerseModel: ReferenceVerseModel) {
        val intent = prepareReferenceVerseIntent(referenceVerseModel)
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }

    @JvmStatic
    fun prepareReferenceVerseIntent(
        showChapterSugg: Boolean,
        title: String,
        desc: String,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ): Intent {
        val referenceVerseModel = ReferenceVerseModel(
            showChapterSugg,
            title,
            desc,
            translSlug,
            chapters,
            verses
        )
        return prepareReferenceVerseIntent(referenceVerseModel)
    }

    @JvmStatic
    fun prepareReferenceVerseIntent(referenceVerseModel: ReferenceVerseModel): Intent {
        val intent = Intent()
        intent.putExtra(KEY_REFERENCE_VERSE_MODEL, referenceVerseModel)
        return intent
    }

    @JvmStatic
    fun prepareLastVersesIntent(
        quranMeta: QuranMeta,
        juzNo: Int,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        readType: Int,
        readerStyle: Int
    ): Intent? {
        var intent: Intent? = null
        if (readType == READER_READ_TYPE_CHAPTER && QuranMeta.isChapterValid(chapterNo)) {
            intent = prepareChapterIntent(chapterNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, ChapterVersePair(chapterNo, fromVerse))
        } else if (readType == READER_READ_TYPE_JUZ && QuranMeta.isJuzValid(juzNo)) {
            intent = prepareJuzIntent(juzNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, ChapterVersePair(chapterNo, fromVerse))
        } else if (quranMeta.isVerseRangeValid4Chapter(chapterNo, fromVerse, toVerse)) {
            intent = prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
        }

        if (intent != null) {
            if (readerStyle != -1) {
                intent.putExtra(Keys.READER_KEY_READER_STYLE, readerStyle)
            }
        }

        return intent
    }

    /**
     * This function creates intent for reader verse range using intArray instead of Pair
     * which will be used in [ShortcutUtils][com.quranapp.android.utils.others.ShortcutUtils], because shortcut uses
     * persistable bundle which doesn't support Pair.
     */
    fun prepareLastVersesIntentForShortcut(
        quranMeta: QuranMeta,
        juzNo: Int,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        readType: Int,
        readerStyle: Int
    ): Intent? {
        var intent: Intent? = null
        if (readType == READER_READ_TYPE_CHAPTER && QuranMeta.isChapterValid(chapterNo)) {
            intent = prepareChapterIntent(chapterNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, ChapterVersePair(chapterNo, fromVerse))
        } else if (readType == READER_READ_TYPE_JUZ && QuranMeta.isJuzValid(juzNo)) {
            intent = prepareJuzIntent(juzNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, ChapterVersePair(chapterNo, fromVerse))
        } else if (quranMeta.isVerseRangeValid4Chapter(chapterNo, fromVerse, toVerse)) {
            intent = prepareVerseRangeIntentForShortcut(chapterNo, fromVerse, toVerse)
        }

        if (intent != null && readerStyle != -1) {
            intent.putExtra(Keys.READER_KEY_READER_STYLE, readerStyle)
        }

        return intent
    }

    @JvmStatic
    fun prepareLastVersesIntent(quranMeta: QuranMeta, lastVersesModel: ReadHistoryModel): Intent? {
        return prepareLastVersesIntent(
            quranMeta,
            lastVersesModel.juzNo,
            lastVersesModel.chapterNo,
            lastVersesModel.fromVerseNo,
            lastVersesModel.toVerseNo,
            lastVersesModel.readType,
            lastVersesModel.readerStyle
        )
    }

    @JvmStatic
    fun startTafsir(context: Context, chapterNo: Int, verseNo: Int) {
        val intent = prepareTafsirIntent(chapterNo, verseNo)
        intent.setClass(context, ActivityTafsir::class.java)
        context.startActivity(intent)
    }

    @JvmStatic
    fun prepareTafsirIntent(chapterNo: Int, verseNo: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSE_NO, verseNo)
        return intent
    }
}
