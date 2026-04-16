package com.quranapp.android.utils.reader

import android.content.Context
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageLineItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory

object ReaderItemsBuilder {
    suspend fun buildVersesForTranslationMode(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        chapterNo: Int,
    ): List<ReaderLayoutItem> {
        if (!QuranMeta.isChapterValid(chapterNo)) return emptyList()

        val verseCount = quranRepository.getChapterVerseCount(chapterNo)
        if (verseCount <= 0) return emptyList()

        val translationFactory = QuranTranslationFactory(context)

        val out = ArrayList<ReaderLayoutItem>()

        out.add(ReaderLayoutItem.ChapterInfo(chapterNo, key = "chapterInfo-$chapterNo"))

        if (chapterNo != 1 && chapterNo != 9) {
            out.add(ReaderLayoutItem.Bismillah(key = "bismillah-$chapterNo"))
        }

        translationFactory.use {
            buildVerses(
                params,
                out,
                it,
                quranRepository,
                chapterNo,
                1,
                verseCount,
            )
        }

        return out
    }

    suspend fun buildJuzVersesForTranslationMode(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        juzNo: Int
    ): List<ReaderLayoutItem> {
        if (!QuranMeta.isJuzValid(juzNo)) return emptyList()

        return buildGroupedVerses(
            context, params, quranRepository,
            quranRepository.getChapterVerseRangesInJuz(juzNo)
        )
    }

    suspend fun buildHizbVersesForTranslationMode(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        hizbNo: Int
    ): List<ReaderLayoutItem> {
        if (!QuranMeta.isHizbValid(hizbNo)) return emptyList()
        return buildGroupedVerses(
            context, params, quranRepository,
            quranRepository.getChapterVerseRangesInHizb(hizbNo)
        )
    }

    private suspend fun buildGroupedVerses(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        chapterRanges: List<Pair<Int, IntRange>>,
    ): List<ReaderLayoutItem> {
        if (chapterRanges.isEmpty()) return emptyList()

        val translationFactory = QuranTranslationFactory(context)
        val out = ArrayList<ReaderLayoutItem>()

        translationFactory.use {
            for ((chapterNo, verseRange) in chapterRanges) {
                if (verseRange.first == 1) {
                    out.add(
                        ReaderLayoutItem.ChapterTitle(
                            chapterNo,
                            key = "chapterTitle-$chapterNo"
                        )
                    )

                    if (chapterNo != 1 && chapterNo != 9) {
                        out.add(ReaderLayoutItem.Bismillah(key = "bismillah-$chapterNo"))
                    }
                }

                buildVerses(
                    params, out, it, quranRepository,
                    chapterNo, verseRange.first, verseRange.last,
                )
            }
        }

        return out
    }

