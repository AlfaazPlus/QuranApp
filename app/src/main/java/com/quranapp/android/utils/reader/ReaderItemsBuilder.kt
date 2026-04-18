package com.quranapp.android.utils.reader

import android.content.Context
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.R
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageLineItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderPreparedData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.ChapterVerseBatch
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory

private data class SectionSnapshot(
    val page: Int,
    val ruku: Int,
    val rub: Int,
    val manzil: Int,
)

object ReaderItemsBuilder {
    suspend fun buildVersesForTranslationMode(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        chapterNo: Int,
    ): ReaderPreparedData? {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            return null
        }

        val verseCount = quranRepository.getChapterVerseCount(chapterNo)
        if (verseCount <= 0) return null

        val translationFactory = QuranTranslationFactory(context)

        val out = ArrayList<ReaderLayoutItem>()
        val textStyles = HashMap<Int, TextStyle>()

        out.add(ReaderLayoutItem.ChapterInfo(chapterNo, key = "chapterInfo-$chapterNo"))

        if (chapterNo != 1 && chapterNo != 9) {
            out.add(ReaderLayoutItem.Bismillah(key = "bismillah-$chapterNo"))
        }

        translationFactory.use {
            buildReaderVerses(
                params,
                out,
                textStyles,
                it,
                quranRepository,
                chapterNo,
                1,
                verseCount,
            )
        }

