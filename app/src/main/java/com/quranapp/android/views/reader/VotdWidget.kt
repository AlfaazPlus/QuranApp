package com.quranapp.android.views.reader

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.quranapp.android.R
import com.quranapp.android.activities.MainActivity
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.utils.verse.VerseUtils


class VotdWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
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
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                VotdWidget::class.java
            )
        )

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.votd_widget)

    QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
        override fun onReady(quranMeta: QuranMeta) {
            VerseUtils.getVOTD(context, quranMeta, null) { chapterNo, verseNo ->
                if (!QuranMeta.isChapterValid(chapterNo) || !quranMeta.isVerseValid4Chapter(
                        chapterNo,
                        verseNo
                    )
                ) {
                    return@getVOTD
                }

                QuranTranslationFactory(context).use { factory ->
                    val bookInfo =
                        factory.getTranslationBookInfo(VerseUtils.obtainOptimalSlugForVotd(context))

                    val translation =
                        factory.getTranslationsSingleSlugVerse(bookInfo.slug, chapterNo, verseNo)


                    views.setTextViewText(
                        R.id.verseInfo,
                        context.getString(
                            R.string.strLabelVerseWithChapNameAndNo,
                            quranMeta.getChapterName(context, chapterNo),
                            chapterNo,
                            verseNo
                        )
                    )

                    // Remove footnote markers etc
                    views.setTextViewText(
                        R.id.votdText,
                        StringUtils.removeHTML(translation.text, false)
                    )


                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    views.setOnClickPendingIntent(R.id.votdWidgetContainer, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

        }
    })
}