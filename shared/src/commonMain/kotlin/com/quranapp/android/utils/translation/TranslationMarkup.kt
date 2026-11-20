package com.quranapp.android.utils.translation

import com.quranapp.android.utils.importing.ImportException
import com.quranapp.android.utils.importing.ImportFailure

private val versesPattern =
    Regex("^\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*$")

private const val MAX_MARKUP_LENGTH = 100_000

/**
 * Validates translation markup in commonMain.
 *
 * Supported elements:
 * - <fn index="1">...</fn>
 * - <reference chapter="2" verses="1-5"/>
 *
 * This is a lightweight markup validator, not a general-purpose XML parser.
 */
fun requireValidTranslationMarkup(
    text: String,
    chapterNo: Int,
    verseNo: Int,
    field: String,
) {
    val issue = markupIssue(text) ?: return

    throw ImportException(
        ImportFailure.INVALID,
        "Chapter $chapterNo verse $verseNo, $field: $issue",
    )
}

private fun markupIssue(text: String): String? {
    if (!containsMarkup(text)) return null

    return try {
        MarkupValidator(text).validate()
    } catch (error: Exception) {
        error.message ?: error.toString()
    }
}

private fun containsMarkup(text: String): Boolean {
    for (char in text) {
        if (char == '<' || char == '&') return true

        if (char < ' ' && char != '\t' && char != '\n' && char != '\r') {
            return true
        }
    }

    return false
}

private class MarkupValidator(private val text: String) {
    private val stack = mutableListOf<String>()
    private var index = 0

    fun validate(): String? {
        if (text.length > MAX_MARKUP_LENGTH) {
            return "Markup is too large."
        }

        while (index < text.length) {
            when {
                text[index] == '<' -> parseTag()
                else -> parseText()
            }
        }

        if (stack.isNotEmpty()) {
            return "Unclosed tag <${stack.last()}>."
        }

        return null
    }

    private fun parseTag() {
        when {
            startsWith("<!--") -> parseComment()
            startsWith("<![CDATA[") -> parseCdata()
            startsWith("<?") -> parseProcessingInstruction()
            startsWith("</") -> parseClosingTag()
            startsWith("<!") -> fail("Unsupported XML declaration.")
            else -> parseOpeningTag()
        }
    }

    private fun parseOpeningTag() {
        index++ // <

        val name = readName()
        validateTagName(name)

        var selfClosing = false

        while (true) {
            skipWhitespace()

            when {
                index >= text.length -> fail("Unexpected end of input.")

                text[index] == '>' -> {
                    index++
                    break
                }

                text[index] == '/' -> {
                    index++

                    if (index >= text.length || text[index] != '>') {
                        fail("Expected />.")
                    }

                    index++
                    selfClosing = true
                    break
                }

                else -> {
                    val attributeName = readName()
                    parseAttribute(attributeName)
                }
            }
        }

        if (!selfClosing) {
            stack.add(name)
        }
    }

    private fun parseAttribute(attributeName: String) {
        skipWhitespace()

        if (index >= text.length || text[index] != '=') {
            fail("Attribute \"$attributeName\" must have a value.")
        }

        index++
        skipWhitespace()

        if (index >= text.length) {
            fail("Missing value for attribute \"$attributeName\".")
        }

        val quote = text[index]

        if (quote != '"' && quote != '\'') {
            fail("Attribute \"$attributeName\" must use quoted values.")
        }

        index++

        val start = index

        while (index < text.length && text[index] != quote) {
            if (text[index] == '<') {
                fail("Unexpected < in attribute \"$attributeName\".")
            }

            index++
        }

        if (index >= text.length) {
            fail("Unterminated attribute \"$attributeName\".")
        }

        val value = text.substring(start, index)
        index++ // closing quote

        validateEntities(value)
    }

    private fun parseClosingTag() {
        index += 2 // </

        val name = readName()
        skipWhitespace()

        if (index >= text.length || text[index] != '>') {
            fail("Expected >.")
        }

        index++

        if (stack.isEmpty()) {
            fail("Unexpected closing tag </$name>.")
        }

        val expected = stack.removeAt(stack.lastIndex)

        if (expected != name) {
            fail("Expected </$expected> but found </$name>.")
        }
    }

    private fun parseText() {
        val start = index

        while (index < text.length && text[index] != '<') {
            val char = text[index]

            if (char < ' ' && char != '\t' && char != '\n' && char != '\r') {
                fail("Invalid XML character.")
            }

            index++
        }

        validateEntities(text.substring(start, index))
    }

    private fun parseComment() {
        index += 4 // <!--

        val end = text.indexOf("-->", index)

        if (end == -1) {
            fail("Unterminated comment.")
        }

        val comment = text.substring(index, end)

        if ("--" in comment) {
            fail("Invalid comment.")
        }

        index = end + 3
    }

    private fun parseCdata() {
        index += 9 // <![CDATA[

        val end = text.indexOf("]]>", index)

        if (end == -1) {
            fail("Unterminated CDATA section.")
        }

        index = end + 3
    }

    private fun parseProcessingInstruction() {
        index += 2 // <?

        val end = text.indexOf("?>", index)

        if (end == -1) {
            fail("Unterminated processing instruction.")
        }

        index = end + 2
    }

    private fun validateTagName(name: String) {
        when (name) {
            "fn" -> validateFn()
            "reference" -> validateReference()
            else -> fail("Unsupported tag <$name>. Only <fn> and <reference> are allowed.")
        }
    }

    private fun validateFn() {
        // Attribute validation is performed while parsing.
        // Required-attribute validation is performed after parsing the tag.
    }

    private fun validateReference() {
        // Attribute validation is performed while parsing.
        // Required-attribute validation is performed after parsing the tag.
    }

    private fun readName(): String {
        val start = index

        while (index < text.length) {
            val char = text[index]

            if (
                char.isLetterOrDigit() ||
                char == '_' ||
                char == '-' ||
                char == ':' ||
                char == '.'
            ) {
                index++
            } else {
                break
            }
        }

        if (start == index) {
            fail("Expected a tag or attribute name.")
        }

        return text.substring(start, index)
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) {
            index++
        }
    }

    private fun validateEntities(value: String) {
        var i = 0

        while (i < value.length) {
            if (value[i] != '&') {
                i++
                continue
            }

            val end = value.indexOf(';', i + 1)

            if (end == -1) {
                fail("Unterminated entity.")
            }

            val entity = value.substring(i + 1, end)

            when {
                entity == "amp" ||
                        entity == "lt" ||
                        entity == "gt" ||
                        entity == "apos" ||
                        entity == "quot" -> Unit

                entity.startsWith("#x") -> {
                    val code = entity.substring(2).toIntOrNull(16)
                        ?: fail("Invalid numeric entity.")

                    validateXmlCodePoint(code)
                }

                entity.startsWith("#") -> {
                    val code = entity.substring(1).toIntOrNull()
                        ?: fail("Invalid numeric entity.")

                    validateXmlCodePoint(code)
                }

                else -> fail("Unknown entity &$entity;.")
            }

            i = end + 1
        }
    }

    private fun validateXmlCodePoint(code: Int) {
        val valid = code == 0x9 ||
                code == 0xA ||
                code == 0xD ||
                code in 0x20..0xD7FF ||
                code in 0xE000..0xFFFD ||
                code in 0x10000..0x10FFFF

        if (!valid) {
            fail("Invalid XML character reference.")
        }
    }

    private fun startsWith(value: String): Boolean =
        text.startsWith(value, index)

    private fun fail(message: String): Nothing =
        throw IllegalArgumentException(message)
}
