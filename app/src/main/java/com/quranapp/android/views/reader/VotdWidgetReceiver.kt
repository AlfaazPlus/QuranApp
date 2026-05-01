package com.quranapp.android.views.reader

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.extensions.sp2px
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeMediumRes
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.utils.verse.VerseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private data class VotdWidgetUiState(
    val verseInfo: String,
    val backgroundBitmap: Bitmap,
    val arabicTextBitmap: Bitmap?,
    val translationBitmap: Bitmap,
    val openReaderIntent: Intent,
    val headerHeightDp: Float,
    val footerHeightDp: Float,
    val textPaddingDp: Float,
    val textVerticalSpacingDp: Float,
    val arabicHeightDp: Float,
    val translationHeightDp: Float,
)

class VotdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VotdGlanceWidget()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        val manager = GlanceAppWidgetManager(context)
        val widget = VotdGlanceWidget()
        val glanceId = manager.getGlanceIdBy(appWidgetId)

        CoroutineScope(Dispatchers.Default).launch {
            widget.update(context, glanceId)
        }
    }
}

private class VotdGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            val sizes = LocalSize.current

            val state by produceState<VotdWidgetUiState?>(null, sizes) {
                value = buildVotdWidgetState(
                    context,
                    id,
                    sizes.width.value,
                    sizes.height.value,
                )
            }

            VotdGlanceContent(context, state = state)
        }
    }
}


