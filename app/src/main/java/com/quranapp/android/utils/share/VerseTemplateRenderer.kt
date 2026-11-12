package com.quranapp.android.utils.share

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.BidiFormatter
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.RichTextPart
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.atlas.AtlasGlyphJson
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.getForWord
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.utils.reader.parseTranslationText
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val ATLAS_WORD_GAP_FRACTION = 0.20f

data class VerseRenderInput(
    val template: VerseTemplate,
    val arabicWords: List<AyahWordEntity>,
    val pageNo: Int,
    val scriptCode: String,
    val atlasBundle: QuranAtlasBundle?,
    val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
    val translationText: String,
    val translationSlug: String,
    val referenceText: String,
    val showArabic: Boolean,
    val showTranslation: Boolean,
    val showReference: Boolean,
    val showLogo: Boolean,
)

private class UnitAtlasGlyph(
    val unitX: Float,
    val unitYRelBaseline: Float,
    val unitW: Float,
    val unitH: Float,
    val glyph: AtlasGlyphJson,
)

private class UnitAtlasWord(
    val unitWidth: Float,
    val unitMinRelY: Float,
    val unitMaxRelY: Float,
    val glyphs: List<UnitAtlasGlyph>,
)

private class UnitAtlasVerse(
    val bundle: QuranAtlasBundle,
    val unitAscent: Float,
    val unitDescent: Float,
    val words: List<UnitAtlasWord>,
)

internal sealed interface PreparedFlowBlock {
    val topY: Float

    class StaticText(
        override val topY: Float,
        val leftX: Float,
        val staticLayout: StaticLayout,
    ) : PreparedFlowBlock

    class AtlasArabic(
        override val topY: Float,
        val bundle: QuranAtlasBundle,
        val color: Int,
        val glyphs: Array<AtlasGlyphJson>,
        val dstCoords: FloatArray, // 4 floats per glyph: left, top, right, bottom
    ) : PreparedFlowBlock
}

internal class PreparedFreeRef(
    val leftX: Float,
    val topY: Float,
    val staticLayout: StaticLayout,
)

internal class PreparedLogo(
    val bitmap: Bitmap,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val alpha: Int,
)

