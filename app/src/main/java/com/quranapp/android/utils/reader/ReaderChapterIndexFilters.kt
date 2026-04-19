package com.quranapp.android.utils.reader

import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.db.relations.SurahWithLocalizations
import kotlinx.serialization.Serializable

@Serializable
enum class ReaderChapterRevelationFilter {
    any,
    meccan,
    medinan,
}

@Serializable
enum class ReaderChapterSajdaFilter {
    any,
    withSajda,
    withoutSajda,
}

@Serializable
enum class ReaderChapterLengthFilter {
    any,
    short,
    medium,
    long,
}

@Serializable
data class ReaderChapterIndexFilters(
    val revelation: ReaderChapterRevelationFilter = ReaderChapterRevelationFilter.any,
    val sajda: ReaderChapterSajdaFilter = ReaderChapterSajdaFilter.any,
    val length: ReaderChapterLengthFilter = ReaderChapterLengthFilter.any,
) {
    fun isDefault(): Boolean = this == Default

    companion object {
        val Default = ReaderChapterIndexFilters()
    }
}

fun List<SurahWithLocalizations>.filteredByChapterIndex(
    filters: ReaderChapterIndexFilters,
    surahNosWithSajdah: Set<Int>,
): List<SurahWithLocalizations> {
    var result = this

    when (filters.revelation) {
        ReaderChapterRevelationFilter.meccan ->
            result = result.filter { it.surah.revelationType == RevelationType.meccan }

        ReaderChapterRevelationFilter.medinan ->
            result = result.filter { it.surah.revelationType == RevelationType.medinan }

        ReaderChapterRevelationFilter.any -> Unit
    }

    when (filters.sajda) {
        ReaderChapterSajdaFilter.withSajda ->
            result = result.filter { it.surah.surahNo in surahNosWithSajdah }

        ReaderChapterSajdaFilter.withoutSajda ->
            result = result.filter { it.surah.surahNo !in surahNosWithSajdah }

        ReaderChapterSajdaFilter.any -> Unit
    }

    when (filters.length) {
        ReaderChapterLengthFilter.short ->
            result = result.filter { it.surah.ayahCount <= 100 }

        ReaderChapterLengthFilter.medium ->
            result = result.filter { it.surah.ayahCount in 101..200 }

        ReaderChapterLengthFilter.long ->
            result = result.filter { it.surah.ayahCount >= 201 }

        ReaderChapterLengthFilter.any -> Unit
    }

    return result
}