    private suspend fun buildVerses(
        params: TextBuilderParams,
        out: ArrayList<ReaderLayoutItem>,
        factory: QuranTranslationFactory,
        quranRepository: QuranRepository,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ) {
        val scriptCode = ReaderPreferences.getQuranScript()
        val batch = quranRepository.loadChapterVerseBatch(chapterNo, fromVerse, toVerse, scriptCode)
            ?: return
        val surah = batch.surah

        val booksInfo = factory.getTranslationBooksInfoValidated(params.slugs)
        val translationsByVerseIndex = factory.getTranslationsVerseRange(
            params.slugs,
            chapterNo,
            fromVerse,
            toVerse,
        )

        val arabicWrapByPage = HashMap<Int, Pair<ParagraphStyle, SpanStyle>>()

        fun arabicWrapForPage(pageNo: Int): Pair<ParagraphStyle, SpanStyle> =
            arabicWrapByPage.getOrPut(pageNo) {
                val style = getQuranTextStyle(
                    QuranTextStyleParams(
                        context = params.context,
                        fontResolver = params.fontResolver,
                        colors = params.colors,
                        type = params.type,
                        script = params.script,
                        pageNo = pageNo,
                        sizeMultiplier = params.arabicSizeMultiplier,
                    )
                )
                style.toParagraphStyle() to style.toSpanStyle()
            }

        val translationWrapStyles = booksInfo.keys.associateWith { slug ->
            val ts = getTranslationTextStyle(
                TranslationTextStyleParams(
                    slug,
                    params.translationSizeMultiplier,
                )
            )
            ts.toParagraphStyle() to ts.toSpanStyle()
        }

        val labelMutedNonUrdu = SpanStyle(
            color = params.colors.onBackground.alpha(0.6f),
            fontSize = params.type.labelMedium.fontSize,
        )

        val labelMutedUrdu = SpanStyle(
            color = params.colors.onBackground.alpha(0.6f),
            fontSize = params.type.labelMedium.fontSize,
            fontFamily = fontUrdu,
        )

        val wbwStyles = TextLinkStyles(
            focusedStyle = SpanStyle(color = params.colors.primary),
            pressedStyle = SpanStyle(color = params.colors.primary),
            hoveredStyle = SpanStyle(color = params.colors.primary),
        )

        for (verseNo in fromVerse..toVerse) {
            val translations =
                translationsByVerseIndex.getOrElse(verseNo - fromVerse) { emptyList() }

            val ayah = batch.ayahByVerseNo[verseNo] ?: continue
            val words = batch.wordsByVerseNo[verseNo] ?: emptyList()

            if (words.isEmpty()) continue

            val pageNo = batch.pageByVerseNo[verseNo] ?: -1

            val verse = VerseWithDetails(
                words = words,
                pageNo = pageNo,
                verse = ayah,
                chapter = surah
            ).apply {
                this.translations = translations
            }

            if (verse.isVOTD(params.context)) {
                out.add(ReaderLayoutItem.IsVotd(key = "isVotd-$chapterNo:$verseNo"))
            }

            val parsedQuranText = if (params.arabicEnabled) buildAnnotatedString {
                val (paragraphStyle, spanStyle) = arabicWrapForPage(verse.pageNo)

                withStyle(paragraphStyle) {
                    withStyle(spanStyle) {
                        verse.words.forEachIndexed { index, word ->
                            /*withLink(
                                LinkAnnotation.Clickable(
                                    tag = "wbw",
                                    styles = wbwStyles
                                ) {
                                    MessageUtils.showRemovableToast(
                                        params.context,
                                        word.text,
                                        Toast.LENGTH_LONG
                                    )
                                }
                            ) {}*/

                            append(word.text)

                            if (!word.isLastWordOfAyah) {
                                append(" ")
                            }
                        }
                    }
                }
            } else null

            val parsedTranslationTexts = translations.mapNotNull { translation ->
                val bookInfo = booksInfo[translation.bookSlug] ?: return@mapNotNull null

                val (paragraphStyle, translationSpanStyle) =
                    translationWrapStyles[translation.bookSlug] ?: return@mapNotNull null

                val annotatedString = buildAnnotatedString {
                    withStyle(paragraphStyle) {
                        withStyle(translationSpanStyle) {
                            append(
                                buildTranslationAnnotatedString(
                                    translation,
                                    params.colors,
                                    actions = VerseActions(
                                        params.verseActions.onReferenceClick,
                                        onFootnoteClickRaw = { slug, footnoteNo ->
                                            params.verseActions.onFootnoteClick?.invoke(
                                                verse,
                                                translation.footnotes[footnoteNo]
                                            )
                                        }
                                    )
                                )
                            )
                        }

                        append("\n")

                        withStyle(
                            if (bookInfo.isUrdu) labelMutedUrdu else labelMutedNonUrdu
                        ) {
                            append(bookInfo.getDisplayName(false))
                        }
                    }
                }

                Pair(translation.bookSlug, annotatedString)
            }

            out.add(
                ReaderLayoutItem.VerseUI(
                    verse = verse,
                    parsedQuranText = parsedQuranText,
                    parsedTranslationTexts = parsedTranslationTexts,
                    isLastInGroup = verseNo == toVerse,
                    key = "verse-$chapterNo:${verse.verseNo}${params.toKey()}"
                )
            )
        }
    }

