package com.quranapp.android.utils.reader.translation

import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

private val versesPattern = Regex("^\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*$")

private val xmlParserFactory by lazy { XmlPullParserFactory.newInstance() }

private const val WRAPPER_OPEN = "<root>"
private const val WRAPPER_CLOSE = "</root>"

/**
 * One parser per import. [XmlPullParser] is not thread-safe, so callers must not share it.
 */
fun newTranslationMarkupParser(): XmlPullParser = xmlParserFactory.newPullParser()

fun requireValidTranslationMarkup(
    parser: XmlPullParser,
    text: String,
    chapterNo: Int,
    verseNo: Int,
    field: String,
) {
    val issue = markupIssue(parser, text) ?: return
    throw ImportException(
        ImportFailure.INVALID,
        "Sura $chapterNo verse $verseNo $field: $issue",
    )
}

private fun markupIssue(parser: XmlPullParser, text: String): String? {
    if (!containsMarkup(text)) return null

    return try {
        parser.setInput(RootWrappedReader(text))

        var depth = 0

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (depth == 1) continue
                    val issue = tagIssue(parser)
                    if (issue != null) return issue
                }

                XmlPullParser.END_TAG -> depth--
            }
        }

        null
    } catch (error: Exception) {
        error.message ?: error.toString()
    }
}

private fun containsMarkup(text: String): Boolean {
    for (index in text.indices) {
        val char = text[index]
        if (char == '<' || char == '&') return true
        if (char < ' ' && char != '\t' && char != '\n' && char != '\r') return true
    }

    return false
}

private fun tagIssue(parser: XmlPullParser): String? {
    return when (parser.name) {
        "fn" -> {
            val indexAttr = parser.getAttributeValue(null, "index")
                ?: return "<fn> tag must include index=\"...\""

            val parsed = indexAttr.trim().toIntOrNull()
            if (parsed == null || parsed <= 0) "<fn index> must be a positive integer" else null
        }

        "reference" -> {
            val chapterAttr = parser.getAttributeValue(null, "chapter")
                ?: return "<reference> tag must include chapter=\"...\""

            val parsed = chapterAttr.trim().toIntOrNull()
            if (parsed == null || parsed <= 0) {
                return "<reference chapter> must be a positive integer"
            }

            val versesAttr = parser.getAttributeValue(null, "verses")
            if (!versesAttr.isNullOrEmpty() && !versesPattern.matches(versesAttr)) {
                "<reference verses> must be like \"12\", \"12-15\", or \"12,15,18-20\""
            } else {
                null
            }
        }

        else -> "Unsupported tag <${parser.name}>. Allowed tags: <fn>, <reference>"
    }
}

/**
 * Feeds `<root>`, the original text, and `</root>` without copying the verse into a new string.
 */
private class RootWrappedReader(private val body: String) : Reader() {
    private var section = 0
    private var offset = 0

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (len == 0) return 0

        var written = 0

        while (written < len && section < 3) {
            val source = when (section) {
                0 -> WRAPPER_OPEN
                1 -> body
                else -> WRAPPER_CLOSE
            }

            val remaining = source.length - offset

            if (remaining <= 0) {
                section++
                offset = 0
                continue
            }

            val count = minOf(len - written, remaining)

            source.toCharArray(cbuf, off + written, offset, offset + count)

            offset += count
            written += count
        }

        return if (written == 0) -1 else written
    }

    override fun close() = Unit
}
