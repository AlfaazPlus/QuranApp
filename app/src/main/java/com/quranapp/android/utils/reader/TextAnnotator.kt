package com.quranapp.android.utils.reader

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import com.quranapp.android.components.quran.subcomponents.Footnote
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.quran.QuranConstants

typealias OnReferenceClick = (slugs: Set<String>, chapterNo: Int, verses: String) -> Unit
typealias OnFootnoteClickRaw = (slug: String, footnoteNo: Int) -> Unit
typealias OnFootnoteClick = (verse: VerseWithDetails, footnote: Footnote?) -> Unit
typealias OnVerseOption = (verse: VerseWithDetails) -> Unit
typealias OnBookmarkRequest = (chapterNo: Int, verseRange: IntRange) -> Unit

data class VerseActions(
    val onReferenceClick: OnReferenceClick,
    val onFootnoteClickRaw: OnFootnoteClickRaw? = null,
    val onFootnoteClick: OnFootnoteClick? = null,
    val onVerseOption: OnVerseOption? = null,
    val onBookmarkRequest: OnBookmarkRequest? = null,
)

val LocalVerseActions = staticCompositionLocalOf<VerseActions> {
    error("VerseActions not provided")
}

fun buildTranslationAnnotatedString(
    translation: Translation,
    colorScheme: ColorScheme,
    actions: VerseActions?,
): AnnotatedString {
    // early check for potential clickables
    if (actions == null) {
        val raw = translation.text
        if ('<' !in raw) {
            return AnnotatedString(raw)
        }
    }

    return buildTranslationAnnotatedString(
        parseTranslationText(translation.text, translation.bookSlug),
        colorScheme,
        actions
    )
}

fun buildTranslationAnnotatedString(
    text: String,
    slug: String,
    colorScheme: ColorScheme,
    actions: VerseActions?,
): AnnotatedString {
    return buildTranslationAnnotatedString(
        parseTranslationText(text, slug),
        colorScheme,
        actions
    )
}

fun buildTranslationAnnotatedString(
    parts: List<RichTextPart>,
    colorScheme: ColorScheme,
    actions: VerseActions?,
): AnnotatedString {
    return buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is RichTextPart.Plain -> {
                    append(part.text)
                }

                is RichTextPart.QuranRef -> {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = QuranConstants.REFERENCE_TAG,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = colorScheme.primary,
                                    background = colorScheme.primary.alpha(0.1f),
                                    fontWeight = FontWeight.Medium
                                ),
                                pressedStyle = SpanStyle(
                                    color = colorScheme.primary,
                                    background = colorScheme.primary.alpha(0.5f),
                                    fontWeight = FontWeight.Medium
                                ),
                            ),
                        ) {
                            actions?.onReferenceClick(part.slugs, part.chapter, part.verses)
                        }
                    ) {
                        append(part.text)
                    }
                }

                is RichTextPart.FootnoteRef -> {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = QuranConstants.FOOTNOTE_REF_TAG,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = if (actions?.onFootnoteClickRaw == null) Color.Transparent else colorScheme.secondary.alpha(
                                        0.7f
                                    ),
                                    baselineShift = BaselineShift.Superscript,
                                    fontSize = if (actions?.onFootnoteClickRaw == null) 0.2.em else 0.8.em
                                ),
                            ),
                        ) {
                            actions?.onFootnoteClickRaw?.invoke(part.slug, part.footnoteNo)
                        }
                    ) {
                        append(part.text)
                    }
                }
            }
        }
    }
}