    suspend fun buildQuickReferenceItems(
        context: Context,
        params: TextBuilderParams,
        repository: QuranRepository,
        chapterNo: Int,
        verseNos: List<Int>,
    ): List<ReaderLayoutItem> {
        val scriptCode = ReaderPreferences.getQuranScript()
        val batch = repository.loadQuickReferenceBatch(chapterNo, verseNos, scriptCode)
            ?: return emptyList()
        val surah = batch.surah

        val translationFactory = QuranTranslationFactory(context)
        val out = ArrayList<ReaderLayoutItem>(verseNos.size)

        translationFactory.use { factory ->
            val booksInfo = factory.getTranslationBooksInfoValidated(params.slugs)

            val arabicWrapByPage = HashMap<Int, Pair<ParagraphStyle, SpanStyle>>()

            fun arabicWrapForPage(pageNo: Int): Pair<ParagraphStyle, SpanStyle> =
                arabicWrapByPage.getOrPut(pageNo) {
                    val style = getQuranTextStyle(
                        QuranTextStyleParams(
                            context = params.context,
                            fontResolver = params.fontResolver,
                            colors = params.colors,
                            type = params.type,
                            script = params.script,
                            pageNo = pageNo,
                            sizeMultiplier = params.arabicSizeMultiplier,
                        )
                    )
                    style.toParagraphStyle() to style.toSpanStyle()
                }

            val translationWrapStyles = booksInfo.keys.associateWith { slug ->
                val ts = getTranslationTextStyle(
                    TranslationTextStyleParams(slug, params.translationSizeMultiplier)
                )
                ts.toParagraphStyle() to ts.toSpanStyle()
            }

            val labelMutedNonUrdu = SpanStyle(
                color = params.colors.onBackground.alpha(0.6f),
                fontSize = params.type.labelMedium.fontSize,
            )
            val labelMutedUrdu = SpanStyle(
                color = params.colors.onBackground.alpha(0.6f),
                fontSize = params.type.labelMedium.fontSize,
                fontFamily = fontUrdu,
            )

            /*val wbwStyles = TextLinkStyles(
                focusedStyle = SpanStyle(color = params.colors.primary),
                pressedStyle = SpanStyle(color = params.colors.primary),
                hoveredStyle = SpanStyle(color = params.colors.primary),
            )*/

            for ((idx, verseNo) in verseNos.withIndex()) {
                val ayah = batch.ayahByVerseNo[verseNo] ?: continue
                val words = batch.wordsByVerseNo[verseNo] ?: emptyList()
                if (words.isEmpty()) continue

                val pageNo = batch.pageByVerseNo[verseNo] ?: -1

                val translations = factory.getTranslationsVerseRange(
                    params.slugs, chapterNo, verseNo, verseNo
                ).firstOrNull() ?: emptyList()

                val verse = VerseWithDetails(
                    words = words,
                    pageNo = pageNo,
                    verse = ayah,
                    chapter = surah
                ).apply {
                    this.translations = translations
                    includeChapterNameInSerial = true
                }

                val parsedQuranText = if (params.arabicEnabled) buildAnnotatedString {
                    val (paragraphStyle, spanStyle) = arabicWrapForPage(pageNo)
                    withStyle(paragraphStyle) {
                        withStyle(spanStyle) {
                            verse.words.forEachIndexed { index, word ->
                                /*withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "wbw", styles = wbwStyles
                                    ) {
                                        MessageUtils.showRemovableToast(
                                            params.context, word.text, Toast.LENGTH_LONG
                                        )
                                    }
                                ) {}*/

                                append(word.text)

                                if (!word.isLastWordOfAyah) append(" ")
                            }
                        }
                    }
                } else null

                val parsedTranslationTexts = translations.mapNotNull { translation ->
                    val bookInfo = booksInfo[translation.bookSlug] ?: return@mapNotNull null
                    val (paragraphStyle, translationSpanStyle) =
                        translationWrapStyles[translation.bookSlug] ?: return@mapNotNull null

                    val annotatedString = buildAnnotatedString {
                        withStyle(paragraphStyle) {
                            withStyle(translationSpanStyle) {
                                append(
                                    buildTranslationAnnotatedString(
                                        translation, params.colors,
                                        actions = VerseActions(
                                            params.verseActions.onReferenceClick,
                                            onFootnoteClickRaw = { slug, footnoteNo ->
                                                params.verseActions.onFootnoteClick?.invoke(
                                                    verse, translation.footnotes[footnoteNo]
                                                )
                                            }
                                        )
                                    )
                                )
                            }
                            append("\n")
                            withStyle(
                                if (bookInfo.isUrdu) labelMutedUrdu else labelMutedNonUrdu
                            ) { append(bookInfo.getDisplayName(false)) }
                        }
                    }
                    Pair(translation.bookSlug, annotatedString)
                }

                out.add(
                    ReaderLayoutItem.VerseUI(
                        verse = verse,
                        parsedQuranText = parsedQuranText,
                        parsedTranslationTexts = parsedTranslationTexts,
                        isLastInGroup = idx == verseNos.lastIndex,
                        key = "qref-$chapterNo:$verseNo${params.toKey()}"
                    )
                )
            }
        }

        return out
    }

