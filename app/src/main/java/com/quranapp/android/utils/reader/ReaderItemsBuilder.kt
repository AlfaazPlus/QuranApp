package com.quranapp.android.utils.reader

import android.content.Context
import com.quranapp.android.components.quran.Quran
import com.quranapp.android.components.quran.Quran2
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageSectionItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.utils.quran.QuranUtils

object ReaderItemsBuilder {
    suspend fun buildVersesForTranslationMode(
        context: Context,
        chapterNo: Int,
        fromVerse: Int?,
        toVerse: Int?
    ): List<ReaderLayoutItem> {
        if (!QuranMeta.isChapterValid(chapterNo)) return emptyList()

        val quran = Quran2.prepareInstance(context)
        val meta = QuranMeta2.prepareInstance(context)

        val chapter = quran.getChapter(chapterNo)

        var toVerseResolved = if (toVerse == null) chapter.verseCount else toVerse
        val fromVerseResolved = if (fromVerse == null) {
            toVerseResolved = chapter.verseCount
            1
        } else fromVerse

        val out = ArrayList<ReaderLayoutItem>()

        if (QuranUtils.doesVerseRangeEqualWhole(
                meta,
                chapterNo,
                fromVerseResolved,
                toVerseResolved
            )
        ) {
            out.add(ReaderLayoutItem.ChapterInfo.apply {
                key = "chapterInfo-$chapterNo"
            })

            if (chapter.canShowBismillah()) {
                out.add(ReaderLayoutItem.Bismillah.apply {
                    key = "bismillah-$chapterNo"
                })
            }
        }

        for (v in fromVerseResolved..toVerseResolved) {
            val verse = quran.getVerse(chapterNo, v) ?: continue
            out.add(ReaderLayoutItem.VerseUI(verse).apply {
                key = "verse-$chapterNo:${verse.verseNo}"
            })
        }

        return out
    }

    suspend fun buildJuzVersesForTranslationMode(
        context: Context,
        juzNo: Int
    ): List<ReaderLayoutItem> {
        if (!QuranMeta.isJuzValid(juzNo)) return emptyList()

        val quran = Quran2.prepareInstance(context)
        val meta = QuranMeta2.prepareInstance(context)

        val out = ArrayList<ReaderLayoutItem>()

        val chapters = meta.getChaptersInJuz(juzNo)

        for (chapterNo in chapters.first..chapters.second) {
            val verseRange = meta.getVerseRangeOfChapterInJuz(juzNo, chapterNo)

            if (verseRange.first == 1) {
                out.add(ReaderLayoutItem.ChapterTitle(chapterNo).apply {
                    key = "chapterTitle-$chapterNo"
                })

                if (QuranMeta.canShowBismillah(chapterNo)) {
                    out.add(ReaderLayoutItem.Bismillah.apply {
                        key = "bismillah-$chapterNo"
                    })
                }
            }

            for (v in verseRange.first..verseRange.second) {
                val verse = quran.getVerse(chapterNo, v)

                out.add(ReaderLayoutItem.VerseUI(verse).apply {
                    key = "verse-$chapterNo:${verse.verseNo}"
                })
            }
        }

        return out
    }


    suspend fun buildChapterForPageMode(
        context: Context,
        chapterNo: Int,
    ): List<QuranPageItem> {
        if (!QuranMeta.isChapterValid(chapterNo)) return emptyList()

        val quran = Quran2.prepareInstance(context)
        val meta = QuranMeta2.prepareInstance(context)

        val pageRange = meta.getChapterPageRange(chapterNo)
        val pages = ArrayList<QuranPageItem>()

        for (pageNo in pageRange.first..pageRange.second) {
            pages.add(buildPage(pageNo, quran, meta))
        }

        return pages
    }

    suspend fun buildJuzForPageMode(
        context: Context,
        juzNo: Int,
    ): List<QuranPageItem> {
        if (!QuranMeta.isJuzValid(juzNo)) return emptyList()

        val quran = Quran2.prepareInstance(context)
        val meta = QuranMeta2.prepareInstance(context)

        val pageRange = meta.getJuzPageRange(juzNo)

        val pages = ArrayList<QuranPageItem>()

        for (pageNo in pageRange.first..pageRange.second) {
            pages.add(buildPage(pageNo, quran, meta))
        }

        return pages
    }

    private fun buildPage(
        pageNo: Int,
        quran: Quran,
        meta: QuranMeta,
    ): QuranPageItem {
        val chaptersOnPage = meta.getChaptersOnPage(pageNo)

        val sections = ArrayList<QuranPageSectionItem>()

        val nameBuilder = StringBuilder()

        for (chapterNo in chaptersOnPage.first..chaptersOnPage.second) {
            val verseRange = meta.getVerseRangeOfChapterOnPage(pageNo, chapterNo)

            val verses = ArrayList<Verse>()
            val showBismillah = verseRange.first == 1 && QuranMeta.canShowBismillah(chapterNo)

            for (v in verseRange.first..verseRange.second) {
                quran.getVerse(chapterNo, v)?.let { verses.add(it) }
            }

            sections.add(
                QuranPageSectionItem(
                    chapterNo = chapterNo,
                    showBismillah = showBismillah,
                    verses = verses,
                ),
            )

            if (nameBuilder.isNotEmpty()) nameBuilder.append(", ")

            nameBuilder
                .append(chapterNo)
                .append(". ")
                .append(meta.getChapterMeta(chapterNo).getName())
        }

        return QuranPageItem(
            pageNo = pageNo,
            juzNo = meta.getJuzForPage(pageNo),
            sections = sections,
            chapterRange = chaptersOnPage.first..chaptersOnPage.second,
            chaptersName = nameBuilder.toString(),
        )
    }
}