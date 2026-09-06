package com.quranapp.android.search

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.text.HtmlCompat
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.StringUtils
import java.text.Normalizer

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

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, SearchResult> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            if (sourceQuran) {
                val normalized = SearchNormalizer.normalize(
                    query,
                )

                val fts = FtsQueryBuilder.toPrefixAndQuery(normalized)
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
                                    preview = highlightMatches(text, query)
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

            val fts = FtsQueryBuilder.toTranslationTextQuery(query)
                ?: return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )

            QuranTranslationFactory(application).use { factory ->
                val dao = DatabaseProvider
                    .getSearchIndexDatabase(application)
                    .searchIndexDao()

                val slugFilter = filters.selectedSlugs?.takeIf { it.isNotEmpty() }

                val versePage = dao.pageMatchedVersesFiltered(
                    ftsQuery = fts,
                    slugs = slugFilter,
                    surahNo = null,
                    limit = limit,
                    offset = offset,
                )

                if (versePage.isEmpty()) {
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                        nextKey = null
                    )
                }

                val verseKeys = versePage.map {
                    it.surahNo to it.ayahNo
                }

                val rows = dao.rowsForPagedVersesFiltered(
                    ftsQuery = fts,
                    keys = verseKeys.map { "${it.first}:${it.second}" },
                    slugs = slugFilter,
                )

                val slugs = rows
                    .map { it.slug }
                    .toSet()

                val bulkTranslations =
                    factory.getTranslationsBulkForSearch(
                        slugs = slugs,
                        verseKeys = verseKeys
                    )

                val books =
                    factory.getAvailableTranslationBooksInfo()

                val grouped = rows
                    .groupBy { it.surahNo to it.ayahNo }
                    .map { (coord, matches) ->

                        SearchResult(
                            chapterNo = coord.first,
                            verseNo = coord.second,
                            matches = matches
                                .sortedBy { it.slug }
                                .map { row ->

                                    val text =
                                        bulkTranslations[row.slug]
                                            ?.get(coord)
                                            ?.text
                                            ?: row.text

                                    SearchResultMatch.TranslationMatch(
                                        slug = row.slug,
                                        displayName =
                                            books[row.slug]?.displayName
                                                ?: row.slug,
                                        preview = highlightMatches(
                                            text.toSearchPreviewText(),
                                            query,
                                        )
                                    )
                                }
                        )
                    }

                return LoadResult.Page(
                    data = grouped,
                    prevKey = if (offset == 0) null
                    else maxOf(0, offset - limit),

                    nextKey =
                        if (versePage.size < limit) null
                        else offset + versePage.size
                )
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(
        state: PagingState<Int, SearchResult>
    ): Int? {

        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor)
            ?: return null

        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)
    }

    private fun highlightMatches(text: String, rawQuery: String): AnnotatedString {
        val contextWindow = 180
        val sidePadding = 48
        val ellipsis = "…"

        val tokens = rawQuery
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { it.foldForSearchHighlight() }

        if (tokens.isEmpty()) {
            if (text.length <= contextWindow) return buildAnnotatedString { append(text) }

            return buildAnnotatedString {
                append(text.take(contextWindow).trimEnd())
                append(ellipsis)
            }
        }

        val source = text
        val foldedSource = source.withHighlightIndexMap()
        val spans = mutableListOf<IntRange>()

        for (token in tokens.sortedByDescending { it.length }) {
            val q = token.foldForSearchHighlight()
            if (q.isBlank()) continue
            var idx = 0

            while (idx < foldedSource.text.length) {
                val at = foldedSource.text.indexOf(q, idx)

                if (at < 0) break

                val start = foldedSource.sourceIndices[at]
                val end = foldedSource.sourceIndices[at + q.length - 1] + 1

                spans += start until source.expandHighlightEnd(end)

                idx = at + q.length
            }
        }

        if (spans.isEmpty()) {
            if (text.length <= contextWindow) return buildAnnotatedString { append(text) }
            return buildAnnotatedString {
                append(text.take(contextWindow).trimEnd())
                append(ellipsis)
            }
        }

        val merged = spans
            .sortedBy { it.first }
            .fold(mutableListOf<IntRange>()) { acc, range ->
                val last = acc.lastOrNull()
                if (last == null || range.first > last.last + 1) {
                    acc.add(range)
                } else {
                    acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
                }
                acc
            }

        val firstHit = merged.first()
        val sliceStart = maxOf(0, firstHit.first - sidePadding)
        val sliceEndExclusive = minOf(source.length, sliceStart + contextWindow)

        val prefix = if (sliceStart > 0) ellipsis else ""
        val suffix = if (sliceEndExclusive < source.length) ellipsis else ""

        val rawSlice = source.substring(sliceStart, sliceEndExclusive)
        val leadingTrimCount = rawSlice.length - rawSlice.trimStart().length
        val visibleText = rawSlice.trimStart().trimEnd()
        val contentStartInSource = sliceStart + leadingTrimCount

        val highlightStyle = SpanStyle(background = Color(0x66FFD858))

        return buildAnnotatedString {
            append(prefix)
            append(visibleText)
            append(suffix)

            val textOffset = prefix.length

            for (range in merged) {
                val clippedStart = maxOf(range.first, sliceStart)
                val clippedEndExclusive = minOf(range.last + 1, sliceEndExclusive)
                if (clippedStart >= clippedEndExclusive) continue

                val startInVisible = clippedStart - contentStartInSource
                val endInVisible = clippedEndExclusive - contentStartInSource
                val styleStart = textOffset + maxOf(0, startInVisible)
                val styleEnd = textOffset + minOf(visibleText.length, endInVisible)
                if (styleStart >= styleEnd) continue

                addStyle(
                    style = highlightStyle,
                    start = styleStart,
                    end = styleEnd,
                )
            }
        }
    }

    private fun String.toSearchPreviewText(): String {
        val stripped = StringUtils.removeHTML(this, false)
        if (stripped.isEmpty()) return ""

        return HtmlCompat.fromHtml(
            stripped,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString()
    }

    private data class HighlightIndexMap(
        val text: String,
        val sourceIndices: List<Int>,
    )

    private fun String.withHighlightIndexMap(): HighlightIndexMap {
        val folded = StringBuilder()
        val sourceIndices = mutableListOf<Int>()

        forEachIndexed { index, char ->
            val normalized = char.toString().foldForSearchHighlight()
            normalized.forEach { foldedChar ->
                folded.append(foldedChar)
                sourceIndices += index
            }
        }

        return HighlightIndexMap(
            text = folded.toString(),
            sourceIndices = sourceIndices,
        )
    }

    private fun String.foldForSearchHighlight(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
    }

    private fun String.expandHighlightEnd(endExclusive: Int): Int {
        var expanded = endExclusive

        while (expanded < length) {
            val char = this[expanded]
            val type = Character.getType(char)

            if (type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
            ) {
                break
            }

            expanded++
        }

        return expanded
    }
}