class PreparedVerseScene internal constructor(
    val templateId: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val fitResult: VerseFitResult,
    val fallbackBgColor: Int,
    private val flowBlocks: List<PreparedFlowBlock>,
    private val freeRef: PreparedFreeRef?,
    private val logo: PreparedLogo?,
) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val atlasPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val srcRect = Rect()
    private val dstRectF = RectF()
    private var cachedAtlasColor: Int = 0
    private var cachedAtlasFilter: PorterDuffColorFilter? = null

    /**
     * Draws the complete template stack onto [canvas] in `[0..canvasWidth, 0..canvasHeight]`.
     *
     * @param allowBlockingTextureLoad true on background export threads; false during UI preview draw.
     */
    fun draw(
        canvas: Canvas,
        bgBitmap: Bitmap?,
        allowBlockingTextureLoad: Boolean = false,
    ) {
        val cw = canvasWidth.toFloat()
        val ch = canvasHeight.toFloat()

        // 1. Background (object-fit: cover)
        if (bgBitmap != null && !bgBitmap.isRecycled && bgBitmap.width > 0 && bgBitmap.height > 0) {
            computeCenterCropSrcRect(
                srcWidth = bgBitmap.width,
                srcHeight = bgBitmap.height,
                dstWidth = canvasWidth,
                dstHeight = canvasHeight,
                outRect = srcRect,
            )
            dstRectF.set(0f, 0f, cw, ch)
            canvas.drawBitmap(bgBitmap, srcRect, dstRectF, bgPaint)
        } else {
            fallbackPaint.color = fallbackBgColor
            canvas.drawRect(0f, 0f, cw, ch, fallbackPaint)
        }

        // 2. Safe-area flow column
        if (flowBlocks.isNotEmpty()) {
            canvas.withSave {
                if (fitResult.stage == VerseFitStage.REJECTED) {
                    clipRect(
                        fitResult.safeLeftPx,
                        fitResult.safeTopPx,
                        fitResult.safeLeftPx + fitResult.safeWidthPx,
                        fitResult.safeTopPx + fitResult.safeHeightPx,
                    )
                }

                for (block in flowBlocks) {
                    when (block) {
                        is PreparedFlowBlock.StaticText -> {
                            canvas.withSave {
                                translate(block.leftX, block.topY)
                                block.staticLayout.draw(this)
                            }
                        }

                        is PreparedFlowBlock.AtlasArabic -> {
                            drawAtlasBlock(canvas, block, allowBlockingTextureLoad)
                        }
                    }
                }
            }
        }

        // 3. Free reference
        if (freeRef != null) {
            canvas.withSave {
                translate(freeRef.leftX, freeRef.topY)
                freeRef.staticLayout.draw(this)
            }
        }

        // 4. Logo
        if (logo != null && !logo.bitmap.isRecycled && logo.alpha > 0) {
            srcRect.set(0, 0, logo.bitmap.width, logo.bitmap.height)
            dstRectF.set(logo.left, logo.top, logo.right, logo.bottom)
            logoPaint.alpha = logo.alpha
            canvas.drawBitmap(logo.bitmap, srcRect, dstRectF, logoPaint)
        }
    }

    private fun drawAtlasBlock(
        canvas: Canvas,
        block: PreparedFlowBlock.AtlasArabic,
        allowBlockingTextureLoad: Boolean,
    ) {
        if (cachedAtlasFilter == null || cachedAtlasColor != block.color) {
            cachedAtlasColor = block.color
            cachedAtlasFilter = PorterDuffColorFilter(block.color, PorterDuff.Mode.SRC_IN)
        }

        atlasPaint.color = block.color
        atlasPaint.colorFilter = cachedAtlasFilter

        val glyphs = block.glyphs
        val coords = block.dstCoords

        for (i in glyphs.indices) {
            val g = glyphs[i]
            val texture = if (allowBlockingTextureLoad) {
                block.bundle.androidTextureForGlyph(g)
            } else {
                block.bundle.cachedAndroidTextureForGlyph(g)
            } ?: continue

            if (g.w <= 0 || g.h <= 0 || g.x < 0 || g.y < 0 ||
                g.x + g.w > texture.width || g.y + g.h > texture.height
            ) {
                continue
            }

            val base = i * 4
            srcRect.set(g.x, g.y, g.x + g.w, g.y + g.h)
            dstRectF.set(
                coords[base],
                coords[base + 1] + block.topY,
                coords[base + 2],
                coords[base + 3] + block.topY,
            )
            canvas.drawBitmap(texture, srcRect, dstRectF, atlasPaint)
        }
    }
}

object VerseTemplateRenderer {
    private const val STANDARD_EXPORT_WIDTH = 1440
    private const val STANDARD_MAX_HEIGHT = 2400
    private const val PREVIEW_WIDTH = 720
    private const val PREVIEW_WIDTH_LOW_RAM = 480
    private const val PREVIEW_WIDTH_MIN = 280

    private val logoCacheLock = Any()
    private var cachedDarkLogo: Bitmap? = null
    private var cachedLightLogo: Bitmap? = null

    fun clearLogoCache() {
        synchronized(logoCacheLock) {
            cachedDarkLogo = null
            cachedLightLogo = null
        }
    }

    private fun getLogoBitmap(context: Context, variant: LogoVariant): Bitmap? {
        synchronized(logoCacheLock) {
            val existing = if (variant == LogoVariant.LIGHT) cachedLightLogo else cachedDarkLogo
            if (existing != null && !existing.isRecycled) {
                return existing
            }

            val resId = if (variant == LogoVariant.LIGHT) {
                R.drawable.logo_light
            } else {
                R.drawable.logo_dark
            }

            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = BitmapFactory.decodeResource(context.resources, resId, opts)
                ?: return null

            if (variant == LogoVariant.LIGHT) {
                cachedLightLogo = decoded
            } else {
                cachedDarkLogo = decoded
            }

            return decoded
        }
    }

    fun scriptFactorsFor(scriptCode: String): ScriptFactors = when (scriptCode) {
        QuranScriptUtils.SCRIPT_UTHMANI -> ScriptFactors(1f, 1f)
        QuranScriptUtils.SCRIPT_DK_INDOPAK,
        QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK -> ScriptFactors(1f, 1.06f)
        QuranScriptUtils.SCRIPT_KFQPC_V1 -> ScriptFactors(30f / 28f, 1f)
        QuranScriptUtils.SCRIPT_KFQPC_V2,
        QuranScriptUtils.SCRIPT_KFQPC_V4 -> ScriptFactors(22f / 28f, 1.22f)
        else -> ScriptFactors(1f, 1f)
    }

