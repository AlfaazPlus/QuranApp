/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */
package com.quranapp.android.utils.others

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.core.R
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.factory.ReaderFactory

object ShortcutUtils {
    fun pushVOTDShortcut(ctx: Context, chapterNo: Int, verseNo: Int) {
        val intent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo).apply {
            setClassName(ctx, AppDestinations.READER)
            action = Intent.ACTION_VIEW
        }

        val builder = ShortcutInfoCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_VOTD)
            .setShortLabel(ctx.getString(R.string.strTitleVOTD))
            .setLongLabel(ctx.getString(R.string.strTitleVOTD))
            .setIntent(intent)

        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.drawable.dr_ic_shortcut_votd))
            ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushLastVersesShortcut(
        ctx: Context,
        entity: ReadHistoryEntity,
    ) {
        try {
            val id = "last_verses"
            val intent = ReaderFactory.prepareLastVersesIntent(
                readType = ReadType.fromValue(entity.readType),
                readerMode = ReaderMode.fromValue(entity.readerMode),
                verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
                divisionNo = entity.divisionNo,
            )

            if (intent == null) {
                val ids: MutableList<String> = ArrayList()
                ids.add(id)
                ShortcutManagerCompat.removeDynamicShortcuts(ctx, ids)
                return
            }

            intent.setClassName(ctx, AppDestinations.READER)
            intent.action = Intent.ACTION_VIEW

            val builder = ShortcutInfoCompat.Builder(ctx, id)
                .setShortLabel(ctx.getString(R.string.continue_reading_2))
                .setLongLabel(ctx.getString(R.string.continue_reading_2))
                .setIntent(intent)

            try {
                builder.setIcon(IconCompat.createWithResource(ctx, R.mipmap.icon_launcher))
                ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.saveError(e, "ShortcutUtils")
        }
    }
}
