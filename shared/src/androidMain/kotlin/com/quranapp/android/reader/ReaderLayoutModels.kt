package com.quranapp.android.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement

sealed class ReaderLayoutItem() {
    abstract val key: String

    data class ChapterInfo(val chapterNo: Int, override val key: String) : ReaderLayoutItem()
    data class Bismillah(override val key: String) : ReaderLayoutItem()
    data class IsVotd(override val key: String) : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int, override val key: String) : ReaderLayoutItem()

    data class VerseUI(
        val verse: VerseWithDetails,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        // list of Pair<langCode, text>
        val parsedTranslationTexts: List<Pair<String, AnnotatedString>> = emptyList(),
        val wbwByWordIndex: Map<Int, WbwWordEntity>? = null,
        val showDivider: Boolean = true,
        override val key: String
    ) : ReaderLayoutItem()

    data class SectionMarker(
        val text: String,
        override val key: String,
    ) : ReaderLayoutItem()
}

data class ReaderPreparedData(
    val items: List<ReaderLayoutItem>,
    /** Quran text style per mushaf page for Arabic in this reader session. */
    val textStyles: Map<Int, TextStyle> = emptyMap(),
)

data class MushafLineLayout(
    val fittedStyle: TextStyle,
    val centeredGap: Dp,
)

sealed class QuranPageLineItem {
    abstract val lineNo: Int

    data class Title(
        override val lineNo: Int, val chapterNo: Int
    ) : QuranPageLineItem()

    data class Bismillah(
        override val lineNo: Int
    ) : QuranPageLineItem()

    data class Text(
        override val lineNo: Int,
        val centered: Boolean,
        val words: List<AyahWordEntity>,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        val layout: MushafLineLayout
    ) : QuranPageLineItem()
}

data class QuranPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val lines: List<QuranPageLineItem>,
    val cacheKey: String,
)

sealed class ReadingFlowSection {
    object Divider : ReadingFlowSection()
    data class Title(val swl: SurahWithLocalizations) : ReadingFlowSection()
    object Bismillah : ReadingFlowSection()

    data class Words(
        val words: List<AyahWordEntity>,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
    ) : ReadingFlowSection()
}

data class ReadingFlowPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNos: List<Int>,
    val chapterNames: String,
    val sections: List<ReadingFlowSection>,
    val cacheKey: String,
)

data class TranslationPageVerse(
    val chapterNo: Int,
    val verseNo: Int,
    /** Index within annotatedText (inclusive). */
    val rangeStart: Int,
    /** Index within annotatedText (Exclusive). */
    val rangeEnd: Int,
)

sealed class TranslationPageSection {
    object Divider : TranslationPageSection()
    data class Title(val swl: SurahWithLocalizations) : TranslationPageSection()
    object Bismillah : TranslationPageSection()

    data class Text(
        val annotatedText: AnnotatedString,
        val verses: List<TranslationPageVerse>,
    ) : TranslationPageSection()
}

data class TranslationPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNos: List<Int>,
    val chapterNames: String,
    val translationSlug: String,
    val sections: List<TranslationPageSection>,
)
