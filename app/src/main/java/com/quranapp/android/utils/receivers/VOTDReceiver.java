/*
 * Created by Faisal Khan on (c) 23/8/2021.
 */

package com.quranapp.android.utils.receivers;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.sp.SPVerses;
import com.quranapp.android.utils.univ.Codes;
import com.quranapp.android.utils.verse.VerseUtils;
import com.quranapp.android.utils.votd.VOTDUtils;

public class VOTDReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        QuranMeta.prepareInstance(context, quranMeta -> VerseUtils.getVOTD(context, quranMeta, null, (chapterNo, verseNo) -> {
            if (!QuranMeta.isChapterValid(chapterNo) || !quranMeta.isVerseValid4Chapter(chapterNo, verseNo)) {
                return;
            }

            int notificationId = Codes.NOTIF_ID_VOTD;
            int flag = FLAG_CANCEL_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flag |= PendingIntent.FLAG_IMMUTABLE;
            }

            Intent readerIntent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo);
            readerIntent.setClass(context, ActivityReader.class);
            PendingIntent readerPendingIntent = PendingIntent.getActivity(context, notificationId, readerIntent, flag);

            String channelId = context.getString(R.string.strNotifChannelIdVOTD);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
            builder.setAutoCancel(true);
            builder.setContentTitle(context.getText(R.string.strTitleVOTD));
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
            builder.setContentIntent(readerPendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setSmallIcon(R.drawable.dr_ic_shortcut_votd);
            } else {
                builder.setSmallIcon(R.drawable.dr_logo);
            }

            String chapName = quranMeta.getChapterName(context, chapterNo);
            CharSequence msg = context.getString(R.string.strLabelVerseWithChapNameWithColon, chapName, verseNo);
            builder.setContentText(msg);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationId, builder.build());
        }));

        if (SPVerses.getVOTDReminderEnabled(context)) {
            VOTDUtils.enableVOTDReminder(context);
        }
    }
}