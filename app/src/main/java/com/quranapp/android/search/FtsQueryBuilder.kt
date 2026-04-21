package com.quranapp.android.search

/**
 * Builds FTS4 MATCH strings from an already normalized query.
 * Uses AND of prefix terms: `word1* AND word2*`.
 */
object FtsQueryBuilder {
    fun toPrefixAndQuery(normalizedQuery: String): String? {
        val tokens = normalizedQuery.split(' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null

        return tokens.joinToString(" AND ") { token ->
            val escaped = escapeFtsToken(token)

            "${escaped}*"
        }
    }

    private fun escapeFtsToken(token: String): String {
        var t = token.replace("\"", "\"\"")

        if (t.any { it.isWhitespace() || it == '*' }) {
            t = "\"$t\""
        }

        return t
    }
}
