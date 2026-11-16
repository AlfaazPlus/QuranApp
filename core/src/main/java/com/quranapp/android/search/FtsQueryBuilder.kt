package com.quranapp.android.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import java.text.Normalizer

/** Converts user input into safe FTS5 MATCH expressions and matching highlight terms. */
object FtsQueryBuilder {
    const val MAX_QUERY_LENGTH = 256
    private val tokenRegex = Regex(
        """("[^"]+"|\bNEAR(?:[/:]\d+)?\b|[\p{L}\p{N}\p{M}_]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val searchableWordRegex = Regex("[\\p{L}\\p{N}\\p{M}_]+")

    fun toFtsMatchQuery(
        rawQuery: String,
        strategy: SearchMatchingStrategy,
    ): String? {
        if (strategy == SearchMatchingStrategy.EXACT_PHRASE) {
            return rawQuery.normalizeSearchPhrase()
                .takeIf { it.isNotBlank() }
                ?.let { phrase -> "\"${phrase.replace("\"", "\"\"")}\"" }
        }

        val tokens = parse(rawQuery)
        if (tokens.isEmpty()) return null

        val result = mutableListOf<String>()
        val currentNearGroup = mutableListOf<String>()
        var currentNearDistance: Int? = null

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]

            if (token is QueryToken.Near) {
                val hasPrev = currentNearGroup.isNotEmpty() || result.isNotEmpty()
                val nextToken = tokens.getOrNull(index + 1)
                val hasNext = nextToken is QueryToken.Term

                if (hasPrev && hasNext) {
                    if (currentNearGroup.isEmpty() && result.isNotEmpty()) {
                        currentNearGroup.add(result.removeAt(result.lastIndex))
                    }
                    if (token.distance != null) {
                        currentNearDistance = if (currentNearDistance == null) {
                            token.distance
                        } else {
                            minOf(currentNearDistance, token.distance)
                        }
                    }
                    currentNearGroup.add((tokens[index + 1] as QueryToken.Term).text)
                    index += 2
                } else {
                    if (currentNearGroup.isNotEmpty()) {
                        result.add(formatNearGroup(currentNearGroup, currentNearDistance))
                        currentNearGroup.clear()
                        currentNearDistance = null
                    }
                    result.add("near*")
                    index++
                }
            } else if (token is QueryToken.Term) {
                if (currentNearGroup.isNotEmpty()) {
                    result.add(formatNearGroup(currentNearGroup, currentNearDistance))
                    currentNearGroup.clear()
                    currentNearDistance = null
                }
                result.add(token.text)
                index++
            }
        }

        if (currentNearGroup.isNotEmpty()) {
            result.add(formatNearGroup(currentNearGroup, currentNearDistance))
        }

        val operator = if (strategy == SearchMatchingStrategy.ANY_WORD) " OR " else " "
        return result
            .takeIf { it.isNotEmpty() }
            ?.joinToString(operator)
    }

    internal fun foldedHighlightTerms(
        query: String,
        strategy: SearchMatchingStrategy
    ): List<String> {
        return highlightTerms(query.take(MAX_QUERY_LENGTH), strategy)
            .map { it.foldForSearchHighlight() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it.length }
    }

    private fun highlightTerms(query: String, strategy: SearchMatchingStrategy): List<String> {
        if (query.isBlank()) return emptyList()

        if (strategy == SearchMatchingStrategy.EXACT_PHRASE) {
            return listOf(query.normalizeSearchPhrase()).filter { it.isNotBlank() }
        }

        val terms = mutableListOf<String>()
        var hasPreviousTerm = false
        val tokens = parse(query)

        tokens.forEachIndexed { index, token ->
            when (token) {
                is QueryToken.Term -> {
                    terms += token.highlightText
                    hasPreviousTerm = true
                }

                is QueryToken.Near -> {
                    val isOperator =
                        hasPreviousTerm && tokens.getOrNull(index + 1) is QueryToken.Term
                    if (!isOperator) {
                        // FTS treats NEAR as a regular prefix term unless it appears between terms.
                        terms += "near"
                        hasPreviousTerm = true
                    }
                }
            }
        }

        return terms.distinct()
    }

    private fun parse(rawQuery: String): List<QueryToken> = tokenRegex
        .findAll(rawQuery)
        .mapNotNull { match ->
            val text = match.value
            when {
                text.startsWith('"') && text.endsWith('"') && text.length > 2 -> {
                    val normalized = text
                        .substring(1, text.length - 1)
                        .normalizeSearchPhrase()
                    if (normalized.isNotBlank()) {
                        QueryToken.Term(
                            text = "\"${normalized.replace("\"", "\"\"")}\"",
                            highlightText = normalized,
                        )
                    } else {
                        null
                    }
                }

                text.equals("NEAR", ignoreCase = true) ||
                        text.startsWith("NEAR/", ignoreCase = true) ||
                        text.startsWith("NEAR:", ignoreCase = true) -> {
                    QueryToken.Near(
                        text.substring(4)
                            .removePrefix("/")
                            .removePrefix(":")
                            .toIntOrNull(),
                    )
                }

                else -> {
                    val normalized = text.foldArabicLetters().lowercase()
                    if (normalized.isNotBlank()) {
                        QueryToken.Term(text = "$normalized*", highlightText = normalized)
                    } else {
                        null
                    }
                }
            }
        }
        .toList()

    private fun formatNearGroup(terms: List<String>, distance: Int?): String {
        val joined = terms.joinToString(" ")
        return if (distance != null) "NEAR($joined, $distance)" else "NEAR($joined)"
    }

    private fun String.foldArabicLetters(): String = SearchNormalizer.foldArabicLetters(this)

    private fun String.normalizeSearchPhrase(): String {
        return searchableWordRegex
            .findAll(foldArabicLetters())
            .joinToString(" ") { it.value.lowercase() }
    }

    private sealed interface QueryToken {
        data class Term(
            val text: String,
            val highlightText: String,
        ) : QueryToken

        data class Near(val distance: Int?) : QueryToken
    }

}

internal fun String.toSearchSnippet(
    foldedTerms: List<String>,
): AnnotatedString {
    val highlightStyle = SpanStyle(background = Color(0x66FFD858))
    val contextWindow = 180
    val sidePadding = 48
    val ellipsis = "…"

    if (isBlank()) return buildAnnotatedString { }

    val source = this
    if (foldedTerms.isEmpty()) {
        if (source.length <= contextWindow) return buildAnnotatedString { append(source) }
        return buildAnnotatedString {
            append(source.take(contextWindow).trimEnd())
            append(ellipsis)
        }
    }

    val foldedSource = source.withHighlightIndexMap()
    val spans = mutableListOf<IntRange>()

    for (term in foldedTerms) {
        var idx = 0
        while (idx < foldedSource.text.length) {
            val at = foldedSource.text.indexOf(term, idx)
            if (at < 0) break

            val start = foldedSource.sourceIndices[at]
            val end = foldedSource.sourceIndices[at + term.length - 1] + 1
            spans += start until source.expandHighlightEnd(end)
            idx = at + term.length
        }
    }

    if (spans.isEmpty()) {
        if (source.length <= contextWindow) return buildAnnotatedString { append(source) }
        return buildAnnotatedString {
            append(source.take(contextWindow).trimEnd())
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

            addStyle(highlightStyle, styleStart, styleEnd)
        }
    }
}

private data class HighlightIndexMap(
    val text: String,
    val sourceIndices: IntArray,
)

private fun String.withHighlightIndexMap(): HighlightIndexMap {
    val folded = StringBuilder(length)
    val indices = IntArray((length * 2).coerceAtLeast(1))
    var size = 0

    forEachIndexed { index, char ->
        char.forEachFolded { foldedChar ->
            folded.append(foldedChar)
            indices[size++] = index
        }
    }

    return HighlightIndexMap(
        text = folded.toString(),
        sourceIndices = if (size == indices.size) indices else indices.copyOf(size),
    )
}

private fun String.foldForSearchHighlight(): String {
    if (isEmpty()) return ""
    val folded = StringBuilder(length)
    for (char in this) {
        char.forEachFolded { folded.append(it) }
    }
    return folded.toString()
}

private inline fun Char.forEachFolded(emit: (Char) -> Unit) {
    val code = code
    if (code < 0x80) {
        emit(lowercaseChar())
        return
    }
    if (code == 0x0640 || isCombiningMark()) return

    val mapped = when (this) {
        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
        'ى' -> 'ي'
        else -> this
    }
    val mappedCode = mapped.code
    if (mappedCode in 0x0600..0x06FF || mappedCode in 0x0750..0x077F || mappedCode in 0x08A0..0x08FF) {
        emit(mapped)
        return
    }

    val parts = decompositionCache.getOrPut(mapped) {
        val nfd = Normalizer.normalize(mapped.toString(), Normalizer.Form.NFD)
        val chars = ArrayList<Char>(2)
        for (c in nfd) {
            if (!c.isCombiningMark()) chars += c.lowercaseChar()
        }
        chars.toCharArray()
    }
    for (part in parts) emit(part)
}

private fun Char.isCombiningMark(): Boolean {
    return when (Character.getType(this)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
            -> true

        else -> false
    }
}

private val decompositionCache = java.util.concurrent.ConcurrentHashMap<Char, CharArray>()

private fun String.expandHighlightEnd(endExclusive: Int): Int {
    var expanded = endExclusive

    while (expanded < length) {
        val type = Character.getType(this[expanded])
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
