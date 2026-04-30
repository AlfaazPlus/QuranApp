package com.quranapp.android.search

object FtsQueryBuilder {

    fun toTranslationTextQuery(rawQuery: String): String? {
        val base = SearchNormalizer.normalize(rawQuery)

        return toPrefixAndQuery(base, matchColumn = null)
    }

    fun toPrefixAndQuery(
        normalizedQuery: String,
        matchColumn: String? = null,
    ): String? {
        val tokens = normalizedQuery.split(' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it.isNotBlank() }
            .filter { it.length >= 2 }
            .filter { isValidToken(it) }

        if (tokens.isEmpty()) return null

        return tokens.joinToString(" ") { token ->
            val term = "${escapeFtsToken(token)}*"
            if (matchColumn == null) term else "$matchColumn:$term"
        }
    }

    private fun isValidToken(token: String): Boolean {
        return token.any { it.isLetterOrDigit() }
    }

    private fun escapeFtsToken(token: String): String {
        val escaped = token.replace("\"", "\"\"")

        val isOperator = escaped.equals("OR", true)
                || token.equals("AND", true)
                || token.equals("NOT", true)

        return if (isOperator) "\"$escaped\"" else escaped
    }
}
