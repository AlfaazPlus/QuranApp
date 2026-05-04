package com.quranapp.android.utils.reader

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.quranapp.android.R
import com.quranapp.android.compose.utils.appPlatformLocale
import com.quranapp.android.compose.utils.formatString
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.reader.atlas.AtlasAyahRasterizer
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.QuranAtlasLoader
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.StringUtils
import kotlin.math.roundToInt

data class QuranVerseWebItem(
    val details: VerseWithDetails,
)

data class QuranVerseWebRequest(
    val context: Context,
    val colorScheme: ColorScheme,
    val scriptCode: String,
    val repository: QuranRepository,
    val atlasAyahImageCache: MutableMap<String, ByteArray>,
    val includeOpenInReaderLink: Boolean,
    val verses: List<QuranVerseWebItem>,
)

data class QuranVerseArabicWebResult(
    val css: String,
    val verseCardHtmlByRef: Map<Pair<Int, Int>, String>,
)

suspend fun buildQuranVerseWebAssets(request: QuranVerseWebRequest): QuranVerseArabicWebResult {
    val translationFactory = QuranTranslationFactory(request.context)
    val isAtlas = request.scriptCode.isQuranAtlasScript()
    val isKFQPC = request.scriptCode.isKFQPCScript()

    val atlasBundle = if (isAtlas) {
        QuranAtlasLoader.getBundle(
            request.context,
            DatabaseProvider.getExternalQuranDatabase(request.context),
            request.scriptCode,
        )
    } else {
        null
    }

    val fontPageNos = HashSet<Int>()
    val cardByRef = LinkedHashMap<Pair<Int, Int>, String>()

    val chapterNames = request.repository.getChapterNames(request.verses.map {
        it.details.chapterNo
    })

    val openLabelEscaped = if (request.includeOpenInReaderLink) {
        escapePlainForHtml(request.context.getString(R.string.strLabelOpenInReader))
    } else {
        ""
    }

    val atlasRasterParams = if (isAtlas) verseWebAtlasRasterParams(
        request.context,
        request.colorScheme,
        request.scriptCode,
    ) else null

    for (item in request.verses) {
        val vwd = item.details
        val chapterNo = vwd.chapterNo
        val verseNo = vwd.verseNo
        val key = chapterNo to verseNo

        if (isKFQPC) {
            fontPageNos.add(vwd.pageNo)
        }

        val arabicHtml = buildVerseArabicParagraphHtml(
            scriptCode = request.scriptCode,
            isAtlas = isAtlas,
            isKFQPC = isKFQPC,
            atlasBundle = atlasBundle,
            atlasAyahImageCache = request.atlasAyahImageCache,
            atlasRasterParams = atlasRasterParams,
            vwd = vwd,
        )

        val translations = translationFactory.getTranslationsSingleVerse(chapterNo, verseNo)
        val primaryTransl = translations.firstOrNull()
        val translationRaw = primaryTransl?.text.orEmpty()

        val translationEscaped = escapePlainForHtml(plainTranslationForWeb(translationRaw))

        val authorAttributionHtml = primaryTransl?.bookSlug?.takeIf { it.isNotEmpty() }?.let { slug ->
            val label = translationFactory.getTranslationBookInfo(slug).getDisplayName(false).trim()
            if (label.isEmpty()) null
            else "<p class=\"translation-attribution\">${escapePlainForHtml(label)}</p>"
        }.orEmpty()

        val refName = formatString(
            request.context,
            $$"%1$s %2$d:%3$d",
            chapterNames[chapterNo] ?: "",
            chapterNo,
            verseNo,
        )

        val refNameEscaped = escapePlainForHtml(refName)

        val footerInner = buildString {
            append("<span>").append(refNameEscaped).append("</span>")

            if (request.includeOpenInReaderLink) {
                append("<span class=\"sep\"></span><a href=\"https://quranapp.verse.reader/$chapterNo/$verseNo\">")
                append(openLabelEscaped)
                append("</a>")
            }
        }

        cardByRef[key] =
            "<div class=\"verse-ref-card verse-ref-card--embedded\">" +
                arabicHtml +
                "<p class=\"translation\">" +
                translationEscaped.ifEmpty { "\u00A0" } +
                "</p>" +
                authorAttributionHtml +
                "<div class=\"footer\">$footerInner</div>" +
                "</div>"
    }

    return QuranVerseArabicWebResult(
        css = quranWebViewArabicFontFaceCss(isAtlas, isKFQPC, fontPageNos),
        verseCardHtmlByRef = cardByRef,
    )
}

