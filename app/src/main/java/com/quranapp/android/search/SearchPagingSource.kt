package com.quranapp.android.search

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory

data class SearchResult(
    val chapterNo: Int,
    val verseNo: Int,
    val matches: List<SearchResultMatch>,
)

sealed class SearchResultMatch {
    data class TranslationMatch(
        val slug: String,
        val displayName: String,
        val preview: AnnotatedString,
    ) : SearchResultMatch()

    data class QuranTextMatch(
        val preview: AnnotatedString,
    ) : SearchResultMatch()
}

class SearchPagingSource(
    private val application: Application,
    private val query: String,
    private val sourceQuran: Boolean,
    private val filters: SearchFilters = SearchFilters(),
) : PagingSource<Int, SearchResult>() {
    override fun getRefreshKey(
        state: PagingState<Int, SearchResult>
    ): Int? {

        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor)
            ?: return null

        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, SearchResult> {
        return try {
        val offset = params.key ?: 0
        val limit = params.loadSize
        val foldedTerms = FtsQueryBuilder.foldedHighlightTerms(query, filters.matchingStrategy)

        if (sourceQuran) {
                val fts = FtsQueryBuilder.toFtsMatchQuery(
                    rawQuery = query,
                    strategy = filters.matchingStrategy,
                )
                    ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )

                val quranRepo = DatabaseProvider.getQuranRepository(application)
                val arabicRows = quranRepo.arabicTextSearch(fts, limit, offset)

                if (arabicRows.isEmpty()) {
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                        nextKey = null
                    )
                }

                val rawSize = arabicRows.size
                val data = arabicRows
                    .map { row ->
                        val (surahNo, ayahNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                        Triple(surahNo, ayahNo, row.text)
                    }
                    .map { (surahNo, ayahNo, text) ->
                        SearchResult(
                            chapterNo = surahNo,
                            verseNo = ayahNo,
                            matches = listOf(
                                SearchResultMatch.QuranTextMatch(
                                    preview = text.toSearchSnippet(foldedTerms)
                                )
                            )
                        )
                    }

                return LoadResult.Page(
                    data = data,
                    prevKey = if (offset == 0) null
                    else maxOf(0, offset - limit),
                    nextKey =
                        if (rawSize < limit) null
                        else offset + rawSize
                )
            }

            val fts = FtsQueryBuilder.toFtsMatchQuery(
                rawQuery = query,
                strategy = filters.matchingStrategy,
            )
                ?: return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )

            QuranTranslationFactory(application).use { factory ->
                val dao = DatabaseProvider
                    .getSearchIndexDatabase(application)
                    .searchIndexDao()

                val rows = dao.pageMatchedRows(
                    ftsQuery = fts,
                    slugs = filters.selectedSlugs?.takeIf { it.isNotEmpty() },
                    limit = limit,
                    offset = offset,
                )

                if (rows.isEmpty()) {
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                        nextKey = null
                    )
                }

                val books = factory.getAvailableTranslationBooksInfo()
                val grouped = rows.groupBy { it.surahNo to it.ayahNo }

                return LoadResult.Page(
                    data = grouped.map { (coord, matches) ->
                        SearchResult(
                            chapterNo = coord.first,
                            verseNo = coord.second,
                            matches = matches.map { row ->
                                SearchResultMatch.TranslationMatch(
                                    slug = row.slug,
                                    displayName = books[row.slug]?.displayName ?: row.slug,
                                    preview = row.text.toSearchSnippet(foldedTerms),
                                )
                            }
                        )
                    },
                    prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                    nextKey = if (grouped.size < limit) null else offset + grouped.size,
                )
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
