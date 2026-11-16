package com.quranapp.android.utils.reader

import com.quranapp.android.utils.quran.QuranConstants
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

private val translationXmlPullParserFactory by lazy {
    XmlPullParserFactory.newInstance()
}

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

fun parseTranslationText(html: String, slug: String): List<RichTextPart> {
    return try {
        val parts = mutableListOf<RichTextPart>()

        val parser = translationXmlPullParserFactory.newPullParser()
        parser.setInput(StringReader("<root>$html</root>"))

        val plainBuffer = StringBuilder()

        var currentTag: String? = null
        var currentText = StringBuilder()

        var currentChapter = -1
        var currentVerses = ""
        var currentFootnoteNo = -1

        fun flushPlain() {
            if (plainBuffer.isNotEmpty()) {
                parts += RichTextPart.Plain(plainBuffer.toString())
                plainBuffer.clear()
            }
        }

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        QuranConstants.REFERENCE_TAG -> {
                            flushPlain()
                            currentTag = QuranConstants.REFERENCE_TAG
                            currentText = StringBuilder()
                            currentChapter =
                                parser.getAttributeValue(
                                    null,
                                    QuranConstants.REFERENCE_ATTR_CHAPTER_NO
                                )
                                    ?.toIntOrNull() ?: -1
                            currentVerses =
                                parser.getAttributeValue(null, QuranConstants.REFERENCE_ATTR_VERSES)
                                    ?: ""
                        }

                        QuranConstants.FOOTNOTE_REF_TAG -> {
                            flushPlain()
                            currentTag = QuranConstants.FOOTNOTE_REF_TAG
                            currentText = StringBuilder()
                            currentFootnoteNo =
                                parser.getAttributeValue(
                                    null,
                                    QuranConstants.FOOTNOTE_REF_ATTR_INDEX
                                )
                                    ?.toIntOrNull() ?: -1
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text.orEmpty()
                    when (currentTag) {
                        QuranConstants.REFERENCE_TAG, QuranConstants.FOOTNOTE_REF_TAG -> currentText.append(
                            text
                        )

                        else -> plainBuffer.append(text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        QuranConstants.REFERENCE_TAG -> {
                            parts += RichTextPart.QuranRef(
                                text = currentText.toString(),
                                slugs = setOf(slug),
                                chapter = currentChapter,
                                verses = currentVerses
                            )
                            currentTag = null
                            currentText = StringBuilder()
                        }

                        QuranConstants.FOOTNOTE_REF_TAG -> {
                            parts += RichTextPart.FootnoteRef(
                                text = currentText.toString(),
                                slug = slug,
                                footnoteNo = currentFootnoteNo
                            )
                            currentTag = null
                            currentText = StringBuilder()
                        }
                    }
                }
            }
            parser.next()
        }

        flushPlain()
        parts
    } catch (e: Exception) {
        e.printStackTrace()
        // Some translations/footnotes can contain malformed entities (e.g. stray '&'),
        listOf(RichTextPart.Plain(stripTagsAndDecodeEntities(html)))
    }
}

private val htmlTagRegex = Regex("<[^>]+>")
private val danglingAmpRegex = Regex("&(?!#\\d+;|#x[0-9a-fA-F]+;|[a-zA-Z]+;)")

private fun stripTagsAndDecodeEntities(raw: String): String {
    return raw
        .replace(htmlTagRegex, "")
        .replace("&nbsp;", "\u00A0")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(danglingAmpRegex, "&amp;")
}
