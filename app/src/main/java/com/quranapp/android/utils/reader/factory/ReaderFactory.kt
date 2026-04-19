package com.quranapp.android.utils.reader.factory

import android.content.Context
import android.content.Intent
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.ActivityTafsir
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.db.entities.ReadHistoryEntity
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.univ.Keys

object ReaderFactory {
    fun startEmptyReader(context: Context) {
        context.startActivity(Intent().setClass(context, ActivityReader::class.java))
    }

    fun startJuz(context: Context, juzNo: Int) {
        context.startActivity(
            prepareJuzIntent(juzNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    fun startHizb(context: Context, hizbNo: Int) {
        context.startActivity(
            prepareHizbIntent(hizbNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    fun startChapter(context: Context, chapterNo: Int) {
        context.startActivity(
            prepareChapterIntent(chapterNo).setClass(context, ActivityReader::class.java)
        )
    }

    @JvmStatic
    fun startChapter(
        context: Context,
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ) {
        val params = ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(chapterNo),
            slugs = translSlugs.toSet(),
        )
        context.startActivity(
            params.toIntent().setClass(context, ActivityReader::class.java)
        )
    }

    @JvmStatic
    fun startVerse(context: Context, chapterNo: Int, verseNo: Int) {
        context.startActivity(
            prepareSingleVerseIntent(chapterNo, verseNo)
                .setClass(context, ActivityReader::class.java)
        )
    }

    fun startVerseRange(context: Context, chapterNo: Int, fromVerse: Int, toVerse: Int) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
                .setClass(context, ActivityReader::class.java)
        )
    }

    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, range: Pair<Int, Int>) {
        startVerseRange(context, chapterNo, range.first, range.second)
    }

    @JvmStatic
    fun prepareJuzIntent(juzNo: Int): Intent {
        return ReaderLaunchParams(ReaderIntentData.FullJuz(juzNo)).toIntent()
    }

    @JvmStatic
    fun prepareHizbIntent(hizbNo: Int): Intent {
        return ReaderLaunchParams(ReaderIntentData.FullHizb(hizbNo)).toIntent()
    }

    @JvmStatic
    fun prepareChapterIntent(chapterNo: Int): Intent {
        return ReaderLaunchParams(ReaderIntentData.FullChapter(chapterNo)).toIntent()
    }

    @JvmStatic
    fun prepareSingleVerseIntent(chapterNo: Int, verseNo: Int): Intent {
        return ReaderLaunchParams(
            ReaderIntentData.FullChapter(chapterNo, ChapterVersePair(chapterNo, verseNo))
        ).toIntent()
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, fromVerse: Int, toVerse: Int): Intent {
        return ReaderLaunchParams(
            ReaderIntentData.FullChapter(chapterNo, ChapterVersePair(chapterNo, fromVerse))
        ).toIntent()
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, range: Pair<Int, Int>): Intent {
        return prepareVerseRangeIntent(chapterNo, range.first, range.second)
    }

    fun startReferenceVerse(
        context: Context,
        title: String,
        desc: String?,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ) {
        val intent = prepareReferenceVerseIntent(
            title, desc, translSlug, chapters, verses
        )
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }

    fun startReferenceVerse(context: Context, referenceVerseModel: ReferenceVerseModel) {
        val intent = prepareReferenceVerseIntent(referenceVerseModel)
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }

    fun prepareReferenceVerseIntent(
        title: String,
        desc: String?,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ): Intent {
        val referenceVerseModel = ReferenceVerseModel(
            title, desc, translSlug, chapters, verses
        )
        return prepareReferenceVerseIntent(referenceVerseModel)
    }

    fun prepareReferenceVerseIntent(referenceVerseModel: ReferenceVerseModel): Intent {
        return Intent().apply {
            putExtras(referenceVerseModel.toBundle())
        }
    }

    fun prepareLastVersesIntent(
        readType: ReadType,
        readerMode: ReaderMode?,
        verse: ChapterVersePair,
        divisionNo: Int?,
    ): Intent? {
        val data = when (readType) {
            ReadType.Chapter -> {
                if (!QuranMeta.isChapterValid(verse.chapterNo)) return null
                ReaderIntentData.FullChapter(verse.chapterNo, verse)
            }

            ReadType.Juz -> {
                if (!QuranMeta.isJuzValid(divisionNo)) return null
                ReaderIntentData.FullJuz(divisionNo!!, verse)
            }

            ReadType.Hizb -> {
                if (!QuranMeta.isHizbValid(divisionNo)) return null
                ReaderIntentData.FullHizb(divisionNo!!, verse)
            }
        }

        return ReaderLaunchParams(data = data, readerMode = readerMode).toIntent()
    }

    fun prepareHistoryIntent(entity: ReadHistoryEntity): Intent? {
        val readType = ReadType.fromValue(entity.readType)
        val readerMode = ReaderMode.fromValue(entity.readerMode)
        val pageNo = entity.pageNo

        if ((readerMode == ReaderMode.Reading || readerMode == ReaderMode.Translation) &&
            pageNo != null && pageNo > 0 && entity.mushafCode != null
        ) {
            return ReaderLaunchParams(
                data = ReaderIntentData.MushafPage(
                    mushafCode = entity.mushafCode,
                    mushafVariant = QuranScriptVariant.fromValue(entity.mushafVariant),
                    pageNo = pageNo,
                    fallbackChapterNo = entity.chapterNo,
                    fallbackVerseNo = entity.fromVerseNo,
                ),
                readerMode = readerMode,
            ).toIntent()
        }

        return prepareLastVersesIntent(
            readType = readType,
            readerMode = readerMode,
            verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
            divisionNo = entity.divisionNo,
        )
    }

    fun startTafsir(context: Context, chapterNo: Int, verseNo: Int) {
        val intent = prepareTafsirIntent(chapterNo, verseNo)
        intent.setClass(context, ActivityTafsir::class.java)
        context.startActivity(intent)
    }

    fun prepareTafsirIntent(chapterNo: Int, verseNo: Int): Intent {
        return Intent().apply {
            putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
            putExtra(Keys.READER_KEY_VERSE_NO, verseNo)
        }
    }
}
