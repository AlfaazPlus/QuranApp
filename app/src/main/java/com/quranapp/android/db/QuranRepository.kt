package com.quranapp.android.db

import android.content.Context
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.db.entities.quran.NavigationType
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.relations.NavigationUnit
import com.quranapp.android.db.relations.NavigationUnitRange
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.getQuranMushafId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Locale

class QuranRepository(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val database: QuranDatabase
) {
    private val mushafDao get() = database.mushafDao()
    private val ayahDao get() = database.ayahDao()
    private val ayahWordDao get() = database.ayahWordDao()
    private val surahDao get() = database.surahDao()
    private val surahSearchDao get() = database.surahSearchDao()
    private val navigationDao get() = database.navigationDao()

    suspend fun getNumberOfPages(mushafId: Int): Int {
        if (mushafId <= 0) return 0
        return mushafDao.getMushaf(mushafId)?.noOfPages ?: 0
    }

    suspend fun getPageLines(mushafId: Int, pageNo: Int): List<MushafMapEntity> {
        if (mushafId <= 0 || pageNo <= 0) return emptyList()
        return mushafDao.getPageLines(mushafId, pageNo)
    }

    /** Juz for this mushaf page from ayah metadata; `-1` if no ayah line maps (same sentinel as [com.quranapp.android.components.quran.QuranMeta.getJuzForPage]). */
    suspend fun getJuzForMushafPage(mushafId: Int, pageNo: Int): Int {
        if (mushafId <= 0 || pageNo <= 0) return -1
        return mushafDao.getJuzForPage(mushafId, pageNo) ?: -1
    }

    suspend fun getSurah(
        chapterNo: Int,
    ): SurahEntity? {
        return surahDao.getSurah(chapterNo)
    }

    suspend fun getSurahWithLocalizations(
        chapterNo: Int,
    ): SurahWithLocalizations? {
        return surahDao.getSurahWithLocalization(chapterNo)
    }

    suspend fun getAyah(
        chapterNo: Int,
        verseNo: Int,
    ): AyahEntity? {
        return ayahDao.getAyah(chapterNo, verseNo)
    }

    suspend fun getWordsForAyah(
        chapterNo: Int,
        verseNo: Int,
        scriptCode: String
    ): List<AyahWordEntity> {
        val words = ayahWordDao.getWordsForAyah(chapterNo, verseNo, scriptCode)
            .sortedBy { it.wordIndex }

        val lastWordIndex = words.lastOrNull()?.wordIndex

        return words.map {
            it.apply {
                isLastWordOfAyah = it.wordIndex == lastWordIndex
            }
        }
    }

    suspend fun getWordsForAyahById(ayahId: Int, scriptCode: String): List<AyahWordEntity> {
        val words = ayahWordDao.getWordsForAyahById(ayahId, scriptCode)
            .sortedBy { it.wordIndex }

        val lastWordIndex = words.lastOrNull()?.wordIndex

        return words.map {
            it.apply {
                isLastWordOfAyah = it.wordIndex == lastWordIndex
            }
        }
    }

    /**
     * Resolves [MushafMapEntity] ayah line to ordered word texts for [scriptCode]
     * (must match [com.quranapp.android.db.entities.quran.ScriptEntity.code]).
     */
    suspend fun resolveMushafLineWords(
        row: MushafMapEntity,
        scriptCode: String
    ): List<AyahWordEntity> {
        if (row.lineType != MushafLineType.ayah) return emptyList()

        val startAyah = row.startAyahId ?: return emptyList()
        val endAyah = row.endAyahId ?: return emptyList()
        val startWi = row.startWordIndex ?: return emptyList()
        val endWi = row.endWordIndex ?: return emptyList()
        if (startAyah > endAyah) return emptyList()

        if (startAyah == endAyah) {
            val lastWordIndex = ayahWordDao.getLastWordIndexForAyah(startAyah, scriptCode)
            val words = ayahWordDao.getWordsForAyahByIndexRange(
                startAyah,
                scriptCode,
                startWi,
                endWi,
            )

            return words.map {
                it.apply {
                    isLastWordOfAyah = wordIndex == lastWordIndex
                }
            }
        }

        val out = ArrayList<AyahWordEntity>(32)

        getWordsForAyahById(startAyah, scriptCode)
            .asSequence()
            .filter { it.wordIndex >= startWi }
            .sortedBy { it.wordIndex }
            .mapTo(out) { it }

        if (endAyah - startAyah > 1) {
            val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)

            for (ayah in middle) {
                getWordsForAyahById(ayah.ayahId, scriptCode).mapTo(out) { it }
            }
        }

        getWordsForAyahById(endAyah, scriptCode)
            .asSequence()
            .filter { it.wordIndex <= endWi }
            .sortedBy { it.wordIndex }
            .mapTo(out) { it }

        return out
    }

    suspend fun getChapterVerseRangesInJuz(juzNo: Int): List<Pair<Int, IntRange>> {
        if (juzNo <= 0) return emptyList()

        val ayahs = ayahDao.getAyahsByJuz(juzNo)

        if (ayahs.isEmpty()) return emptyList()

        return ayahs.groupBy { it.surahNo }
            .entries
            .sortedBy { it.key }
            .map { (surahNo, list) ->
                val minAyah = list.minOf { it.ayahNo }
                val maxAyah = list.maxOf { it.ayahNo }
                surahNo to (minAyah..maxAyah)
            }
    }

    suspend fun getOrderedSurahNosOnMushafPage(mushafId: Int, pageNo: Int): List<Int> {
        if (mushafId <= 0 || pageNo <= 0) return emptyList()
        val rows = getPageLines(mushafId, pageNo)
        val ordered = LinkedHashSet<Int>()
        for (row in rows) {
            val surahNo = row.surahNo?.takeIf { it > 0 }
                ?: row.startAyahId?.let { ayahDao.getAyahById(it)?.surahNo }
            if (surahNo != null && surahNo > 0) {
                ordered.add(surahNo)
            }
        }
        return ordered.toList()
    }

    suspend fun getChapterNamesOnMushafPage(
        mushafId: Int,
        pageNo: Int,
    ): String {
        val surahs = getOrderedSurahNosOnMushafPage(mushafId, pageNo)

        if (surahs.isEmpty()) return ""

        val langCode = Locale.getDefault().language

        return buildString {
            for (surahNo in surahs) {
                if (isNotEmpty()) append(", ")

                val name =
                    surahDao.getLocalization(surahNo, langCode)?.name?.takeIf { it.isNotBlank() }
                        ?: surahDao.getLocalization(surahNo, "en")?.name.orEmpty()

                append(name)
            }
        }
    }

    fun getAllSurahs(): Flow<List<SurahWithLocalizations>> {
        return surahDao.getAllSurahsWithLocalizations()
    }

    fun getJuzs() = getRangesResolved(NavigationType.juz)

    fun getHizbs() = getRangesResolved(NavigationType.hizb)

    fun getRubs() = getRangesResolved(NavigationType.rub)
    fun getManzil() = getRangesResolved(NavigationType.manzil)

    private fun getRangesResolved(type: NavigationType): Flow<List<NavigationUnit>> {
        return combine(
            navigationDao.getRanges(type),
            surahDao.getAllSurahsWithLocalizations()
        ) { ranges, surahs ->
            val surahsByNo = surahs.associateBy { it.surah.surahNo }
            ranges.groupBy { it.type to it.unitNo }
                .map { (key, ranges) ->
                    val (type, unitNo) = key

                    NavigationUnit(
                        type = type,
                        unitNo = unitNo,
                        ranges = ranges
                            .sortedBy { it.surahNo }
                            .mapNotNull { range ->
                                surahsByNo[range.surahNo]?.let { surah ->
                                    NavigationUnitRange(
                                        surah = surah,
                                        startAyah = range.startAyah,
                                        endAyah = range.endAyah
                                    )
                                }
                            }
                    )
                }
                .sortedWith(compareBy<NavigationUnit> { it.type.name }.thenBy { it.unitNo })
        }
    }

    suspend fun searchSurahNos(query: String): List<Int> {
        return surahSearchDao.searchSurahNos(
            query
                .trim()
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
        ).map { it.surahNo }
    }

    suspend fun searchSurahs(query: String): List<SurahWithLocalizations> {
        val surahNos = searchSurahNos(query)
        if (surahNos.isEmpty()) return emptyList()

        val byNo = surahDao.getSurahsWithLocalizationsByNos(surahNos)
            .associateBy { it.surah.surahNo }

        return surahNos.mapNotNull { byNo[it] }
    }

    suspend fun getChapterName(chapterNo: Int): String {
        if (chapterNo <= 0) return ""

        val langCode = Locale.getDefault().language

        return surahDao.getLocalization(chapterNo, langCode)?.name?.takeIf { it.isNotBlank() }
            ?: surahDao.getLocalization(chapterNo, "en")?.name.orEmpty()
    }

    suspend fun getFirstPageOfChapter(chapterNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || chapterNo <= 0) return null

        return mushafDao.getFirstPageOfChapter(mushafId, chapterNo)
    }

    suspend fun getPageForVerse(surahNo: Int, ayahNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || surahNo <= 0 || ayahNo <= 0) return null
        return mushafDao.getPageForVerse(mushafId, ayahId = QuranUtils.getAyahId(surahNo, ayahNo))
    }

    suspend fun getFirstPageOfJuz(juzNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || juzNo <= 0) return null
        return mushafDao.getFirstPageOfJuz(mushafId, juzNo)
    }

    suspend fun getFirstPageOfHizb(hizbNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || hizbNo <= 0) return null
        return mushafDao.getFirstPageOfHizb(mushafId, hizbNo)
    }

    suspend fun getChapterVerseRangesInHizb(hizbNo: Int): List<Pair<Int, IntRange>> {
        if (hizbNo <= 0) return emptyList()

        val ayahs = ayahDao.getAyahsByHizb(hizbNo)
        if (ayahs.isEmpty()) return emptyList()

        return ayahs.groupBy { it.surahNo }
            .entries
            .sortedBy { it.key }
            .map { (surahNo, list) ->
                val minAyah = list.minOf { it.ayahNo }
                val maxAyah = list.maxOf { it.ayahNo }
                surahNo to (minAyah..maxAyah)
            }
    }

    suspend fun getFirstAyahIdOnPage(pageNo: Int): Int? {
        val mushafId = ReaderPreferences.getQuranScript()
            .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || pageNo <= 0) return null
        return mushafDao.getFirstAyahIdOnPage(mushafId, pageNo)
    }

    suspend fun getChapterVerseCount(chapterNo: Int): Int {
        if (!QuranMeta.isChapterValid(chapterNo)) return 0

        return surahDao.getSurah(chapterNo)?.ayahCount ?: 0
    }

    suspend fun isVerseValid4Chapter(chapterNo: Int, verseNo: Int): Boolean {
        return getSurah(chapterNo)?.isVerseValid(verseNo) == true
    }
}