private fun escapePlainForHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun plainTranslationForWeb(translationRaw: String): String {
    val stripped = StringUtils.removeHTML(translationRaw, false)
    if (stripped.isEmpty()) return ""
    return HtmlCompat.fromHtml(stripped, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
}

private data class VerseWebAtlasRasterParams(
    val fontSizePx: Float,
    val lineHeightPx: Float,
    val wordGapPx: Float,
    val maxLineWidthPx: Float,
    val onSurfaceArgb: Int,
    val colorHex: String,
    val sizeCacheKey: Int,
    val maxLineCacheKey: Int,
    val density: Float,
)

private fun verseWebAtlasRasterParams(
    context: Context,
    colorScheme: ColorScheme,
    scriptCode: String,
): VerseWebAtlasRasterParams {
    val dm = context.resources.displayMetrics
    val fontSizePx = context.resources.getDimension(
        scriptCode.getQuranScriptVerseTextSizeMediumRes()
    ) * ReaderPreferences.getArabicTextSizeMultiplier()

    val lineHeightPx = fontSizePx * 1.5f
    val wordGapPx = fontSizePx * 0.15f
    val maxLineWidthPx = (dm.widthPixels - 72f * dm.density).coerceAtLeast(240f)
    val onSurfaceArgb = colorScheme.onSurface.toArgb()

    val colorHex = String.format(
        appPlatformLocale(),
        "%06X",
        0xFFFFFF and onSurfaceArgb,
    )

    return VerseWebAtlasRasterParams(
        fontSizePx = fontSizePx,
        lineHeightPx = lineHeightPx,
        wordGapPx = wordGapPx,
        maxLineWidthPx = maxLineWidthPx,
        onSurfaceArgb = onSurfaceArgb,
        colorHex = colorHex,
        sizeCacheKey = fontSizePx.roundToInt(),
        maxLineCacheKey = maxLineWidthPx.roundToInt(),
        density = dm.density
    )
}

private fun quranWebViewArabicFontFaceCss(
    isAtlas: Boolean,
    isKFQPC: Boolean,
    fontPageNos: Set<Int>,
): String = buildString {
    if (!isAtlas && isKFQPC) {
        fontPageNos.forEach { pageNo ->
            append(
                "@font-face { font-family: page_$pageNo; src: url('https://assets-font/quran-arabic/page_$pageNo'); }",
            )
        }
    } else if (!isAtlas) {
        append(
            "@font-face { font-family: quran-arabic; src: url('https://assets-font/quran-arabic'); }",
        )
    }
}

private suspend fun buildVerseArabicParagraphHtml(
    scriptCode: String,
    isAtlas: Boolean,
    isKFQPC: Boolean,
    atlasBundle: QuranAtlasBundle?,
    atlasAyahImageCache: MutableMap<String, ByteArray>,
    atlasRasterParams: VerseWebAtlasRasterParams?,
    vwd: VerseWithDetails,
): String {
    if (vwd.words.isEmpty()) {
        return ""
    }

    val quranText = vwd.words.joinToString(" ") { it.text }

    val placements = if (isAtlas && atlasBundle != null && vwd.words.isNotEmpty()) {
        atlasBundle.getPlacementsForWords(vwd.words)
    } else {
        emptyMap()
    }

    return when {
        isAtlas -> {
            if (atlasBundle == null) {
                ""
            } else {
                val p = atlasRasterParams!!

                val cacheUrl = "https://assets-atlas".toUri().buildUpon()
                    .appendPath("ayah")
                    .appendPath(scriptCode)
                    .appendPath("${vwd.chapterNo}")
                    .appendPath("${vwd.verseNo}")
                    .appendQueryParameter("s", "${p.fontSizePx}")
                    .appendQueryParameter("w", "${p.maxLineWidthPx}")
                    .appendQueryParameter("c", p.colorHex)
                    .build()
                    .toString()

                val png = AtlasAyahRasterizer.renderAyahToPng(
                    atlasBundle,
                    vwd.words,
                    placements,
                    p.fontSizePx,
                    p.lineHeightPx,
                    p.onSurfaceArgb,
                    p.wordGapPx,
                    maxLineWidthPx = p.maxLineWidthPx,
                )

                if (png != null) {
                    atlasAyahImageCache[cacheUrl] = png
                }

                val atlasImgHtml = verseWebAtlasAyahImgHtml(
                    cacheUrl = cacheUrl,
                    pngBytes = png,
                    alt = "${vwd.chapterNo}:${vwd.verseNo}",
                    density = p.density,
                )

                "<p class=\"arabic atlas-img\" dir=\"rtl\">$atlasImgHtml</p>"
            }
        }

        isKFQPC -> {
            "<p class='arabic' style='font-family:page_${vwd.pageNo}'>$quranText</p>"
        }

        else -> {
            "<p class='arabic' style='font-family:quran-arabic'>$quranText</p>"
        }
    }
}

private fun verseWebAtlasAyahImgHtml(
    cacheUrl: String,
    pngBytes: ByteArray?,
    alt: String,
    density: Float,
): String {
    val dim = pngBytes?.let { pngIntrinsicPhysicalSizePx(it) }

    return if (dim != null) {
        val (wPhys, hPhys) = dim

        val scale = density.coerceAtLeast(0.5f)
        val w = (wPhys / scale).roundToInt().coerceAtLeast(1)
        val h = (hPhys / scale).roundToInt().coerceAtLeast(1)

        """<img src="$cacheUrl" width="$w" height="$h" alt="$alt" style="width:${w}px;max-width:100%;height:auto;" />"""
    } else {
        """<img src="$cacheUrl" alt="$alt" />"""
    }
}

private fun pngIntrinsicPhysicalSizePx(pngBytes: ByteArray): Pair<Int, Int>? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, opts)

    val w = opts.outWidth
    val h = opts.outHeight

    return if (w > 0 && h > 0) w to h else null
}
