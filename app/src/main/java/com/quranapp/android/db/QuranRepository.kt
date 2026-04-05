package com.quranapp.android.db

import android.content.Context
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.MushafMapEntity
import java.util.Locale

class QuranRepository(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val database: QuranDatabase
) {
    private val mushafDao get() = database.mushafDao()
    private val ayahDao get() = database.ayahDao()
    private val ayahWordDao get() = database.ayahWordDao()
    private val surahDao get() = database.surahDao()

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

    suspend fun getWordsForAyah(ayahId: Int, scriptCode: String): List<AyahWordEntity> {
        val words = ayahWordDao.getWordsForAyah(ayahId, scriptCode)
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

        getWordsForAyah(startAyah, scriptCode)
            .asSequence()
            .filter { it.wordIndex >= startWi }
            .sortedBy { it.wordIndex }
            .mapTo(out) { it }

        if (endAyah - startAyah > 1) {
            val middle = ayahDao.getAyahsStrictlyBetween(startAyah, endAyah)

            for (ayah in middle) {
                getWordsForAyah(ayah.ayahId, scriptCode).mapTo(out) { it }
            }
        }

        getWordsForAyah(endAyah, scriptCode)
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
}
