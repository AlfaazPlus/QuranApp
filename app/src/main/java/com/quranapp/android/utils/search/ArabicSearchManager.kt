package com.quranapp.android.utils.search

import android.content.Context
import com.quranapp.android.api.JsonHelper
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

object ArabicSearchManager {

    // ─── Data classes ──────────────────────────────────────────────────────────

    data class NormalizedVerse(
        val suraId: Int,
        val verseId: Int,
        val originalText: String,
        val normalizedText: String,
        val normalizedWords: Set<String>
    )

    data class ArabicSearchResult(
        val chapterNo: Int,
        val verseNo: Int,
        var score: Int
    )

    // ─── Cache ─────────────────────────────────────────────────────────────────

    private val cache = AtomicReference<List<NormalizedVerse>?>(null)

    // ─── Pre-compiled Regex Patterns for Normalization ────────────────────────

    private val REGEX_INVISIBLE = "[\u200C\u200D]".toRegex()
    private val REGEX_DIACRITICS = "[\u064B-\u065F\u0670\u0610-\u061A\u06D6-\u06E4\u06E7-\u06ED]".toRegex()
    private val REGEX_ALEF_FAMILY = "[\u0622\u0623\u0625\u0671\u0672\u0673\u0675]".toRegex()
    private val REGEX_LAM_ALEF = "[\uFEF5-\uFEFC]".toRegex()
    private val REGEX_YEH_FAMILY = "[\u0649\u0626\u06CC]".toRegex()
    private val REGEX_NON_ALPHANUMERIC = "[^\\p{L}\\s\\d]".toRegex()
    private val REGEX_WHITESPACE = "\\s+".toRegex()

    // ─── Arabic detection ──────────────────────────────────────────────────────

    /** Returns true if [text] contains at least one Arabic-script character. */
    fun isArabic(text: String): Boolean =
        text.any { c ->
            c in '\u0600'..'\u06FF' ||
                    c in '\u0750'..'\u077F' ||
                    c in '\u08A0'..'\u08FF'
        }

    // ─── Characters to ignore during search ───────────────────────────────────

    private fun isIgnoredInSearch(c: Char): Boolean =
        c in '\u064B'..'\u065F' ||
                c == '\u0670'           ||
                c in '\u0610'..'\u061A' ||
                c in '\u06D6'..'\u06ED' ||
                c == '\u0640'           ||
                c in '\uFE70'..'\uFEFF'

    // ─── Corpus loading ────────────────────────────────────────────────────────

    private fun loadAndPrepareQuran(context: Context): List<NormalizedVerse> {
        cache.get()?.let { return it }

        val built = buildCorpus(context)
        cache.compareAndSet(null, built)
        return cache.get()!!
    }

