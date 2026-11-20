package com.quranapp.android.search

import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.miscellenous.RegexPattern
import com.quranapp.android.utils.quran.QuranMeta

sealed class QuickLinkItem {
    data class Chapter(val surah: SurahWithLocalizations) : QuickLinkItem()
    data class Verse(val chapterNo: Int, val verseNo: Int) : QuickLinkItem()
    data class Juz(val juzNo: Int) : QuickLinkItem()
    data class Hizb(val hizbNo: Int) : QuickLinkItem()
    data class Tafsir(val chapterNo: Int, val verseNo: Int) : QuickLinkItem()
}

object SearchQuickLinksParser {

    private suspend fun chapterQuickLink(
        repository: QuranRepository,
        chapterNo: Int,
    ): QuickLinkItem.Chapter? =
        repository.getSurahWithLocalizations(chapterNo)?.let { QuickLinkItem.Chapter(it) }

    /**
     * Parses quick navigation patterns from the search query (verse refs, chapter/juz numbers,
     * and surah name/alias substring matches.
     */
    suspend fun parse(repository: QuranRepository, rawQuery: String): List<QuickLinkItem> {
        val q = rawQuery.trim()
        if (q.isEmpty()) return emptyList()

        val out = ArrayList<QuickLinkItem>()
        val mtchrVRangeJump = RegexPattern.VERSE_RANGE_JUMP_PATTERN.matchEntire(q)
        val mtchrVJump = RegexPattern.VERSE_JUMP_PATTERN.matchEntire(q)

        val mtchrChapOrJuzNo = RegexPattern.CHAPTER_OR_JUZ_PATTERN.matchEntire(q)

        if (mtchrVRangeJump != null) {
            val chapNo = mtchrVRangeJump.groupValues[1].toInt()

            if (!QuranMeta.isChapterValid(chapNo)) {
                return out
            }

            var fromVerse = mtchrVRangeJump.groupValues[2].toInt()
            var toVerse = mtchrVRangeJump.groupValues[3].toInt()

            if (fromVerse > toVerse) {
                val tmp = fromVerse
                fromVerse = toVerse
                toVerse = tmp
            }

            val isFromValid =
                repository.isVerseValid4Chapter(chapNo, fromVerse)

            val isToValid =
                repository.isVerseValid4Chapter(chapNo, toVerse)

            if (isFromValid) {
                out += QuickLinkItem.Verse(chapNo, fromVerse)
                out += QuickLinkItem.Tafsir(chapNo, fromVerse)
            }

            if (isToValid && toVerse != fromVerse) {
                out += QuickLinkItem.Verse(chapNo, toVerse)
                out += QuickLinkItem.Tafsir(chapNo, toVerse)
            }

            chapterQuickLink(repository, chapNo)?.let {
                out += it
            }

        } else if (mtchrVJump != null) {
            val chapNo = mtchrVJump.groupValues[1].toInt()
            val verseNo = mtchrVJump.groupValues[2].toInt()

            if (!QuranMeta.isChapterValid(chapNo)) {
                return out
            }

            if (repository.isVerseValid4Chapter(chapNo, verseNo)) {
                out += QuickLinkItem.Verse(chapNo, verseNo)
                out += QuickLinkItem.Tafsir(chapNo, verseNo)
            }

            chapterQuickLink(repository, chapNo)?.let {
                out += it
            }

        } else if (mtchrChapOrJuzNo != null) {
            val chapOrJuz = mtchrChapOrJuzNo.groupValues[1].toInt()

            if (QuranMeta.isChapterValid(chapOrJuz)) {
                chapterQuickLink(repository, chapOrJuz)?.let {
                    out += it
                }
            }

            if (QuranMeta.isJuzValid(chapOrJuz)) {
                out += QuickLinkItem.Juz(chapOrJuz)
            }

            if (QuranMeta.isHizbValid(chapOrJuz)) {
                out += QuickLinkItem.Hizb(chapOrJuz)
            }
        }

        repository.searchSurahs(rawQuery).take(2).forEach {
            out += QuickLinkItem.Chapter(it)
        }

        return out.distinctBy { it.stableKey() }
    }

}

fun QuickLinkItem.stableKey(): String =
    when (this) {
        is QuickLinkItem.Chapter -> "c${surah.surah.surahNo}"
        is QuickLinkItem.Juz -> "j$juzNo"
        is QuickLinkItem.Hizb -> "h$hizbNo"
        is QuickLinkItem.Tafsir -> "t$chapterNo:$verseNo"
        is QuickLinkItem.Verse -> "v$chapterNo:$verseNo"
    }
