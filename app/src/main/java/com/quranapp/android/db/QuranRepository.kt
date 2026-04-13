package com.quranapp.android.db

import android.content.Context
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
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
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.toQuranMushafId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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

    suspend fun getVerseWithDetails(
        chapterNo: Int,
        verseNo: Int,
        scriptCode: String? = null
    ): VerseWithDetails? {
        val script = scriptCode ?: ReaderPreferences.getQuranScript()
        val batch = loadChapterVerseBatch(chapterNo, verseNo, verseNo, script) ?: return null
        val ayah = batch.ayahByVerseNo[verseNo] ?: return null
        val words = batch.wordsByVerseNo[verseNo] ?: emptyList()
        return VerseWithDetails(
            words = words,
            pageNo = batch.pageByVerseNo[verseNo] ?: 0,
            verse = ayah,
            chapter = batch.surah
        )
    }

    /**
     * Batched ayahs, words, and mushaf page numbers for [fromVerse]..[toVerse] (inclusive).
     */
    suspend fun loadChapterVerseBatch(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        scriptCode: String,
    ): ChapterVerseBatch? {
        val lo = minOf(fromVerse, toVerse)
        val hi = maxOf(fromVerse, toVerse)
        val ayahs = ayahDao.getAyahsInRange(chapterNo, lo, hi)
        if (ayahs.isEmpty()) return null
        val surah = surahDao.getSurahWithLocalization(chapterNo) ?: return null

        val mushafId = scriptCode.toQuranMushafId(ReaderPreferences.getQuranScriptVariant())
        val ayahByVerse = ayahs.associateBy { it.ayahNo }
        val ids = ayahs.map { it.ayahId }
        val wordsFlat = if (ids.isNotEmpty()) {
            ayahWordDao.getWordsForAyahs(ids, scriptCode)
        } else {
            emptyList()
        }
        val wordsByAyahId = groupWordsByAyahIdWithLastFlags(wordsFlat)

        val firstPage = if (ayahs.any { it.ayahNo == 1 }) {
            if (mushafId > 0) mushafDao.getFirstPageOfChapter(mushafId, chapterNo) else null
        } else {
            null
        }

        val idsForPage = ayahs.filter { it.ayahNo > 1 }.map { it.ayahId }
        val pageByAyahId = if (mushafId > 0 && idsForPage.isNotEmpty()) {
            mushafDao.getPagesForAyahIds(mushafId, idsForPage)
                .associate { it.ayahId to it.pageNumber }
        } else {
            emptyMap()
        }

        val pageByVerse = HashMap<Int, Int>(ayahs.size)
        for (a in ayahs) {
            val p = when {
                a.ayahNo == 1 -> firstPage
                mushafId > 0 -> pageByAyahId[a.ayahId]
                else -> null
            }
            pageByVerse[a.ayahNo] = p ?: -1
        }

        val wordsByVerse = HashMap<Int, List<AyahWordEntity>>(ayahs.size)
        for (a in ayahs) {
            wordsByVerse[a.ayahNo] = wordsByAyahId[a.ayahId] ?: emptyList()
        }

        return ChapterVerseBatch(
            surah = surah,
            ayahByVerseNo = ayahByVerse,
            wordsByVerseNo = wordsByVerse,
            pageByVerseNo = pageByVerse,
        )
    }

    /**
     * Batched load for an arbitrary set of verse numbers in one chapter (e.g. quick reference).
     */
    suspend fun loadQuickReferenceBatch(
        chapterNo: Int,
        verseNos: List<Int>,
        scriptCode: String,
    ): ChapterVerseBatch? {
        val distinct = verseNos.distinct()
        if (distinct.isEmpty()) return null
        val ayahIds = distinct.map { QuranUtils.getAyahId(chapterNo, it) }
        val ayahs = ayahDao.getAyahsByIds(ayahIds)
        if (ayahs.isEmpty()) return null
        val surah = surahDao.getSurahWithLocalization(chapterNo) ?: return null

        val mushafId = scriptCode.toQuranMushafId(ReaderPreferences.getQuranScriptVariant())
        val ayahByVerse = ayahs.associateBy { it.ayahNo }
        val ids = ayahs.map { it.ayahId }
        val wordsFlat = ayahWordDao.getWordsForAyahs(ids, scriptCode)
        val wordsByAyahId = groupWordsByAyahIdWithLastFlags(wordsFlat)

        val firstPage = if (ayahs.any { it.ayahNo == 1 }) {
            if (mushafId > 0) mushafDao.getFirstPageOfChapter(mushafId, chapterNo) else null
        } else {
            null
        }

        val idsForPage = ayahs.filter { it.ayahNo > 1 }.map { it.ayahId }
        val pageByAyahId = if (mushafId > 0 && idsForPage.isNotEmpty()) {
            mushafDao.getPagesForAyahIds(mushafId, idsForPage)
                .associate { it.ayahId to it.pageNumber }
        } else {
            emptyMap()
        }

        val pageByVerse = HashMap<Int, Int>(ayahs.size)
        for (a in ayahs) {
            val p = when {
                a.ayahNo == 1 -> firstPage
                mushafId > 0 -> pageByAyahId[a.ayahId]
                else -> null
            }
            pageByVerse[a.ayahNo] = p ?: -1
        }

        val wordsByVerse = HashMap<Int, List<AyahWordEntity>>(ayahs.size)
        for (a in ayahs) {
            wordsByVerse[a.ayahNo] = wordsByAyahId[a.ayahId] ?: emptyList()
        }

        return ChapterVerseBatch(
            surah = surah,
            ayahByVerseNo = ayahByVerse,
            wordsByVerseNo = wordsByVerse,
            pageByVerseNo = pageByVerse,
        )
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
     * When [wordCache] is provided (full ayah word lists), avoids DB reads.
     */
    suspend fun resolveMushafLineWords(
        row: MushafMapEntity,
        scriptCode: String,
        wordCache: Map<Int, List<AyahWordEntity>>? = null,
    ): List<AyahWordEntity> {
        if (row.lineType != MushafLineType.ayah) return emptyList()

        val startAyah = row.startAyahId ?: return emptyList()
        val endAyah = row.endAyahId ?: return emptyList()
        val startWi = row.startWordIndex ?: return emptyList()
        val endWi = row.endWordIndex ?: return emptyList()
        if (startAyah > endAyah) return emptyList()

        if (startAyah == endAyah) {
            val lastWordIndex = if (wordCache != null) {
                (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                    .lastOrNull()?.wordIndex
            } else {
                ayahWordDao.getLastWordIndexForAyah(startAyah, scriptCode)
            }
            val words = if (wordCache != null) {
                (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                    .asSequence()
                    .filter { it.wordIndex in startWi..endWi }
                    .sortedBy { it.wordIndex }
                    .toList()
            } else {
                ayahWordDao.getWordsForAyahByIndexRange(
                    startAyah,
                    scriptCode,
                    startWi,
                    endWi,
                )
            }

            return words.map {
                it.apply {
                    isLastWordOfAyah = lastWordIndex != null && wordIndex == lastWordIndex
                }
            }
        }

        val out = ArrayList<AyahWordEntity>(32)

        if (wordCache != null) {
            (wordCache[startAyah] ?: getWordsForAyahById(startAyah, scriptCode))
                .asSequence()
                .filter { it.wordIndex >= startWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        } else {
            getWordsForAyahById(startAyah, scriptCode)
                .asSequence()
                .filter { it.wordIndex >= startWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        }

        if (endAyah - startAyah > 1) {
            val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)
            if (wordCache != null) {
                for (ayah in middle) {
                    (wordCache[ayah.ayahId] ?: getWordsForAyahById(ayah.ayahId, scriptCode))
                        .mapTo(out) { it }
                }
            } else {
                val middleIds = middle.map { it.ayahId }
                if (middleIds.isNotEmpty()) {
                    val flat = ayahWordDao.getWordsForAyahs(middleIds, scriptCode)
                    val byId = groupWordsByAyahIdWithLastFlags(flat)
                    for (ayah in middle) {
                        byId[ayah.ayahId]?.mapTo(out) { it }
                    }
                }
            }
        }

        if (wordCache != null) {
            (wordCache[endAyah] ?: getWordsForAyahById(endAyah, scriptCode))
                .asSequence()
                .filter { it.wordIndex <= endWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        } else {
            getWordsForAyahById(endAyah, scriptCode)
                .asSequence()
                .filter { it.wordIndex <= endWi }
                .sortedBy { it.wordIndex }
                .mapTo(out) { it }
        }

        return out
    }

    private fun groupWordsByAyahIdWithLastFlags(
        flat: List<AyahWordEntity>,
    ): Map<Int, List<AyahWordEntity>> {
        if (flat.isEmpty()) return emptyMap()
        val grouped = flat.groupBy { it.ayahId }
        val out = HashMap<Int, List<AyahWordEntity>>(grouped.size)
        for ((ayahId, list) in grouped) {
            val sorted = list.sortedBy { it.wordIndex }
            val last = sorted.lastOrNull()?.wordIndex
            out[ayahId] = sorted.map { w ->
                w.apply {
                    isLastWordOfAyah = last != null && w.wordIndex == last
                }
            }
        }
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
        val idsNeedingLookup = rows.mapNotNull { row ->
            if (row.surahNo != null && row.surahNo!! > 0) null
            else row.startAyahId?.takeIf { it > 0 }
        }.distinct()
        val ayahById = if (idsNeedingLookup.isNotEmpty()) {
            ayahDao.getAyahsByIds(idsNeedingLookup).associateBy { it.ayahId }
        } else {
            emptyMap()
        }
        val ordered = LinkedHashSet<Int>()
        for (row in rows) {
            val surahNo = row.surahNo?.takeIf { it > 0 }
                ?: row.startAyahId?.let { ayahById[it]?.surahNo }
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
        val surahNos = getOrderedSurahNosOnMushafPage(mushafId, pageNo)
        if (surahNos.isEmpty()) return ""

        val withLocs = surahDao.getSurahsWithLocalizationsByNos(surahNos)
        val byNo = withLocs.associateBy { it.surah.surahNo }

        return buildString {
            for (surahNo in surahNos) {
                if (isNotEmpty()) append(", ")
                append(byNo[surahNo]?.getCurrentName().orEmpty())
            }
        }
    }

    suspend fun getPageLinesGroupedForPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): Map<Int, List<MushafMapEntity>> {
        if (mushafId <= 0 || pageNumbers.isEmpty()) return emptyMap()
        val rows = mushafDao.getPageLinesForPages(mushafId, pageNumbers)
        return rows.groupBy { it.pageNumber }
    }

    suspend fun getJuzForMushafPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): Map<Int, Int> {
        if (mushafId <= 0 || pageNumbers.isEmpty()) return emptyMap()
        return mushafDao.getJuzForPages(mushafId, pageNumbers)
            .associate { it.pageNumber to it.juzNo }
    }

    /**
     * Preloads full-word lists for all ayahs touched by mushaf ayah lines (for a prefetch batch).
     */
    suspend fun preloadMushafLineWordCache(
        ayahLineRows: List<MushafMapEntity>,
        scriptCode: String,
    ): Map<Int, List<AyahWordEntity>> {
        val ids = LinkedHashSet<Int>()
        for (row in ayahLineRows) {
            if (row.lineType != MushafLineType.ayah) continue
            val startAyah = row.startAyahId ?: continue
            val endAyah = row.endAyahId ?: continue
            if (row.startWordIndex == null || row.endWordIndex == null) continue
            if (startAyah > endAyah) continue
            if (startAyah == endAyah) {
                ids.add(startAyah)
            } else {
                ids.add(startAyah)
                ids.add(endAyah)
                if (endAyah - startAyah > 1) {
                    val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)
                    for (ayah in middle) ids.add(ayah.ayahId)
                }
            }
        }
        if (ids.isEmpty()) return emptyMap()
        val flat = ayahWordDao.getWordsForAyahs(ids.toList(), scriptCode)
        return groupWordsByAyahIdWithLastFlags(flat)
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

        return appFallbackLanguageCodes()
            .firstNotNullOfOrNull { code ->
                surahDao.getLocalization(chapterNo, code)
                    ?.name
                    ?.takeIf { it.isNotBlank() }
            }
            .orEmpty()
    }

    suspend fun getChapterNames(chapterNos: List<Int>): Map<Int, String> {
        if (chapterNos.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, String>()

        for (code in appFallbackLanguageCodes()) {
            val remaining = chapterNos.filter { it !in result.keys }
            if (remaining.isEmpty()) break

            val localizations = surahDao.getLocalizations(remaining, code)

            localizations.forEach { entity ->
                val name = entity.name

                if (!name.isNullOrBlank()) {
                    result[entity.surahNo] = name
                }
            }
        }

        return result
    }

    suspend fun getFirstPageOfChapter(chapterNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || chapterNo <= 0) return null

        return mushafDao.getFirstPageOfChapter(mushafId, chapterNo)
    }

    suspend fun getPageForVerse(surahNo: Int, ayahNo: Int, scriptCode: String? = null): Int? {
        if (ayahNo == 1) {
            return getFirstPageOfChapter(surahNo)
        }

        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || surahNo <= 0 || ayahNo <= 0) return null
        return mushafDao.getPageForVerse(mushafId, ayahId = QuranUtils.getAyahId(surahNo, ayahNo))
    }

    suspend fun getFirstPageOfJuz(juzNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        if (mushafId <= 0 || juzNo <= 0) return null
        return mushafDao.getFirstPageOfJuz(mushafId, juzNo)
    }

    suspend fun getFirstPageOfHizb(hizbNo: Int, scriptCode: String? = null): Int? {
        val mushafId = (scriptCode ?: ReaderPreferences.getQuranScript())
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

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
            .toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        return getFirstAyahIdOnPage(mushafId, pageNo)
    }

    suspend fun getFirstAyahIdOnPage(mushafId: Int, pageNo: Int): Int? {
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