        return ReaderPreparedData(out, textStyles)
    }

    suspend fun buildJuzVersesForTranslationMode(
        context: Context,
        params: TextBuilderParams,
        quranRepository: QuranRepository,
        juzNo: Int
    ): ReaderPreparedData? {
        if (!QuranMeta.isJuzValid(juzNo)) {
            return null
        }

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
    ): ReaderPreparedData? {
        if (!QuranMeta.isHizbValid(hizbNo)) {
            return null
        }

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
    ): ReaderPreparedData? {
        if (chapterRanges.isEmpty()) return null

        val translationFactory = QuranTranslationFactory(context)
        val out = ArrayList<ReaderLayoutItem>()
        val textStyles = HashMap<Int, TextStyle>()

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

                buildReaderVerses(
                    params, out, textStyles, it, quranRepository,
                    chapterNo, verseRange.first, verseRange.last,
                )
            }
        }

        return ReaderPreparedData(out, textStyles)
    }

    private suspend fun buildReaderVerses(
        params: TextBuilderParams,
        out: ArrayList<ReaderLayoutItem>,
        textStyles: MutableMap<Int, TextStyle>,
        factory: QuranTranslationFactory,
        quranRepository: QuranRepository,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ) {
        val wbwTranslationEnabled = ReaderPreferences.getWbwShowTranslation()
        val wbwTransliterationEnabled = ReaderPreferences.getWbwShowTransliteration()
        val wbwId = ReaderPreferences.getWbwId()

        val scriptCode = ReaderPreferences.getQuranScript()
        val chapterVerseCount = quranRepository.getChapterVerseCount(chapterNo)
        val batchHi = minOf(toVerse + 1, chapterVerseCount)
        val batch = quranRepository.loadChapterVerseBatch(chapterNo, fromVerse, batchHi, scriptCode)
            ?: return
        val surah = batch.surah

        val booksInfo = factory.getTranslationBooksInfoValidated(params.slugs)
        val translationsByVerseIndex = factory.getTranslationsVerseRange(
            params.slugs,
            chapterNo,
            fromVerse,
            toVerse,
        )

        fun ensureQuranTextStyleForPage(pageNo: Int) {
            textStyles.getOrPut(pageNo) {
                getQuranTextStyle(
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
            }
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

        val wbwByAyah =
            if (wbwId != null && (wbwTranslationEnabled || wbwTransliterationEnabled)) {
                val ids =
                    (fromVerse..toVerse).mapNotNull { vn -> batch.ayahByVerseNo[vn]?.ayahId }
                if (ids.isEmpty()) emptyMap()
                else quranRepository.getWbwWordsForAyahs(wbwId, ids)
            } else emptyMap()

        var prevSection: SectionSnapshot? = null

        for (verseNo in fromVerse..toVerse) {
            val translations =
                translationsByVerseIndex.getOrElse(verseNo - fromVerse) { emptyList() }

            val ayah = batch.ayahByVerseNo[verseNo] ?: continue
            val words = batch.wordsByVerseNo[verseNo] ?: emptyList()
            val pageNo = batch.pageByVerseNo[verseNo] ?: -1
            val cur = SectionSnapshot(
                page = pageNo,
                ruku = ayah.rukuNo,
                rub = ayah.rubNo,
                manzil = ayah.manzilNo,
            )

            out.addSectionMarker(params.context, chapterNo, verseNo, cur, prevSection)
            prevSection = cur

            if (words.isEmpty()) continue

            ensureQuranTextStyleForPage(pageNo)

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
                    parsedTranslationTexts = parsedTranslationTexts,
                    wbwByWordIndex = wbwByAyah[verse.id]?.takeIf { it.isNotEmpty() },
                    showDivider = verseNo != toVerse,
                    key = "verse-$chapterNo:${verse.verseNo}${params.toKey()}"
                )
            )
        }

        out.addSectionMarkerAtRangeEnd(
            params.context,
            quranRepository,
            scriptCode,
            chapterNo = chapterNo,
            toVerse = toVerse,
            verseCount = chapterVerseCount,
            batch = batch,
        )
    }

    suspend fun buildQuickReferenceItems(
        context: Context,
        params: TextBuilderParams,
        repository: QuranRepository,
        chapterNo: Int,
        verseNos: List<Int>,
    ): ReaderPreparedData? {
        val wbwTranslationEnabled = ReaderPreferences.getWbwShowTranslation()
        val wbwTransliterationEnabled = ReaderPreferences.getWbwShowTransliteration()
        val wbwId = ReaderPreferences.getWbwId()
        val scriptCode = ReaderPreferences.getQuranScript()

        val batch = repository.loadQuickReferenceBatch(chapterNo, verseNos, scriptCode)
            ?: return null
        val surah = batch.surah

        val translationFactory = QuranTranslationFactory(context)
        val out = ArrayList<ReaderLayoutItem>(verseNos.size)

        val textStyles = HashMap<Int, TextStyle>()

        translationFactory.use { factory ->
            val booksInfo = factory.getTranslationBooksInfoValidated(params.slugs)

            fun ensureQuranTextStyleForPage(pageNo: Int) {
                textStyles.getOrPut(pageNo) {
                    getQuranTextStyle(
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
                }
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

            val wbwByAyah =
                if (wbwId != null && (wbwTranslationEnabled || wbwTransliterationEnabled)) {
                    val ids = verseNos.mapNotNull { batch.ayahByVerseNo[it]?.ayahId }
                    if (ids.isEmpty()) emptyMap()
                    else repository.getWbwWordsForAyahs(wbwId, ids)
                } else emptyMap()

            for ((idx, verseNo) in verseNos.withIndex()) {
                val ayah = batch.ayahByVerseNo[verseNo] ?: continue
                val words = batch.wordsByVerseNo[verseNo] ?: emptyList()
                if (words.isEmpty()) continue

                val pageNo = batch.pageByVerseNo[verseNo] ?: -1
                ensureQuranTextStyleForPage(pageNo)

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
                        parsedTranslationTexts = parsedTranslationTexts,
                        wbwByWordIndex = wbwByAyah[verse.id]?.takeIf { it.isNotEmpty() },
                        showDivider = idx != verseNos.lastIndex,
                        key = "qref-$chapterNo:$verseNo${params.toKey()}"
                    )
                )
            }
        }

        return ReaderPreparedData(out, textStyles)
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

    private fun ArrayList<ReaderLayoutItem>.addSectionMarker(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        cur: SectionSnapshot,
        prev: SectionSnapshot?,
    ) {
        val pageEnded = prev != null && cur.page > 0 && cur.page != prev.page
        val rukuEnded = prev != null && cur.ruku > 0 && cur.ruku != prev.ruku
        val rubEnded = prev != null && cur.rub > 0 && cur.rub != prev.rub
        val manzilEnded = prev != null && cur.manzil > 0 && cur.manzil != prev.manzil

        if (!pageEnded && !rukuEnded && !rubEnded && !manzilEnded) {
            return
        }

        val prevSnap = checkNotNull(prev)

        val pageNo = prevSnap.page.takeIf { pageEnded }
        val rukuNo = prevSnap.ruku.takeIf { rukuEnded }
        val rubNo = prevSnap.rub.takeIf { rubEnded }
        val manzilNo = prevSnap.manzil.takeIf { manzilEnded }

        val text = context.formatSectionMarkerLabel(pageNo, rukuNo, rubNo, manzilNo)
        if (text.isEmpty()) return

        add(
            ReaderLayoutItem.SectionMarker(
                text = text,
                key = buildString {
                    append("section-$chapterNo:after:${verseNo - 1}")
                    if (pageEnded) append("-p${prevSnap.page}")
                    if (rukuEnded) append("-r${prevSnap.ruku}")
                    if (rubEnded) append("-rb${prevSnap.rub}")
                    if (manzilEnded) append("-mz${prevSnap.manzil}")
                },
            )
        )

        clearDividerBeforeMarker(verseNo = verseNo)
    }

    private suspend fun ArrayList<ReaderLayoutItem>.addSectionMarkerAtRangeEnd(
        context: Context,
        quranRepository: QuranRepository,
        scriptCode: String,
        chapterNo: Int,
        toVerse: Int,
        verseCount: Int,
        batch: ChapterVerseBatch,
    ) {
        val lastAyah = batch.ayahByVerseNo[toVerse] ?: return
        val lastPage = batch.pageByVerseNo[toVerse] ?: -1
        val lastRuku = lastAyah.rukuNo
        val lastRub = lastAyah.rubNo
        val lastManzil = lastAyah.manzilNo
        val isLastVerseOfChapter = toVerse == verseCount

        val nextAyahInChapter = batch.ayahByVerseNo[toVerse + 1]
            ?: quranRepository.getAyah(chapterNo, toVerse + 1).takeIf { !isLastVerseOfChapter }

        val rukuEnded = isLastVerseOfChapter ||
                (nextAyahInChapter != null && nextAyahInChapter.rukuNo != lastRuku)

        val nextAyahAfterRange = when {
            !isLastVerseOfChapter ->
                batch.ayahByVerseNo[toVerse + 1]
                    ?: quranRepository.getAyah(chapterNo, toVerse + 1)

            QuranMeta.isChapterValid(chapterNo + 1) ->
                quranRepository.getAyah(chapterNo + 1, 1)

            else -> null
        }

        val nextPage: Int? = when {
            !isLastVerseOfChapter -> {
                val p = batch.pageByVerseNo[toVerse + 1]
                if (p != null && p > 0) p
                else quranRepository.getPageForVerse(chapterNo, toVerse + 1, scriptCode)
            }

            QuranMeta.isChapterValid(chapterNo + 1) ->
                quranRepository.getPageForVerse(chapterNo + 1, 1, scriptCode)

            else -> null
        }

        val pageEnded = lastPage > 0 &&
                nextPage != null &&
                nextPage > 0 &&
                lastPage != nextPage

        val nextRub = nextAyahAfterRange?.rubNo
        val nextManzil = nextAyahAfterRange?.manzilNo
        val rubEnded = lastRub > 0 && nextRub != null && nextRub > 0 && lastRub != nextRub
        val manzilEnded =
            lastManzil > 0 && nextManzil != null && nextManzil > 0 && lastManzil != nextManzil

        val pageForMarker = lastPage.takeIf { pageEnded }
        val rukuForMarker = lastRuku.takeIf { rukuEnded && lastRuku > 0 }
        val rubForMarker = lastRub.takeIf { rubEnded && lastRub > 0 }
        val manzilForMarker = lastManzil.takeIf { manzilEnded && lastManzil > 0 }

        if (pageForMarker == null && rukuForMarker == null && rubForMarker == null &&
            manzilForMarker == null
        ) {
            return
        }

        val text = context.formatSectionMarkerLabel(
            pageForMarker,
            rukuForMarker,
            rubForMarker,
            manzilForMarker,
        )
        if (text.isEmpty()) return

        add(
            ReaderLayoutItem.SectionMarker(
                text = text,
                key = buildString {
                    append("section-$chapterNo:after:$toVerse-end")
                    pageForMarker?.let { append("-p$it") }
                    rukuForMarker?.let { append("-r$it") }
                    rubForMarker?.let { append("-rb$it") }
                    manzilForMarker?.let { append("-mz$it") }
                },
            )
        )

        clearDividerBeforeMarker(verseNo = toVerse + 1)
    }

    private fun ArrayList<ReaderLayoutItem>.clearDividerBeforeMarker(verseNo: Int) {
        val clearFrom = verseNo - 1
        if (clearFrom < 1) return
        for (i in lastIndex downTo 0) {
            val item = get(i)
            if (item is ReaderLayoutItem.VerseUI && item.verse.verseNo == clearFrom) {
                if (item.showDivider) {
                    set(i, item.copy(showDivider = false))
                }
                break
            }
        }
    }

    private fun Context.formatSectionMarkerLabel(
        pageNo: Int?,
        rukuNo: Int?,
        rubNo: Int?,
        manzilNo: Int?,
    ): String {
        val parts = buildList {
            pageNo?.takeIf { it > 0 }?.let { add(getString(R.string.endOfPageNo, it)) }
            rukuNo?.takeIf { it > 0 }?.let { add(getString(R.string.endOfRukuNo, it)) }
            rubNo?.takeIf { it > 0 }?.let { add(getString(R.string.endOfRubNo, it)) }
            manzilNo?.takeIf { it > 0 }?.let { add(getString(R.string.endOfManzilNo, it)) }
        }

        return parts.chunked(2).joinToString("\n") { chunk ->
            chunk.joinToString(" · ")
        }
    }
}
