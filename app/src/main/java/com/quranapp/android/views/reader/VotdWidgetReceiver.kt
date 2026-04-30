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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getDimenPx
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
import kotlinx.coroutines.withContext

private data class VotdWidgetUiState(
    val verseInfo: String,
    val translationText: String,
    val backgroundBitmap: Bitmap,
    val arabicTextBitmap: Bitmap?,
    val openReaderIntent: Intent,
)

class VotdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VotdGlanceWidget
}

private object VotdGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.Default) {
            buildVotdWidgetState(context, id)
        }

        provideContent {
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
                .cornerRadius(99.dp)
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
    ) {
        Image(
            provider = ImageProvider(state.backgroundBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)
        )

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    .defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.arabicTextBitmap?.let { arabicBitmap ->
                    Image(
                        provider = ImageProvider(arabicBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = state.translationText,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 5.dp),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(1.dp)
                    .background(Color.White.alpha(0.15f))
            ) {}

            Text(
                text = state.verseInfo,
                modifier = GlanceModifier.fillMaxWidth().padding(top = 10.dp),
                style = TextStyle(
                    color = ColorProvider(Color.White.alpha(0.75f)),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                ),
            )
        }
    }
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

internal fun createTextBitmap(
    context: Context,
    text: String,
    typeface: Typeface,
    textSize: Int,
    color: Int,
    maxWidth: Int,
    maxHeight: Int,
): Bitmap {
    if (maxWidth <= 0 || maxHeight <= 0) {
        // return a 1x1 bitmap to avoid crash
        return createBitmap(1, 1)
    }

    val horizontalPaddingPx = context.dp2px(20f) // 10.dp start + 10.dp end from parent Column
    val width = (maxWidth - horizontalPaddingPx).coerceAtLeast(1)

    val minTextSizePx = context.sp2px(14f)
    val maxTextSizePx = textSize.coerceAtLeast(minTextSizePx)
    val targetMaxHeight = (maxHeight * 0.42f).toInt().coerceAtLeast(context.dp2px(48f))

    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.typeface = typeface
    }

    fun buildLayout(sizePx: Int): StaticLayout {
        paint.textSize = sizePx.toFloat()

        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 0.9f)
            .setIncludePad(false)
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

    val staticLayout = bestLayout
    val height = staticLayout.height
    if (height <= 0) {
        return createBitmap(1, 1)
    }

    // Create bitmap with calculated height
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    canvas.withSave {
        staticLayout.draw(this)
    }

    return bitmap
}

fun updateAllVotdWidgets(context: Context) {
    CoroutineScope(Dispatchers.Default).launch {
        VotdGlanceWidget.updateAll(context)
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

private suspend fun buildVotdWidgetState(context: Context, glanceId: GlanceId): VotdWidgetUiState? {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

    val maxWidth =
        context.dp2px(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat())
            .takeIf { it > 0 } ?: context.dp2px(300f)
    val maxHeight =
        context.dp2px(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat())
            .takeIf { it > 0 } ?: context.dp2px(220f)

    val repository = DatabaseProvider.getQuranRepository(context)
    val verse = VerseUtils.getVOTD(context, repository) ?: return null

    if (
        !QuranMeta.isChapterValid(verse.chapterNo) ||
        !repository.isVerseValid4Chapter(verse.chapterNo, verse.verseNo)
    ) {
        return null
    }

    val arabicBitmap = if (ReaderPreferences.getArabicTextEnabled()) {
        prepareArabicTextBitmap(
            context = context,
            vwd = verse,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )
    } else null

    return buildVotdUiState(
        context = context,
        repository = repository,
        vwd = verse,
        arabicTextBitmap = arabicBitmap,
        widgetWidth = maxWidth,
        widgetHeight = maxHeight,
    )
}

private suspend fun buildVotdUiState(
    context: Context,
    repository: QuranRepository,
    vwd: VerseWithDetails,
    arabicTextBitmap: Bitmap?,
    widgetWidth: Int,
    widgetHeight: Int,
): VotdWidgetUiState {
    val chapterNo = vwd.chapterNo
    val verseNo = vwd.verseNo

    QuranTranslationFactory(context).use { factory ->
        val bookInfo = factory.getTranslationBookInfo(VerseUtils.obtainOptimalSlugForVotd())
        val translation = factory.getTranslationsSingleSlugVerse(bookInfo.slug, chapterNo, verseNo)
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
            translationText = StringUtils.removeHTML(translation.text, false),
            backgroundBitmap = createWidgetBackgroundBitmap(context, widgetWidth, widgetHeight),
            arabicTextBitmap = arabicTextBitmap,
            openReaderIntent = openIntent,
        )
    }
}
