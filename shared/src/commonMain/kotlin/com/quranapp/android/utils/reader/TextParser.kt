package com.quranapp.android.utils.reader

import com.quranapp.android.utils.quran.QuranConstants


sealed interface RichTextPart {
    data class Plain(val text: String) : RichTextPart

    data class QuranRef(
        val text: String,
        val slugs: Set<String>,
        val chapter: Int,
        val verses: String
    ) : RichTextPart

    data class FootnoteRef(
        val text: String,
        val slug: String,
        val footnoteNo: Int
    ) : RichTextPart
}

private data class ParsedTag(
    val name: String,
    val isClosing: Boolean,
    val attributes: Map<String, String>
)

private val tagNameRegex = Regex("""^\s*/?\s*([a-zA-Z0-9_-]+)""")

private val attributeRegex = Regex(
    """([a-zA-Z0-9_:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s/>]+))"""
)

private val htmlTagRegex = Regex("<[^>]+>")

private val danglingAmpRegex =
    Regex("&(?!#\\d+;|#x[0-9a-fA-F]+;|[a-zA-Z]+;)")

fun parseTranslationText(
    html: String,
    slug: String
): List<RichTextPart> {
    return try {
        parseMarkup(html, slug)
    } catch (_: Exception) {
        listOf(
            RichTextPart.Plain(
                stripTagsAndDecodeEntities(html)
            )
        )
    }
}

private fun parseMarkup(
    html: String,
    slug: String
): List<RichTextPart> {
    val parts = mutableListOf<RichTextPart>()
    val plainBuffer = StringBuilder()
    val currentText = StringBuilder()

    var currentTag: String? = null
    var currentChapter = -1
    var currentVerses = ""
    var currentFootnoteNo = -1

    fun flushPlain() {
        if (plainBuffer.isNotEmpty()) {
            parts += RichTextPart.Plain(plainBuffer.toString())
            plainBuffer.clear()
        }
    }

    fun flushCurrentText() {
        when (currentTag) {
            QuranConstants.REFERENCE_TAG -> {
                parts += RichTextPart.QuranRef(
                    text = currentText.toString(),
                    slugs = setOf(slug),
                    chapter = currentChapter,
                    verses = currentVerses
                )
            }

            QuranConstants.FOOTNOTE_REF_TAG -> {
                parts += RichTextPart.FootnoteRef(
                    text = currentText.toString(),
                    slug = slug,
                    footnoteNo = currentFootnoteNo
                )
            }
        }

        currentTag = null
        currentText.clear()
    }

    var position = 0

    while (position < html.length) {
        if (html[position] != '<') {
            val nextTag = html.indexOf('<', position)
                .let { if (it == -1) html.length else it }

            val rawText = html.substring(position, nextTag)
            val decodedText = decodeEntities(rawText)

            when (currentTag) {
                QuranConstants.REFERENCE_TAG,
                QuranConstants.FOOTNOTE_REF_TAG -> {
                    currentText.append(decodedText)
                }

                else -> {
                    plainBuffer.append(decodedText)
                }
            }

            position = nextTag
            continue
        }

        val tagEnd = html.indexOf('>', position)

        // Treat a '<' without a closing '>' as text.
        if (tagEnd == -1) {
            val rawText = html.substring(position)
            val decodedText = decodeEntities(rawText)

            when (currentTag) {
                QuranConstants.REFERENCE_TAG,
                QuranConstants.FOOTNOTE_REF_TAG -> currentText.append(decodedText)

                else -> plainBuffer.append(decodedText)
            }

            break
        }

        val rawTag = html.substring(position + 1, tagEnd)
        val parsedTag = parseTag(rawTag)

        if (parsedTag != null) {
            if (!parsedTag.isClosing) {
                when (parsedTag.name) {
                    QuranConstants.REFERENCE_TAG -> {
                        flushPlain()

                        currentTag = QuranConstants.REFERENCE_TAG
                        currentText.clear()

                        currentChapter =
                            parsedTag.attributes[
                                QuranConstants.REFERENCE_ATTR_CHAPTER_NO
                            ]?.toIntOrNull() ?: -1

                        currentVerses =
                            parsedTag.attributes[
                                QuranConstants.REFERENCE_ATTR_VERSES
                            ] ?: ""
                    }

                    QuranConstants.FOOTNOTE_REF_TAG -> {
                        flushPlain()

                        currentTag = QuranConstants.FOOTNOTE_REF_TAG
                        currentText.clear()

                        currentFootnoteNo =
                            parsedTag.attributes[
                                QuranConstants.FOOTNOTE_REF_ATTR_INDEX
                            ]?.toIntOrNull() ?: -1
                    }
                }
            } else {
                when (parsedTag.name) {
                    QuranConstants.REFERENCE_TAG -> {
                        if (currentTag == QuranConstants.REFERENCE_TAG) {
                            flushCurrentText()
                        }
                    }

                    QuranConstants.FOOTNOTE_REF_TAG -> {
                        if (currentTag == QuranConstants.FOOTNOTE_REF_TAG) {
                            flushCurrentText()
                        }
                    }
                }
            }
        }

        position = tagEnd + 1
    }

    flushPlain()

    return parts
}

private fun parseTag(rawTag: String): ParsedTag? {
    val nameMatch = tagNameRegex.find(rawTag) ?: return null

    val matchedName = nameMatch.groupValues[1]
    val isClosing = rawTag.trimStart().startsWith("/")

    val name = matchedName

    val attributes = mutableMapOf<String, String>()

    if (!isClosing) {
        attributeRegex.findAll(rawTag).forEach { match ->
            val attributeName = match.groupValues[1]

            val value = match.groupValues[2]
                .ifEmpty { match.groupValues[3] }
                .ifEmpty { match.groupValues[4] }

            attributes[attributeName] = decodeEntities(value)
        }
    }

    return ParsedTag(
        name = name,
        isClosing = isClosing,
        attributes = attributes
    )
}

private fun stripTagsAndDecodeEntities(raw: String): String {
    return decodeEntities(
        raw.replace(htmlTagRegex, "")
            .replace(danglingAmpRegex, "&amp;")
    )
}

private fun decodeEntities(text: String): String {
    val entityRegex = Regex(
        """&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);"""
    )

    return entityRegex.replace(text) { match ->
        val entity = match.groupValues[1]

        when {
            entity.startsWith("#x", ignoreCase = true) -> {
                entity.substring(2)
                    .toIntOrNull(16)
                    ?.let(::codePointToString)
                    ?: match.value
            }

            entity.startsWith("#") -> {
                entity.substring(1)
                    .toIntOrNull()
                    ?.let(::codePointToString)
                    ?: match.value
            }

            else -> {
                when (entity) {
                    "nbsp" -> "\u00A0"
                    "amp" -> "&"
                    "lt" -> "<"
                    "gt" -> ">"
                    "quot" -> "\""
                    "apos" -> "'"
                    "#39" -> "'"
                    else -> match.value
                }
            }
        }
    }
}

private fun codePointToString(codePoint: Int): String {
    if (codePoint !in 0..0x10FFFF) return "\uFFFD"

    if (codePoint <= 0xFFFF) {
        if (codePoint in 0xD800..0xDFFF) return "\uFFFD"
        return codePoint.toChar().toString()
    }

    val adjusted = codePoint - 0x10000
    val high = 0xD800 + (adjusted shr 10)
    val low = 0xDC00 + (adjusted and 0x3FF)

    return high.toChar().toString() + low.toChar().toString()
}