    private fun buildCorpus(context: Context): List<NormalizedVerse> = try {
        val content = context.assets
            .open("scripts/quran_no_tashkeel.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JsonHelper.json.parseToJsonElement(content).jsonArray
        val list = mutableListOf<NormalizedVerse>()

        for (suraElement in jsonArray) {
            val sura = suraElement.jsonObject
            val suraId  = sura["id"]?.jsonPrimitive?.int ?: continue
            val verses  = sura["verses"]?.jsonArray     ?: continue

            for (verseElement in verses) {
                val verse   = verseElement.jsonObject
                val verseId = verse["id"]?.jsonPrimitive?.int     ?: continue
                val text    = verse["text"]?.jsonPrimitive?.content ?: ""

                val norm = cleanAndNormalize(text)
                list.add(
                    NormalizedVerse(
                        suraId      = suraId,
                        verseId     = verseId,
                        originalText   = text,
                        normalizedText = norm,
                        normalizedWords = norm.split(" ").filter { it.isNotEmpty() }.toSet()
                    )
                )
            }
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // ─── Public search ─────────────────────────────────────────────────────────

    /**
     * Searches the Quran corpus for [query].
     *
     * Both the corpus and the query are fully normalised before comparison.
     */
    fun search(
        context: Context,
        query: String,
        searchWordPart: Boolean
    ): List<ArabicSearchResult> {
        val quran = loadAndPrepareQuran(context)

        val normalizedQuery = cleanAndNormalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val queryWordsList = normalizedQuery.split(" ").filter { it.isNotEmpty() }
        val queryWords = queryWordsList.filter { it.length >= 2 }
        if (queryWords.isEmpty()) return emptyList()

        // ── Pre-calculate search patterns for performance ────────────────────
        val fullPhrase = if (queryWordsList.size >= 2) queryWordsList.joinToString(" ") else null
        
        val subPhrases = mutableListOf<Pair<String, Int>>()
        if (queryWordsList.size >= 2) {
            for (len in queryWordsList.size - 1 downTo 2) {
                val scoreContrib = (len * len) * 500
                for (i in 0..queryWordsList.size - len) {
                    val sub = queryWordsList.subList(i, i + len).joinToString(" ")
                    subPhrases.add(sub to scoreContrib)
                }
            }
        }

        val results = mutableListOf<ArabicSearchResult>()

        for (verse in quran) {
            val text  = verse.normalizedText
            var score = 0

            // 1. Full-phrase match
            if (fullPhrase != null && text.contains(fullPhrase)) {
                score += queryWordsList.size * 2000
            }

            // 2. Sub-phrase matching
            for (sp in subPhrases) {
                if (text.contains(sp.first)) {
                    score += sp.second
                }
            }

            // 3. Individual word matching
            var matchedCount = 0
            val verseWords = verse.normalizedWords

            for (word in queryWords) {
                // Optimization: use set lookup instead of regex for whole-word boundary matching.
                // Since normalizedText only contains words separated by single spaces,
                // a word in the set is guaranteed to be a whole word in the text.
                if (verseWords.contains(word)) {
                    matchedCount++
                    score += word.length * 20
                } else if (searchWordPart && text.contains(word)) {
                    matchedCount++
                    score += word.length * 10
                }
            }

            // ── Relevance gate ───────────────────────────────────────────────
            val isRelevant = score >= 500 ||
                    (queryWords.size > 2 && matchedCount >= (queryWords.size + 1) / 2) ||
                    (queryWords.size <= 2 && matchedCount > 0)

            if (isRelevant) {
                results.add(ArabicSearchResult(verse.suraId, verse.verseId, score))
            }
        }

        return results.sortedByDescending { it.score }
    }

    // ─── Normalization ─────────────────────────────────────────────────────────
    fun cleanAndNormalize(text: String): String {
        var t = text

        // 1. Invisible Characters
        t = t.replace(REGEX_INVISIBLE, "")

        // 2. Arabic Presentation Forms
        t = normalizePresentationForms(t)

        // 3. Remove tatweel (kashida)
        t = t.replace("\u0640", "")

        // 4. Correctly Handle Small Waw and Small Yeh BEFORE stripping marks
        t = t.replace("\u06E5", "\u0648") // Small Waw -> Waw
        t = t.replace("\u06E6", "\u064A") // Small Yeh -> Yeh

        // 5. Safe Diacritic Removal
        t = t.replace(REGEX_DIACRITICS, "")

        // 6. Alef family -> bare alef
        t = t.replace(REGEX_ALEF_FAMILY, "\u0627")

        // 7. Lam-Alef ligatures -> لا
        t = t.replace(REGEX_LAM_ALEF, "\u0644\u0627")

        // 8. Hamza -> alef
        t = t.replace("\u0621", "\u0627")

        // 9. Waw with hamza -> waw
        t = t.replace("\u0624", "\u0648")

        // 10. Teh marbuta -> heh
        t = t.replace("\u0629", "\u0647")

        // 11. Yeh family unification
        t = t.replace(REGEX_YEH_FAMILY, "\u064A")

        // 12. Kaf family unification
        t = t.replace("\u06A9", "\u0643")

        // 13. Dotless beh variants -> beh
        t = t.replace("\u067E", "\u0628")

        // 14. Dal/Dhal unification
        t = t.replace("\u0630", "\u062F")

        // 15. Clean non-letters and whitespace
        t = t.replace(REGEX_NON_ALPHANUMERIC, "")
        return t.trim().replace(REGEX_WHITESPACE, " ").lowercase()
    }

    private fun normalizePresentationForms(text: String): String {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
    }

    // ─── Highlight range calculation ───────────────────────────────────────────

    fun findHighlightRanges(
        originalText: String,
        query: String,
        searchWordPart: Boolean
    ): List<Pair<Int, Int>> {
        val normalizedQuery = cleanAndNormalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val indexMap           = mutableListOf<Int>()
        val cleanedTextBuilder = StringBuilder()

        for (i in originalText.indices) {
            val c = originalText[i]
            if (isIgnoredInSearch(c)) continue

            val n = normalizeSingleChar(c)
            when {
                n.isLetter() || n.isDigit() -> {
                    cleanedTextBuilder.append(n.lowercaseChar())
                    indexMap.add(i)
                }
                n.isWhitespace() -> {
                    if (cleanedTextBuilder.isNotEmpty() && cleanedTextBuilder.last() != ' ') {
                        cleanedTextBuilder.append(' ')
                        indexMap.add(i)
                    }
                }
            }
        }

        val cleanedText = cleanedTextBuilder.toString()
        val rawRanges   = mutableListOf<Pair<Int, Int>>()

        fun addRangeFromCleaned(start: Int, end: Int) {
            if (start < 0 || end > indexMap.size || start >= end) return

            val sIdx = start.coerceIn(0, indexMap.size - 1)
            val eIdx = (end - 1).coerceIn(0, indexMap.size - 1)

            var s = indexMap[sIdx]
            var e = indexMap[eIdx] + 1

            while (s > 0 && !originalText[s - 1].isWhitespace()) s--
            while (e < originalText.length && !originalText[e].isWhitespace()) e++

            rawRanges.add(s to e)
        }

        fun isWordBoundaryPattern(word: String) =
            "(?<![\\p{L}\\d])${Pattern.quote(word)}(?![\\p{L}\\d])"

        val queryWords = normalizedQuery.split(" ").filter { it.isNotEmpty() }
        if (queryWords.isEmpty()) return emptyList()

        val matchedCleanedRanges = mutableSetOf<Pair<Int, Int>>()

        fun findAndAdd(pattern: String) {
            runCatching {
                val m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(cleanedText)
                while (m.find()) {
                    val r = m.start() to m.end()
                    if (matchedCleanedRanges.add(r)) {
                        addRangeFromCleaned(m.start(), m.end())
                    }
                }
            }
        }

        if (queryWords.size == 1) {
            val word    = queryWords[0]
            val pattern = if (searchWordPart) Pattern.quote(word)
            else isWordBoundaryPattern(word)
            findAndAdd(pattern)
        } else {
            val fullPhrase = queryWords.joinToString("\\s+") { Pattern.quote(it) }
            findAndAdd(fullPhrase)

            for (len in queryWords.size - 1 downTo 2) {
                for (i in 0..queryWords.size - len) {
                    val sub = queryWords.subList(i, i + len)
                        .joinToString("\\s+") { Pattern.quote(it) }
                    findAndAdd(sub)
                }
            }

            for (word in queryWords) {
                val pattern = if (searchWordPart) Pattern.quote(word)
                else isWordBoundaryPattern(word)
                findAndAdd(pattern)
            }
        }

        return mergeRanges(rawRanges)
    }

    private fun normalizeSingleChar(c: Char): Char = when (c) {
        '\u0622', '\u0623', '\u0625',
        '\u0671', '\u0672', '\u0673', '\u0675' -> '\u0627'
        '\u0621' -> '\u0627'
        '\u0624' -> '\u0648'
        '\u0629' -> '\u0647'
        '\u0649', '\u0626' -> '\u064A'
        '\u067E' -> '\u0628'
        '\u0630' -> '\u062F'
        else -> c
    }

    private fun mergeRanges(ranges: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        if (ranges.isEmpty()) return emptyList()
        val sorted  = ranges.sortedBy { it.first }
        val merged  = mutableListOf<Pair<Int, Int>>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            current = if (next.first <= current.second) {
                current.first to maxOf(current.second, next.second)
            } else {
                merged.add(current); next
            }
        }
        merged.add(current)
        return merged
    }
}

