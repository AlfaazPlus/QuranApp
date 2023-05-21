/*
 * Created by Faisal Khan on (c) 23/8/2021.
 */

package com.quranapp.android.utils.votd;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.quranapp.android.utils.receivers.BootReceiver;
import com.quranapp.android.utils.receivers.ReceiverUtils;
import com.quranapp.android.utils.receivers.VotdReceiver;
import com.quranapp.android.utils.sharedPrefs.SPVerses;
import com.quranapp.android.utils.univ.Codes;

import java.util.Calendar;

public final class VOTDUtils {
    public static void enableVOTDReminder(Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        ReceiverUtils.enableReceiver(context, BootReceiver.class);

        PendingIntent votdReminder = createVOTDReminder(context);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        if (cal.get(Calendar.HOUR_OF_DAY) >= 4) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 4);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, votdReminder);
    }

    public static void disableVOTDReminder(Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        ReceiverUtils.disableReceiver(context, BootReceiver.class);

        PendingIntent votdReminder = createVOTDReminder(context);
        if (votdReminder != null) {
            alarmManager.cancel(votdReminder);
        }
    }

    private static PendingIntent createVOTDReminder(Context context) {
        Intent receiver = new Intent(context, VotdReceiver.class);
        int flag = PendingIntent.FLAG_CANCEL_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, Codes.NOTIF_ID_VOTD, receiver, flag);
    }

    public static boolean isVOTDTrulyEnabled(Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);

        if (alarmManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms()
                && NotificationManagerCompat.from(context).areNotificationsEnabled()
                && SPVerses.getVOTDReminderEnabled(context);
        }

        return NotificationManagerCompat.from(context).areNotificationsEnabled()
            && SPVerses.getVOTDReminderEnabled(context);
    }
}
