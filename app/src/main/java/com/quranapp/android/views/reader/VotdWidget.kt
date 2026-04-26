package com.quranapp.android.views.reader

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.extensions.sp2px
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeWidgetSP
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.reader.toKFQPCFontFilenameOld
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.utils.verse.VerseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class VotdWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        for (appWidgetId in newWidgetIds!!) {
            updateAppWidget(context!!, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context, VotdWidget::class.java
            )
        )

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

fun updateAllVotdWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(
            context, VotdWidget::class.java
        )
    )

    for (appWidgetId in appWidgetIds) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
}

internal fun updateAppWidget(
    context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int
) {
    CoroutineScope(Dispatchers.Default).launch {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val maxWidth =
            context.dp2px(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat())
                .takeIf { it > 0 } ?: context.dp2px(300f)


        val repository = DatabaseProvider.getQuranRepository(context)

        val verse = VerseUtils.getVOTD(context, repository) ?: return@launch

        if (
            !QuranMeta.isChapterValid(verse.chapterNo) ||
            !repository.isVerseValid4Chapter(verse.chapterNo, verse.verseNo)
        ) {
            return@launch
        }

        if (ReaderPreferences.getArabicTextEnabled()) {
            onObtainVotd(
                context,
                appWidgetManager,
                appWidgetId,
                repository,
                verse,
                prepareArabicTextBitmap(
                    context,
                    verse,
                    maxWidth
                )
            )
        } else {
            onObtainVotd(
                context,
                appWidgetManager,
                appWidgetId,
                repository,
                verse,
                null,
            )
        }
    }
}

internal suspend fun onObtainVotd(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    repository: QuranRepository,
    vwd: VerseWithDetails,
    arabicTextBitmap: Bitmap?
) {
    val chapterNo = vwd.chapterNo
    val verseNo = vwd.verseNo
    val views = RemoteViews(context.packageName, R.layout.votd_widget)

    QuranTranslationFactory(context).use { factory ->
        val bookInfo = factory.getTranslationBookInfo(VerseUtils.obtainOptimalSlugForVotd())

        val translation = factory.getTranslationsSingleSlugVerse(bookInfo.slug, chapterNo, verseNo)


        views.setTextViewText(
            R.id.verseInfo, context.getString(
                R.string.strLabelVerseWithChapNameAndNo,
                repository.getChapterName(chapterNo),
                chapterNo,
                verseNo
            )
        )

        if (arabicTextBitmap != null) {
            views.setImageViewBitmap(R.id.votdArabicText, arabicTextBitmap)
        }

        views.setViewVisibility(
            R.id.votdArabicText,
            if (arabicTextBitmap != null) View.VISIBLE else View.GONE
        )

        // Remove footnote markers etc
        views.setTextViewText(
            R.id.votdText, StringUtils.removeHTML(translation.text, false)
        )

        val intent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo).apply {
            setClass(context, ActivityReader::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.votdWidgetContainer, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

internal fun prepareArabicTextBitmap(
    context: Context,
    vwd: VerseWithDetails,
    maxWidth: Int,
): Bitmap? {
    val pageNo = vwd.pageNo

    val quranScript = ReaderPreferences.getQuranScript()
    val txtSizeSp = quranScript.getQuranScriptVerseTextSizeWidgetSP()
    val verseTextSize = context.sp2px(txtSizeSp)
    var fontQuranText: Typeface?

    if (quranScript.isKFQPCScript()) {
        val fontsDir = FileUtils.newInstance(context).getKFQPCScriptFontDir(quranScript)
        val fontFile = File(fontsDir, pageNo.toKFQPCFontFilename(true))

        fontQuranText = if (fontFile.length() > 0L) {
            Typeface.createFromFile(fontFile)
        } else {
            Typeface.createFromFile(File(fontsDir, pageNo.toKFQPCFontFilenameOld()))
        }
    } else {
        fontQuranText = context.getFont(
            quranScript.getQuranScriptFontRes(true)
        )
    }

    if (fontQuranText == null) {
        return null
    }

    val bitmap = createTextBitmap(
        context,
        vwd.words.joinToString(" ") { it.text },
        fontQuranText,
        verseTextSize,
        Color.WHITE,
        maxWidth
    )

    return bitmap
}

internal fun createTextBitmap(
    context: Context,
    text: String,
    typeface: Typeface,
    textSize: Int,
    color: Int,
    maxWidth: Int,
): Bitmap {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    paint.color = color
    paint.textSize = textSize.toFloat()
    paint.typeface = typeface

    // Layout the text, wrap at maxWidth
    val staticLayout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, maxWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1f)
        .setIncludePad(false)
        .build()

    val height = staticLayout.height
    if (maxWidth <= 0 || height <= 0) {
        // return null or a 1x1 bitmap to avoid crash
        return createBitmap(1, 1)
    }

    // Create bitmap with calculated height
    val bitmap = createBitmap(maxWidth, staticLayout.height)
    val canvas = Canvas(bitmap)
    canvas.withSave {
        staticLayout.draw(this)
    }

    return bitmap
}
