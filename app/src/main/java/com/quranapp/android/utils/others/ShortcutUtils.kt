/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */

package com.quranapp.android.utils.others;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.util.ArrayList;
import java.util.List;

public class ShortcutUtils {
    public static void pushVOTDShortcut(Context ctx, int chapterNo, int verseNo) {
        Intent intent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo);
        intent.setClass(ctx, ActivityReader.class);
        intent.setAction(Intent.ACTION_VIEW);

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(ctx, ctx.getString(R.string.strNotifChannelIdVOTD))
                .setShortLabel(ctx.getString(R.string.strTitleVOTD))
                .setLongLabel(ctx.getString(R.string.strTitleVOTD))
                .setIntent(intent);
        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.drawable.dr_ic_shortcut_votd));
        } catch (Exception ignored) {}
        ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build());
    }

    public static void pushLastVersesShortcut(Context ctx, QuranMeta quranMeta, int readType, int readerStyle,
                                              int juzNo, int chapterNo, int fromVerse, int toVerse) {
        String id = ctx.getString(R.string.strShortcutIdLastVerses);

        Intent intent = ReaderFactory.prepareLastVersesIntent(quranMeta, juzNo, chapterNo, fromVerse, toVerse, readType, readerStyle);
        if (intent == null) {
            List<String> ids = new ArrayList<>();
            ids.add(id);
            ShortcutManagerCompat.removeDynamicShortcuts(ctx, ids);
            return;
        }

        intent.setClass(ctx, ActivityReader.class);
        intent.setAction(Intent.ACTION_VIEW);

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(ctx, id)
                .setShortLabel(ctx.getString(R.string.strLabelContinueReading))
                .setLongLabel(ctx.getString(R.string.strLabelContinueReading))
                .setIntent(intent);

        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.mipmap.icon_launcher));
        } catch (Exception ignored) {}

        ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build());
    }
}
