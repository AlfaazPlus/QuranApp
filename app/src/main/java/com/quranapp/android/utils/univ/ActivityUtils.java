package com.quranapp.android.utils.univ;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

public abstract class ActivityUtils {
    public static void openAppDetailsActivity(@NonNull Context context) {
        final Intent settingIntent = new Intent();
        settingIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        settingIntent.addCategory(Intent.CATEGORY_DEFAULT);
        settingIntent.setData(Uri.parse("package:" + context.getPackageName()));
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(settingIntent);
    }
}