    /**
     * Builds several mushaf pages: batched mushaf_map + juz queries, two-phase ayah word preload.
     */
    suspend fun buildMushafPages(
        quranRepository: QuranRepository,
        fontResolver: FontResolver,
        pageNumbers: Collection<Int>,
        params: PageBuilderParams
    ): Map<Int, QuranPageItem> {
        val distinct = pageNumbers.filter { it > 0 }.distinct().sorted()
        if (distinct.isEmpty()) return emptyMap()

        val scriptCode = ReaderPreferences.getQuranScript()
        val mushafId = scriptCode.toQuranMushafId(ReaderPreferences.getQuranScriptVariant())

        val linesByPage = quranRepository.getPageLinesGroupedForPages(mushafId, distinct)
        val juzByPage = quranRepository.getJuzForMushafPages(mushafId, distinct)
        val ayahRows = linesByPage.values.asSequence()
            .flatten()
            .filter { it.lineType == MushafLineType.ayah }
            .toList()
        val wordCacheFull = quranRepository.preloadMushafLineWordCache(ayahRows, scriptCode)
        val wordCache = wordCacheFull.takeIf { it.isNotEmpty() }

        val out = LinkedHashMap<Int, QuranPageItem>(distinct.size)
        for (pageNo in distinct) {
            val rows = linesByPage[pageNo].orEmpty()
            val lines = ArrayList<QuranPageLineItem>(rows.size)

            val baseStyle = getQuranTextStyle(
                QuranTextStyleParams(
                    context = params.context,
                    fontResolver = fontResolver,
                    colors = params.colors,
                    type = params.type,
                    pageNo = pageNo,
                    script = scriptCode,
                    sizeMultiplier = 1f,
                )
            )

            val contentWidthDp = with(params.density) { params.contentWidthPx.toDp().value }
            val cappedBaseStyle = mushafCappedBaseStyle(baseStyle, contentWidthDp)

            for (row in rows) {
                mapMushafRowToLineItem(
                    row,
                    quranRepository,
                    scriptCode,
                    cappedBaseStyle,
                    params,
                    wordCache,
                )?.let {
                    lines.add(it)
                }
            }

            out[pageNo] = QuranPageItem(
                pageNo = pageNo,
                juzNo = juzByPage[pageNo] ?: -1,
                lines = lines,
            )
        }

        return out
    }

    private suspend fun mapMushafRowToLineItem(
        row: MushafMapEntity,
        quranRepository: QuranRepository,
        scriptCode: String,
        cappedBaseStyle: TextStyle,
        params: PageBuilderParams,
        wordCache: Map<Int, List<AyahWordEntity>>?,
    ): QuranPageLineItem? {
        return when (row.lineType) {
            MushafLineType.surah_name -> {
                val chapter = row.surahNo.takeIf { it != null && it > 0 } ?: return null
                QuranPageLineItem.Title(row.lineNumber, chapter)
            }

            MushafLineType.basmallah -> QuranPageLineItem.Bismillah(row.lineNumber)

            MushafLineType.ayah -> {
                val words = quranRepository.resolveMushafLineWords(row, scriptCode, wordCache)

                val layout = fitMushafLineLayout(
                    words = words,
                    centered = row.isCentered,
                    cappedBaseStyle = cappedBaseStyle,
                    maxLineWidthPx = params.contentWidthPx.toFloat(),
                    lineWidthBounded = true,
                    density = params.density,
                    textMeasurer = params.textMeasurer,
                )

                QuranPageLineItem.Text(
                    lineNo = row.lineNumber,
                    centered = row.isCentered,
                    words = words,
                    layout = layout
                )
            }
        }
    }
}