    fun cleanTranslationText(translation: Translation?): String {
        if (translation == null || translation.text.isBlank()) return ""

        val parts = parseTranslationText(translation.text, translation.bookSlug)
        val sb = StringBuilder(translation.text.length)

        for (part in parts) {
            when (part) {
                is RichTextPart.Plain -> sb.append(part.text)
                is RichTextPart.QuranRef -> Unit
                is RichTextPart.FootnoteRef -> Unit
            }
        }

        return sb.toString().replace(Regex("[ \\t]{2,}"), " ").trim()
    }

    fun isLowRamDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.isLowRamDevice == true
    }

    fun previewWidthCap(context: Context): Int {
        return if (isLowRamDevice(context)) PREVIEW_WIDTH_LOW_RAM else PREVIEW_WIDTH
    }

    fun computeExportDimensions(
        layout: VerseTemplateLayout,
    ): Pair<Int, Int> {
        return computeCanvasDimensions(layout, STANDARD_EXPORT_WIDTH, STANDARD_MAX_HEIGHT)
    }

    fun computePreviewDimensions(
        layout: VerseTemplateLayout,
        targetWidthPx: Int,
    ): Pair<Int, Int> {
        val width = targetWidthPx.coerceIn(PREVIEW_WIDTH_MIN, PREVIEW_WIDTH)
        val maxHeight = (width / 0.25f).roundToInt()
        return computeCanvasDimensions(layout, width, maxHeight)
    }

    private fun computeCanvasDimensions(
        layout: VerseTemplateLayout,
        baseWidth: Int,
        maxHeight: Int,
    ): Pair<Int, Int> {
        val aspect = layout.aspectRatio.coerceIn(0.25f, 4f)
        val rawHeight = (baseWidth / aspect).roundToInt().coerceAtLeast(1)

        return if (rawHeight <= maxHeight) {
            baseWidth to rawHeight
        } else {
            val scaledWidth = (maxHeight * aspect).roundToInt().coerceAtLeast(1)
            scaledWidth to maxHeight
        }
    }

    suspend fun prepareScene(
        context: Context,
        fontResolver: FontResolver,
        input: VerseRenderInput,
        canvasSize: Pair<Int, Int>? = null,
    ): PreparedVerseScene = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        val layout = input.template.layout
        val (canvasWidth, canvasHeight) = canvasSize ?: computeExportDimensions(layout)

        val arabicPlainText = if (input.showArabic && input.arabicWords.isNotEmpty()) {
            input.arabicWords.joinToString(" ") { it.text }.trim()
        } else {
            ""
        }

        val unitAtlas = if (
            input.showArabic &&
            input.arabicWords.isNotEmpty() &&
            input.scriptCode.isQuranAtlasScript() &&
            input.atlasBundle != null
        ) {
            buildUnitAtlasVerse(
                bundle = input.atlasBundle,
                words = input.arabicWords,
                placementsByWord = input.atlasPlacements,
            )
        } else {
            null
        }

        val hasArabic = input.showArabic && (unitAtlas != null || arabicPlainText.isNotEmpty())
        val trimmedTranslation = if (input.showTranslation) input.translationText.trim() else ""
        val hasTranslation = trimmedTranslation.isNotEmpty()
        val trimmedReference = if (input.showReference) input.referenceText.trim() else ""
        val hasReference = trimmedReference.isNotEmpty()

        val isArabicLightText = relativeLuminance(layout.arabic.color) > 0.5f

        val arabicTypeface = if (hasArabic && unitAtlas == null) {
            fontResolver.typeface(
                script = input.scriptCode,
                pageNo = input.pageNo,
                isDark = isArabicLightText,
            ) ?: Typeface.DEFAULT
        } else {
            null
        }

        val isUrduTranslation = hasTranslation && TranslUtils.isUrdu(input.translationSlug)
        val isRtlTranslation = hasTranslation && (
            StringUtils.isRtlLanguage(input.translationSlug) ||
                BidiFormatter.getInstance().isRtl(trimmedTranslation)
            )
        val translationTypeface = if (isUrduTranslation) {
            appContext.getFont(R.font.noto_nastaliq_urdu_regular) ?: Typeface.DEFAULT
        } else {
            Typeface.DEFAULT
        }

        val isRtlReference = hasReference && BidiFormatter.getInstance().isRtl(trimmedReference)

        val arabicPaint = newTextPaint(arabicTypeface ?: Typeface.DEFAULT, layout.arabic.color)
        val translationPaint = newTextPaint(translationTypeface, layout.translation.color)
        val referencePaint = newTextPaint(Typeface.DEFAULT, layout.reference.color).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val arabicStaticAlign = resolveStaticAlignment(layout.alignment, isRtlParagraph = true)
        val translationStaticAlign = resolveStaticAlignment(layout.alignment, isRtlParagraph = isRtlTranslation)
        val referenceFlowAlign = resolveStaticAlignment(layout.alignment, isRtlParagraph = isRtlReference)

        val flowMeasurer = VerseBlockMeasurer { kind, fontSizePx, lineHeightMult, maxWidthPx ->
            when (kind) {
                FlowBlockKind.ARABIC -> {
                    if (unitAtlas != null) {
                        measureUnitAtlasHeight(
                            atlas = unitAtlas,
                            fontSizePx = fontSizePx,
                            lineHeightMultiplier = lineHeightMult,
                            maxWidthPx = maxWidthPx,
                        )
                    } else {
                        buildStaticTextLayout(
                            text = arabicPlainText,
                            paint = arabicPaint,
                            fontSizePx = fontSizePx,
                            lineHeightMultiplier = lineHeightMult,
                            widthPx = maxWidthPx.roundToInt().coerceAtLeast(1),
                            alignment = arabicStaticAlign,
                            isRtl = true,
                            isUrdu = false,
                        ).height.toFloat()
                    }
                }

                FlowBlockKind.TRANSLATION -> {
                    buildStaticTextLayout(
                        text = trimmedTranslation,
                        paint = translationPaint,
                        fontSizePx = fontSizePx,
                        lineHeightMultiplier = lineHeightMult,
                        widthPx = maxWidthPx.roundToInt().coerceAtLeast(1),
                        alignment = translationStaticAlign,
                        isRtl = isRtlTranslation,
                        isUrdu = isUrduTranslation,
                    ).height.toFloat()
                }

                FlowBlockKind.REFERENCE -> {
                    buildStaticTextLayout(
                        text = trimmedReference,
                        paint = referencePaint,
                        fontSizePx = fontSizePx,
                        lineHeightMultiplier = lineHeightMult,
                        widthPx = maxWidthPx.roundToInt().coerceAtLeast(1),
                        alignment = referenceFlowAlign,
                        isRtl = isRtlReference,
                        isUrdu = false,
                    ).height.toFloat()
                }
            }
        }

        val freeRefEps = max(0.5f, canvasHeight * 0.0005f)
        val freeRefMeasurer = FreeReferenceMeasurer { fontSizePx, lineHeightMult, boxWidthPx, boxHeightPx ->
            val sl = buildStaticTextLayout(
                text = trimmedReference,
                paint = referencePaint,
                fontSizePx = fontSizePx,
                lineHeightMultiplier = lineHeightMult,
                widthPx = boxWidthPx.roundToInt().coerceAtLeast(1),
                alignment = Layout.Alignment.ALIGN_CENTER,
                isRtl = isRtlReference,
                isUrdu = false,
            )
            sl.height <= boxHeightPx + freeRefEps
        }

        val fitResult = VerseTemplateFitSolver.solve(
            layout = layout,
            canvasWidth = canvasWidth.toFloat(),
            canvasHeight = canvasHeight.toFloat(),
            hasArabic = hasArabic,
            hasTranslation = hasTranslation,
            hasReference = hasReference,
            scriptFactors = scriptFactorsFor(input.scriptCode),
            flowMeasurer = flowMeasurer,
            freeRefMeasurer = freeRefMeasurer,
        )

        val columnStartY = when {
            fitResult.stage == VerseFitStage.REJECTED -> fitResult.safeTopPx
            layout.verticalAlignment == TemplateVerticalAlignment.TOP -> fitResult.safeTopPx
            layout.verticalAlignment == TemplateVerticalAlignment.BOTTOM -> {
                fitResult.safeTopPx + fitResult.safeHeightPx - fitResult.totalFlowHeightPx
            }
            else -> {
                fitResult.safeTopPx + (fitResult.safeHeightPx - fitResult.totalFlowHeightPx) * 0.5f
            }
        }

        val preparedFlow = ArrayList<PreparedFlowBlock>(fitResult.flowBlocks.size)
        var cursorY = columnStartY
        val safeWidthInt = fitResult.safeWidthPx.roundToInt().coerceAtLeast(1)

        for (solved in fitResult.flowBlocks) {
            cursorY += solved.gapBeforePx

            val prepared: PreparedFlowBlock = when (solved.kind) {
                FlowBlockKind.ARABIC -> {
                    if (unitAtlas != null) {
                        buildPreparedAtlasBlock(
                            atlas = unitAtlas,
                            topY = cursorY,
                            safeLeftPx = fitResult.safeLeftPx,
                            safeWidthPx = fitResult.safeWidthPx,
                            alignment = layout.alignment,
                            fontSizePx = solved.fontSizePx,
                            lineHeightMultiplier = solved.lineHeightMultiplier,
                            color = layout.arabic.color,
                        )
                    } else {
                        val sl = buildStaticTextLayout(
                            text = arabicPlainText,
                            paint = newTextPaint(arabicTypeface ?: Typeface.DEFAULT, layout.arabic.color),
                            fontSizePx = solved.fontSizePx,
                            lineHeightMultiplier = solved.lineHeightMultiplier,
                            widthPx = safeWidthInt,
                            alignment = arabicStaticAlign,
                            isRtl = true,
                            isUrdu = false,
                        )
                        PreparedFlowBlock.StaticText(
                            topY = cursorY,
                            leftX = fitResult.safeLeftPx,
                            staticLayout = sl,
                        )
                    }
                }

                FlowBlockKind.TRANSLATION -> {
                    val sl = buildStaticTextLayout(
                        text = trimmedTranslation,
                        paint = newTextPaint(translationTypeface, layout.translation.color),
                        fontSizePx = solved.fontSizePx,
                        lineHeightMultiplier = solved.lineHeightMultiplier,
                        widthPx = safeWidthInt,
                        alignment = translationStaticAlign,
                        isRtl = isRtlTranslation,
                        isUrdu = isUrduTranslation,
                    )
                    PreparedFlowBlock.StaticText(
                        topY = cursorY,
                        leftX = fitResult.safeLeftPx,
                        staticLayout = sl,
                    )
                }

                FlowBlockKind.REFERENCE -> {
                    val sl = buildStaticTextLayout(
                        text = trimmedReference,
                        paint = newTextPaint(Typeface.DEFAULT, layout.reference.color),
                        fontSizePx = solved.fontSizePx,
                        lineHeightMultiplier = solved.lineHeightMultiplier,
                        widthPx = safeWidthInt,
                        alignment = referenceFlowAlign,
                        isRtl = isRtlReference,
                        isUrdu = false,
                    )
                    PreparedFlowBlock.StaticText(
                        topY = cursorY,
                        leftX = fitResult.safeLeftPx,
                        staticLayout = sl,
                    )
                }
            }

            preparedFlow.add(prepared)
            cursorY += solved.measuredHeightPx
        }

        val preparedFreeRef = fitResult.freeReference?.let { free ->
            val sl = buildStaticTextLayout(
                text = trimmedReference,
                paint = newTextPaint(Typeface.DEFAULT, layout.reference.color),
                fontSizePx = free.fontSizePx,
                lineHeightMultiplier = free.lineHeightMultiplier,
                widthPx = free.boxWidthPx.roundToInt().coerceAtLeast(1),
                alignment = Layout.Alignment.ALIGN_CENTER,
                isRtl = isRtlReference,
                isUrdu = false,
            )
            val centeredTop = free.boxTopPx + (free.boxHeightPx - sl.height) * 0.5f
            PreparedFreeRef(
                leftX = free.boxLeftPx,
                topY = centeredTop,
                staticLayout = sl,
            )
        }

        val preparedLogo = if (input.showLogo && layout.logo.opacity > 0f) {
            buildPreparedLogo(
                context = appContext,
                logo = layout.logo,
                canvasWidth = canvasWidth.toFloat(),
                canvasHeight = canvasHeight.toFloat(),
            )
        } else {
            null
        }

        val fallbackBg = if (isArabicLightText) {
            0xFF142024.toInt()
        } else {
            0xFFF2EFE9.toInt()
        }

        PreparedVerseScene(
            templateId = input.template.id,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            fitResult = fitResult,
            fallbackBgColor = fallbackBg,
            flowBlocks = preparedFlow,
            freeRef = preparedFreeRef,
            logo = preparedLogo,
        )
    }

    suspend fun renderHighQualityBitmap(
        context: Context,
        scene: PreparedVerseScene,
        template: VerseTemplate,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val bgBitmap = VerseTemplateManager.decodeExportBitmap(
            context = context,
            template = template,
            reqWidth = scene.canvasWidth,
            reqHeight = scene.canvasHeight,
        ) ?: return@withContext null

        try {
            val outBitmap = createBitmap(
                scene.canvasWidth,
                scene.canvasHeight,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(outBitmap)
            scene.draw(
                canvas = canvas,
                bgBitmap = bgBitmap,
                allowBlockingTextureLoad = true,
            )
            outBitmap
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            if (!bgBitmap.isRecycled) {
                bgBitmap.recycle()
            }
        }
    }

    private fun newTextPaint(typeface: Typeface, color: Int): TextPaint {
        return TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG
        ).apply {
            this.typeface = typeface
            this.color = color
        }
    }

    private fun resolveStaticAlignment(
        alignment: TemplateHorizontalAlignment,
        isRtlParagraph: Boolean,
    ): Layout.Alignment = when (alignment) {
        TemplateHorizontalAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        TemplateHorizontalAlignment.START -> {
            if (isRtlParagraph) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        }
        TemplateHorizontalAlignment.END -> {
            if (isRtlParagraph) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
        }
    }

    private fun buildStaticTextLayout(
        text: String,
        paint: TextPaint,
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        widthPx: Int,
        alignment: Layout.Alignment,
        isRtl: Boolean,
        isUrdu: Boolean,
    ): StaticLayout {
        paint.textSize = max(1f, fontSizePx)

        val fm = paint.fontMetrics
        val naturalHeight = (fm.descent - fm.ascent).coerceAtLeast(1f)
        val targetStep = paint.textSize * lineHeightMultiplier
        val minAllowedStep = if (isUrdu) {
            naturalHeight * 0.78f
        } else {
            paint.textSize * 1.02f
        }
        val effectiveStep = max(targetStep, minAllowedStep)
        val spacingAdd = effectiveStep - naturalHeight

        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, max(1, widthPx))
            .setAlignment(alignment)
            .setTextDirection(if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setLineSpacing(spacingAdd, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun buildUnitAtlasVerse(
        bundle: QuranAtlasBundle,
        words: List<AyahWordEntity>,
        placementsByWord: Map<Int, List<AtlasGlyphPlacement>>,
    ): UnitAtlasVerse? {
        val unitsPerEm = bundle.meta.font.unitsPerEm
        val ppem = bundle.layer.ppem
        if (unitsPerEm <= 0 || ppem <= 0) return null

        val unitFontScale = 1f / unitsPerEm.toFloat()
        val unitGlyphScale = 1f / ppem.toFloat()

        val ascenderFu = bundle.meta.font.ascenderFu
        val descenderFu = bundle.meta.font.descenderFu
        val unitAscent = if (ascenderFu > 0) ascenderFu * unitFontScale else 0.8f
        val unitDescent = if (descenderFu < 0) -descenderFu * unitFontScale else 0.2f

        val unitWords = ArrayList<UnitAtlasWord>(words.size)

        for (word in words) {
            val placements = placementsByWord.getForWord(word).orEmpty()
            val glyphs = ArrayList<UnitAtlasGlyph>(placements.size)
            var currentX = 0f

            for (p in placements) {
                val glyph = bundle.layer.glyphs[p.gid.toString()]
                if (glyph != null) {
                    val gx = (currentX + p.xOffsetFu * unitFontScale + glyph.bearingX * unitGlyphScale).toFloat()
                    val gyRel = (-p.yOffsetFu * unitFontScale - glyph.bearingY * unitGlyphScale).toFloat()
                    val gw = glyph.w * unitGlyphScale
                    val gh = glyph.h * unitGlyphScale
                    glyphs.add(UnitAtlasGlyph(gx, gyRel, gw, gh, glyph))
                }
                currentX += (p.xAdvanceFu * unitFontScale).toFloat()
            }

            val unitW = if (glyphs.isEmpty()) {
                max(0.35f * word.text.length.coerceAtLeast(1), 0.4f)
            } else {
                currentX
            }

            var minRelY = -unitAscent
            var maxRelY = unitDescent

            if (glyphs.isNotEmpty()) {
                minRelY = Float.POSITIVE_INFINITY
                maxRelY = Float.NEGATIVE_INFINITY
                for (g in glyphs) {
                    if (g.unitYRelBaseline < minRelY) minRelY = g.unitYRelBaseline
                    if (g.unitYRelBaseline + g.unitH > maxRelY) maxRelY = g.unitYRelBaseline + g.unitH
                }
            }

            unitWords.add(
                UnitAtlasWord(
                    unitWidth = unitW,
                    unitMinRelY = minRelY,
                    unitMaxRelY = maxRelY,
                    glyphs = glyphs,
                )
            )
        }

        if (unitWords.isEmpty()) return null

        return UnitAtlasVerse(
            bundle = bundle,
            unitAscent = unitAscent,
            unitDescent = unitDescent,
            words = unitWords,
        )
    }

    private fun measureUnitAtlasHeight(
        atlas: UnitAtlasVerse,
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        maxWidthPx: Float,
    ): Float {
        val words = atlas.words
        if (words.isEmpty()) return 0f

        val fs = max(1f, fontSizePx)
        val wordGapPx = fs * ATLAS_WORD_GAP_FRACTION

        var lineCount = 0
        var curW = 0f
        var curCount = 0
        var curMinRel = Float.POSITIVE_INFINITY
        var curMaxRel = Float.NEGATIVE_INFINITY
        var firstLineMinRel = 0f
        var lastLineMaxRel = 0f

        for (i in words.indices) {
            val w = words[i]
            val wordW = w.unitWidth * fs
            val extra = if (curCount == 0) 0f else wordGapPx

            if (curCount > 0 && curW + extra + wordW > maxWidthPx) {
                if (lineCount == 0) firstLineMinRel = curMinRel
                lineCount++
                curW = 0f
                curCount = 0
                curMinRel = Float.POSITIVE_INFINITY
                curMaxRel = Float.NEGATIVE_INFINITY
            }

            curW = if (curCount == 0) wordW else curW + extra + wordW
            curCount++
            if (w.unitMinRelY < curMinRel) curMinRel = w.unitMinRelY
            if (w.unitMaxRelY > curMaxRel) curMaxRel = w.unitMaxRelY
        }

        if (curCount > 0) {
            if (lineCount == 0) firstLineMinRel = curMinRel
            lastLineMaxRel = curMaxRel
            lineCount++
        }

        if (lineCount == 0) return 0f

        val lineHeightPx = fs * lineHeightMultiplier
        val typographicH = (atlas.unitAscent + atlas.unitDescent) * fs
        val halfLeading = (lineHeightPx - typographicH) * 0.5f
        val firstBaseline = halfLeading + atlas.unitAscent * fs
        val lastBaseline = firstBaseline + (lineCount - 1) * lineHeightPx
        val nominalTotal = lineCount * lineHeightPx

        val topOverhang = max(0f, -(firstBaseline + firstLineMinRel * fs))
        val bottomOverhang = max(0f, (lastBaseline + lastLineMaxRel * fs) - nominalTotal)

        return nominalTotal + topOverhang + bottomOverhang
    }

    private fun buildPreparedAtlasBlock(
        atlas: UnitAtlasVerse,
        topY: Float,
        safeLeftPx: Float,
        safeWidthPx: Float,
        alignment: TemplateHorizontalAlignment,
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        color: Int,
    ): PreparedFlowBlock.AtlasArabic {
        val fs = max(1f, fontSizePx)
        val wordGapPx = fs * ATLAS_WORD_GAP_FRACTION

        val lines = ArrayList<ArrayList<UnitAtlasWord>>()
        var curLine = ArrayList<UnitAtlasWord>()
        var curW = 0f

        for (w in atlas.words) {
            val wordW = w.unitWidth * fs
            val extra = if (curLine.isEmpty()) 0f else wordGapPx

            if (curLine.isNotEmpty() && curW + extra + wordW > safeWidthPx) {
                lines.add(curLine)
                curLine = ArrayList()
                curW = 0f
            }

            curLine.add(w)
            curW = if (curLine.size == 1) wordW else curW + extra + wordW
        }
        if (curLine.isNotEmpty()) {
            lines.add(curLine)
        }

        val lineHeightPx = fs * lineHeightMultiplier
        val typographicH = (atlas.unitAscent + atlas.unitDescent) * fs
        val halfLeading = (lineHeightPx - typographicH) * 0.5f
        val baseFirstBaseline = halfLeading + atlas.unitAscent * fs

        val firstLineMinRel = lines.firstOrNull()?.minOfOrNull { it.unitMinRelY } ?: 0f
        val topOverhang = max(0f, -(baseFirstBaseline + firstLineMinRel * fs))
        val firstBaseline = baseFirstBaseline + topOverhang

        var totalGlyphCount = 0
        for (line in lines) {
            for (w in line) {
                totalGlyphCount += w.glyphs.size
            }
        }

        val glyphList = ArrayList<AtlasGlyphJson>(totalGlyphCount)
        val coords = FloatArray(totalGlyphCount * 4)
        var glyphIdx = 0

        for ((lineIdx, line) in lines.withIndex()) {
            var lineContentW = 0f
            for (w in line) {
                lineContentW += w.unitWidth * fs
            }
            if (line.size > 1) {
                lineContentW += wordGapPx * (line.size - 1)
            }

            val lineStartX = when (alignment) {
                TemplateHorizontalAlignment.START -> safeLeftPx
                TemplateHorizontalAlignment.END -> safeLeftPx + safeWidthPx - lineContentW
                TemplateHorizontalAlignment.CENTER -> safeLeftPx + (safeWidthPx - lineContentW) * 0.5f
            }

            val baselineY = firstBaseline + lineIdx * lineHeightPx
            var cursorRightX = lineStartX + lineContentW

            for ((wordIdx, w) in line.withIndex()) {
                val wordW = w.unitWidth * fs
                val wordLeftX = cursorRightX - wordW

                for (ug in w.glyphs) {
                    val dstLeft = wordLeftX + ug.unitX * fs
                    val dstTop = baselineY + ug.unitYRelBaseline * fs
                    val dstW = max(1f, ug.unitW * fs)
                    val dstH = max(1f, ug.unitH * fs)

                    glyphList.add(ug.glyph)
                    val base = glyphIdx * 4
                    coords[base] = dstLeft
                    coords[base + 1] = dstTop
                    coords[base + 2] = dstLeft + dstW
                    coords[base + 3] = dstTop + dstH
                    glyphIdx++
                }

                cursorRightX = wordLeftX - if (wordIdx < line.lastIndex) wordGapPx else 0f
            }
        }

        return PreparedFlowBlock.AtlasArabic(
            topY = topY,
            bundle = atlas.bundle,
            color = color,
            glyphs = glyphList.toTypedArray(),
            dstCoords = coords,
        )
    }

    private fun buildPreparedLogo(
        context: Context,
        logo: LogoBlockStyle,
        canvasWidth: Float,
        canvasHeight: Float,
    ): PreparedLogo? {
        val logoBitmap = getLogoBitmap(context, logo.variant) ?: return null
        if (logoBitmap.width <= 0 || logoBitmap.height <= 0) return null

        val alphaInt = (logo.opacity.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        if (alphaInt <= 0) return null

        val targetLogoWidth = (logo.maximumWidth * canvasWidth).coerceAtLeast(1f)
        val logoAspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
        val targetLogoHeight = targetLogoWidth / logoAspect

        val logoLeft = when {
            logo.anchor.isHorizontalCenter -> (canvasWidth - targetLogoWidth) * 0.5f
            logo.anchor.isLeft -> logo.insetX * canvasWidth
            else -> canvasWidth - logo.insetX * canvasWidth - targetLogoWidth
        }

        val logoTop = if (logo.anchor.isTop) {
            logo.insetY * canvasHeight
        } else {
            canvasHeight - logo.insetY * canvasHeight - targetLogoHeight
        }

        return PreparedLogo(
            bitmap = logoBitmap,
            left = logoLeft,
            top = logoTop,
            right = logoLeft + targetLogoWidth,
            bottom = logoTop + targetLogoHeight,
            alpha = alphaInt,
        )
    }

    private fun relativeLuminance(argb: Int): Float {
        val r = Color.red(argb) / 255f
        val g = Color.green(argb) / 255f
        val b = Color.blue(argb) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}

internal fun computeCenterCropSrcRect(
    srcWidth: Int,
    srcHeight: Int,
    dstWidth: Int,
    dstHeight: Int,
    outRect: Rect,
) {
    if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
        outRect.set(0, 0, max(1, srcWidth), max(1, srcHeight))
        return
    }

    val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
    val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()

    if (srcAspect > dstAspect) {
        val cropWidth = (srcHeight * dstAspect).roundToInt().coerceIn(1, srcWidth)
        val left = (srcWidth - cropWidth) / 2
        outRect.set(left, 0, left + cropWidth, srcHeight)
    } else {
        val cropHeight = (srcWidth / dstAspect).roundToInt().coerceIn(1, srcHeight)
        val top = (srcHeight - cropHeight) / 2
        outRect.set(0, top, srcWidth, top + cropHeight)
    }
}
