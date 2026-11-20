package com.quranapp.android.utils.reader.factory

import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams

object ReaderFactory {
    fun empty(): ReaderLaunchParams =
        chapter(chapterNo = 1)

    fun chapter(
        chapterNo: Int,
        initialVerse: ChapterVersePair? = null,
        slugs: Set<String>? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams = ReaderLaunchParams(
        data = ReaderIntentData.FullChapter(chapterNo, initialVerse),
        readerMode = readerMode,
        slugs = slugs,
    )

    fun verse(
        chapterNo: Int,
        verseNo: Int,
        slugs: Set<String>? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams = chapter(
        chapterNo = chapterNo,
        initialVerse = ChapterVersePair(chapterNo, verseNo),
        slugs = slugs,
        readerMode = readerMode,
    )

    fun verseRange(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int? = null,
        slugs: Set<String>? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams = chapter(
        chapterNo = chapterNo,
        initialVerse = ChapterVersePair(chapterNo, fromVerse),
        slugs = slugs,
        readerMode = readerMode,
    )

    fun juz(
        juzNo: Int,
        initialVerse: ChapterVersePair? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams = ReaderLaunchParams(
        data = ReaderIntentData.FullJuz(juzNo, initialVerse),
        readerMode = readerMode,
    )

    fun hizb(
        hizbNo: Int,
        initialVerse: ChapterVersePair? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams = ReaderLaunchParams(
        data = ReaderIntentData.FullHizb(hizbNo, initialVerse),
        readerMode = readerMode,
    )

    fun mushafPage(
        mushafCode: String,
        mushafVariant: QuranScriptVariant?,
        pageNo: Int,
        initialVerse: ChapterVersePair? = null,
        readerMode: ReaderMode = ReaderMode.Reading,
    ): ReaderLaunchParams = ReaderLaunchParams(
        data = ReaderIntentData.MushafPage(
            mushafCode = mushafCode,
            mushafVariant = mushafVariant,
            pageNo = pageNo,
            initialVerse = initialVerse,
        ),
        readerMode = readerMode,
    )

    suspend fun mushafPage(
        pageNo: Int,
        initialVerse: ChapterVersePair? = null,
        readerMode: ReaderMode = ReaderMode.Reading,
    ): ReaderLaunchParams = mushafPage(
        mushafCode = ReaderPreferences.getQuranScript(),
        mushafVariant = ReaderPreferences.getQuranScriptVariant(),
        pageNo = pageNo,
        initialVerse = initialVerse,
        readerMode = readerMode,
    )

    fun forLastVerses(
        readType: ReadType,
        verse: ChapterVersePair,
        divisionNo: Int? = null,
        readerMode: ReaderMode? = null,
    ): ReaderLaunchParams? {
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

        return ReaderLaunchParams(data = data, readerMode = readerMode)
    }

    fun fromHistory(entity: ReadHistoryEntity): ReaderLaunchParams? {
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
                    initialVerse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
                ),
                readerMode = readerMode,
                mainHistoryId = mainHistoryId,
            )
        }

        return forLastVerses(
            readType = readType,
            verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
            divisionNo = entity.divisionNo,
            readerMode = readerMode,
        )?.copy(mainHistoryId = mainHistoryId)
    }
}
