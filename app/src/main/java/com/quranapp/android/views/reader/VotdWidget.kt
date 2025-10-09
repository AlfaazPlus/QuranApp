package com.quranapp.android.views.reader

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.quranapp.android.activities.MainActivity
import com.quranapp.android.components.quran.Quran
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.extensions.sp2px
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeWidgetSP
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.reader.toKFQPCFontFilenameOld
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.utils.verse.VerseUtils
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
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val maxWidth =
        context.dp2px(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat())

    QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
        override fun onReady(quranMeta: QuranMeta) {
            VerseUtils.getVOTD(context, quranMeta, null) { chapterNo, verseNo ->
                if (!QuranMeta.isChapterValid(chapterNo) || !quranMeta.isVerseValid4Chapter(
                        chapterNo, verseNo
                    )
                ) {
                    return@getVOTD
                }

                if (SPReader.getArabicTextEnabled(context)) {
                    Quran.prepareInstance(
                        context, quranMeta, object : OnResultReadyCallback<Quran> {
                            override fun onReady(quran: Quran) {
                                onObtainVotd(
                                    context,
                                    appWidgetManager,
                                    appWidgetId,
                                    quranMeta,
                                    chapterNo,
                                    verseNo,
                                    prepareArabicTextBitmap(
                                        context,
                                        quran.getVerse(chapterNo, verseNo),
                                        quran.script,
                                        maxWidth
                                    )
                                )
                            }
                        })
                } else {
                    onObtainVotd(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        quranMeta,
                        chapterNo,
                        verseNo,
                        null,
                    )
                }
            }
        }
    })
}

internal fun onObtainVotd(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    quranMeta: QuranMeta,
    chapterNo: Int,
    verseNo: Int,
    arabicTextBitmap: Bitmap?
) {
    val views = RemoteViews(context.packageName, R.layout.votd_widget)

    QuranTranslationFactory(context).use { factory ->
        val bookInfo = factory.getTranslationBookInfo(VerseUtils.obtainOptimalSlugForVotd(context))

        val translation = factory.getTranslationsSingleSlugVerse(bookInfo.slug, chapterNo, verseNo)


        views.setTextViewText(
            R.id.verseInfo, context.getString(
                R.string.strLabelVerseWithChapNameAndNo,
                quranMeta.getChapterName(context, chapterNo),
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


        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.votdWidgetContainer, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

internal fun prepareArabicTextBitmap(
    context: Context,
    verse: Verse,
    quranScript: String,
    maxWidth: Int,
): Bitmap? {
    val pageNo = verse.pageNo

    val txtSizeSp = quranScript.getQuranScriptVerseTextSizeWidgetSP()
    val verseTextSize = context.sp2px(txtSizeSp)
    var fontQuranText: Typeface?

    if (quranScript.isKFQPCScript()) {
        val fontsDir = FileUtils.newInstance(context).getKFQPCScriptFontDir(quranScript)
        val fontFile = File(fontsDir, pageNo.toKFQPCFontFilename())

        fontQuranText = if (fontFile.length() > 0L) {
            Typeface.createFromFile(fontFile)
        } else {
            Typeface.createFromFile(File(fontsDir, pageNo.toKFQPCFontFilenameOld()))
        }
    } else {
        fontQuranText = context.getFont(quranScript.getQuranScriptFontRes())
    }

    if (fontQuranText == null) {
        return null
    }

    val bitmap = createTextBitmap(
        context,
        verse.arabicText,
        fontQuranText,
        verseTextSize,
        Color.WHITE,
        maxWidth
    )

    return bitmap
}

internal fun createTextBitmap(
    context: Context, text: String, typeface: Typeface, textSize: Int, color: Int, maxWidth: Int,
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

    // Create bitmap with calculated height
    val bitmap = createBitmap(maxWidth, staticLayout.height)
    val canvas = Canvas(bitmap)
    canvas.withSave {
        staticLayout.draw(this)
    }

    return bitmap
}
