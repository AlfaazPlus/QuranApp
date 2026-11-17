package com.quranapp.android.utils.reader.factory

import android.content.Context
import android.content.Intent
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams

object ReaderFactory {
    fun startEmptyReader(context: Context) {
        context.startActivity(Intent().setClassName(context, AppDestinations.READER))
    }

    fun startJuz(context: Context, juzNo: Int) {
        context.startActivity(
            prepareJuzIntent(juzNo).setClassName(context, AppDestinations.READER)
        )
    }

    fun startHizb(context: Context, hizbNo: Int) {
        context.startActivity(
            prepareHizbIntent(hizbNo).setClassName(context, AppDestinations.READER)
        )
    }

    suspend fun startMushafPage(context: Context, pageNo: Int) {
        val mushafCode = ReaderPreferences.getQuranScript()
        val variant = ReaderPreferences.getQuranScriptVariant()

        context.startActivity(
            ReaderLaunchParams(
                data = ReaderIntentData.MushafPage(
                    mushafCode = mushafCode,
                    mushafVariant = variant,
                    pageNo = pageNo,
                ),
                readerMode = ReaderMode.Reading,
            ).toIntent().setClassName(context, AppDestinations.READER)
        )
    }

    fun startChapter(context: Context, chapterNo: Int) {
        context.startActivity(
            prepareChapterIntent(chapterNo).setClassName(context, AppDestinations.READER)
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
            params.toIntent().setClassName(context, AppDestinations.READER)
        )
    }

    @JvmStatic
    fun startVerse(context: Context, chapterNo: Int, verseNo: Int) {
        context.startActivity(
            prepareSingleVerseIntent(chapterNo, verseNo)
                .setClassName(context, AppDestinations.READER)
        )
    }

    fun startVerseRange(context: Context, chapterNo: Int, fromVerse: Int, toVerse: Int) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
                .setClassName(context, AppDestinations.READER)
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

    fun startReferenceVerse(context: Context, referenceVerseModel: ReferenceVerseModel) {
        val intent = prepareReferenceVerseIntent(referenceVerseModel)
        intent.setClassName(context, AppDestinations.REFERENCE)
        context.startActivity(intent)
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
        val mainHistoryId = if (entity.isMain && entity.id > 0L) entity.id else null

        if ((readerMode == ReaderMode.Reading || readerMode == ReaderMode.Translation) &&
            pageNo != null && pageNo > 0 && entity.mushafCode != null
        ) {
            return ReaderLaunchParams(
                data = ReaderIntentData.MushafPage(
                    mushafCode = entity.mushafCode,
                    mushafVariant = QuranScriptVariant.fromValue(entity.mushafVariant),
                    pageNo = pageNo,
                    initialVerse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo)
                ),
                readerMode = readerMode,
                mainHistoryId = mainHistoryId,
            ).toIntent()
        }

        return prepareLastVersesIntent(
            readType = readType,
            readerMode = readerMode,
            verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
            divisionNo = entity.divisionNo,
        )?.also { intent ->
            if (mainHistoryId != null) {
                val baseParams = ReaderLaunchParams.fromIntent(intent)
                return baseParams.copy(mainHistoryId = mainHistoryId).toIntent()
            }
        }
    }

    fun startTafsir(context: Context, chapterNo: Int, verseNo: Int) {
        val intent = prepareTafsirIntent(chapterNo, verseNo)
        intent.setClassName(context, AppDestinations.TAFSIR)
        context.startActivity(intent)
    }

    fun prepareTafsirIntent(chapterNo: Int, verseNo: Int): Intent {
        return Intent().apply {
            putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
            putExtra(Keys.READER_KEY_VERSE_NO, verseNo)
        }
    }
}