@androidx.compose.runtime.Composable
private fun VotdGlanceContent(context: Context, state: VotdWidgetUiState?) {
    if (state == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black))
                .cornerRadius(16.dp)
        ) {
            Text(
                text = context.getString(R.string.strTitleVOTD),
                modifier = GlanceModifier.padding(12.dp),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        return
    }

    Box(
        modifier = GlanceModifier
            .clickable(onClick = actionStartActivity(state.openReaderIntent))
            .cornerRadius(16.dp)
    ) {
        Image(
            provider = ImageProvider(state.backgroundBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)
        )

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(state.headerHeightDp.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.strTitleVOTD),
                    modifier = GlanceModifier
                        .background(Color.White.alpha(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .cornerRadius(99.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp
                    ),
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(R.drawable.dr_logo),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
            }

            Column(
                GlanceModifier
                    .defaultWeight()
                    .padding(state.textPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.arabicTextBitmap?.let { arabicBitmap ->
                    Image(
                        provider = ImageProvider(arabicBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxWidth()
                            .height(state.arabicHeightDp.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = GlanceModifier.height(state.textVerticalSpacingDp.dp))
                }

                Image(
                    provider = ImageProvider(state.translationBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth()
                        .height(state.translationHeightDp.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.alpha(0.15f))
            ) {}

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height((state.footerHeightDp - 1).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.verseInfo,
                    modifier = GlanceModifier
                        .fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(Color.White.alpha(0.75f)),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

fun updateAllVotdWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val widget = VotdGlanceWidget()

    CoroutineScope(Dispatchers.Default).launch {
        val glanceIds = manager.getGlanceIds(widget.javaClass)

        glanceIds.forEach { glanceId ->
            widget.update(context, glanceId)
        }
    }
}

private val votdWidgetPrefsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

@OptIn(FlowPreview::class)
fun startVotdWidgetPreferenceObserver(context: Context) {
    val app = context.applicationContext

    votdWidgetPrefsScope.launch {
        combine(
            DataStoreManager.flowMultiple(
                ReaderPreferences.KEY_SCRIPT,
                ReaderPreferences.KEY_SCRIPT_VARIANT,
                ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
                ReaderPreferences.KEY_TRANSLATIONS,
                ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
            ),
            ThemeUtils.widgetAppearancePreferencesFlow(),
            VersePreferences.votdStorageFlow(),
        ) { prefs, theme, votd ->
            "${prefs.toKey()}|${theme.first}|${theme.second}|${theme.third}|${votd.first}|${votd.second}|${votd.third}"
        }
            .distinctUntilChanged()
            .drop(1)
            .debounce(250)
            .collect {
                updateAllVotdWidgets(app)
            }
    }
}

private suspend fun buildVotdWidgetState(
    context: Context,
    glanceId: GlanceId,
    widgetWidthDp: Float,
    widgetHeightDp: Float,
): VotdWidgetUiState? {
    val repository = DatabaseProvider.getQuranRepository(context)
    val vwd = VerseUtils.getVOTD(context, repository) ?: return null

    if (
        !QuranMeta.isChapterValid(vwd.chapterNo) ||
        !repository.isVerseValid4Chapter(vwd.chapterNo, vwd.verseNo)
    ) {
        return null
    }

    val showArabic = widgetHeightDp >= 203f
    val hasArabic = showArabic && ReaderPreferences.getArabicTextEnabled()

    val headerHeightDp = 42f
    val footerHeightDp = 32f
    val textVerticalSpacingDp = if (hasArabic) 8f else 0f
    val textPaddingDp = 12f

    val contentHeightDp =
        widgetHeightDp - (headerHeightDp + footerHeightDp + textVerticalSpacingDp + textPaddingDp * 2)

    val textMaxWidthPx = context.dp2px(widgetWidthDp - textPaddingDp * 2)

    val arabicRatio = when {
        !hasArabic -> 0f
        else -> 0.45f
    }

    val translationRatio = 1f - arabicRatio

    val arabicHeightDp = contentHeightDp * arabicRatio
    val translationHeightDp = contentHeightDp * translationRatio

    val arabicHeightPx = context.dp2px(arabicHeightDp)
    val translationHeightPx = context.dp2px(translationHeightDp)

    val arabicBitmap = if (hasArabic) {
        prepareArabicTextBitmap(
            context = context,
            vwd = vwd,
            maxWidth = textMaxWidthPx,
            maxHeight = arabicHeightPx,
        )
    } else null

    val chapterNo = vwd.chapterNo
    val verseNo = vwd.verseNo

    val translation = QuranTranslationFactory(context).use { factory ->
        val bookInfo = factory.getTranslationBookInfo(VerseUtils.obtainOptimalSlugForVotd())
        factory.getTranslationsSingleSlugVerse(bookInfo.slug, chapterNo, verseNo)
    }

    val translationText = translation.text

    val translationBitmap = createTextBitmap(
        context = context,
        text = StringUtils.removeHTML(translationText, false),
        typeface = if (translation.isUrdu) context.getFont(R.font.noto_nastaliq_urdu_regular) else null,
        textSize = context.sp2px(20f),
        color = Color.White.toArgb(),
        targetMaxWidth = textMaxWidthPx,
        targetMaxHeight = translationHeightPx
    )

    val openIntent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo).apply {
        setClass(context, ActivityReader::class.java)
    }

    return VotdWidgetUiState(
        verseInfo = context.getString(
            R.string.strLabelVerseWithChapNameAndNo,
            repository.getChapterName(chapterNo),
            chapterNo,
            verseNo
        ),
        backgroundBitmap = createWidgetBackgroundBitmap(
            context,
            context.dp2px(widgetWidthDp),
            context.dp2px(widgetHeightDp),
        ),
        arabicTextBitmap = arabicBitmap,
        translationBitmap = translationBitmap,
        openReaderIntent = openIntent,
        headerHeightDp = headerHeightDp,
        footerHeightDp = footerHeightDp,
        textPaddingDp = textPaddingDp,
        textVerticalSpacingDp = textVerticalSpacingDp,
        arabicHeightDp = arabicHeightDp,
        translationHeightDp = translationHeightDp,
    )
}


internal fun prepareArabicTextBitmap(
    context: Context,
    vwd: VerseWithDetails,
    maxWidth: Int,
    maxHeight: Int,
): Bitmap? {
    val pageNo = vwd.pageNo

    val quranScript = ReaderPreferences.getQuranScript()
    val sizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier()
    val txtSizePx =
        (context.getDimenPx(quranScript.getQuranScriptVerseTextSizeMediumRes()) * sizeMultiplier)
            .toInt()

    // VOTD is always dark background, so we use the "dark" variant of KFQPC if available
    var fontQuranText = FontResolver.getInstance(context)
        .typeface(quranScript, pageNo, true)

    if (fontQuranText == null) {
        return null
    }

    val bitmap = createTextBitmap(
        context,
        vwd.words.joinToString(" ") { it.text },
        fontQuranText,
        txtSizePx,
        Color.White.toArgb(),
        maxWidth,
        maxHeight
    )

    return bitmap
}

internal fun createTextBitmap(
    context: Context,
    text: String,
    typeface: Typeface?,
    textSize: Int,
    color: Int,
    targetMaxWidth: Int,
    targetMaxHeight: Int,
): Bitmap {
    if (targetMaxWidth <= 0 || targetMaxHeight <= 0) {
        return createBitmap(1, 1)
    }

    val minTextSizePx = context.sp2px(12f)
    val maxTextSizePx = textSize.coerceAtLeast(minTextSizePx)

    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.typeface = typeface
    }

    fun buildLayout(sizePx: Int): StaticLayout {
        paint.textSize = sizePx.toFloat()

        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, targetMaxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()
    }

    var bestLayout = buildLayout(minTextSizePx)
    var low = minTextSizePx
    var high = maxTextSizePx

    while (low <= high) {
        val mid = (low + high) / 2
        val candidate = buildLayout(mid)

        if (candidate.height <= targetMaxHeight) {
            bestLayout = candidate
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    val height = bestLayout.height
    if (height <= 0) return createBitmap(1, 1)

    val bitmap = createBitmap(targetMaxWidth, height)
    val canvas = Canvas(bitmap)

    canvas.withSave {
        bestLayout.draw(this)
    }

    return bitmap
}


internal fun createWidgetBackgroundBitmap(
    context: Context,
    width: Int,
    height: Int,
): Bitmap {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(context.dp2px(160f))

    val colorScheme = ThemeUtils.colorSchemeFromPreferences(context, true)

    val bitmap = createBitmap(safeWidth, safeHeight)
    val canvas = Canvas(bitmap)

    val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            safeWidth.toFloat(),
            safeHeight.toFloat(),
            intArrayOf(
                Color.Black.toArgb(),
                colorScheme.primary.toArgb(),
                Color.Black.toArgb()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Black.alpha(0.8f).toArgb()
    }

    val cornerRadius = context.dp2px(12f).toFloat()
    val rect = RectF(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gradientPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, overlayPaint)

    return bitmap
}
