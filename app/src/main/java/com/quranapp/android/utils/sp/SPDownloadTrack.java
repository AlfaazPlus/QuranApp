/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */

package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.quranapp.android.utils.univ.DateUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class SPDownloadTrack {
    public static final String SP_DOWNLOAD_TRACK = "sp_download_track";

    public static final String KEY_DOWNLOAD_TRACK_TRANSLS = "app.download_track.translations";

    public static int getTranslDownloadsUnder24Hrs(Context ctx, String slug) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_DOWNLOAD_TRACK, Context.MODE_PRIVATE);
        Set<String> totalDownloads = sp.getStringSet(makeKeyTranslDownloadTrack(slug), new HashSet<>());

        Set<String> downloadsUnder24Hrs = new HashSet<>();
        totalDownloads.stream().iterator().forEachRemaining(downloadTime -> {
            if (!TextUtils.isEmpty(downloadTime)) {
                try {
                    if (isDownloadedUnder24Hrs(downloadTime)) {
                        downloadsUnder24Hrs.add(downloadTime);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
        setTranslDownloadsUnder24Hrs(ctx, slug, downloadsUnder24Hrs);
        return downloadsUnder24Hrs.size();
    }

    public static boolean isDownloadedUnder24Hrs(String downloadTime) {
        return DateUtils.hoursSince(downloadTime) < 24;
    }

    public static void clearTranslDownloadsUnder24Hrs(Context ctx, String slug) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_DOWNLOAD_TRACK, Context.MODE_PRIVATE).edit();
        editor.putStringSet(makeKeyTranslDownloadTrack(slug), new HashSet<>());
        editor.apply();
    }

    private static void setTranslDownloadsUnder24Hrs(Context ctx, String slug, Set<String> downloadsUnder24Hrs) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_DOWNLOAD_TRACK, Context.MODE_PRIVATE).edit();
        editor.putStringSet(makeKeyTranslDownloadTrack(slug), downloadsUnder24Hrs);
        editor.apply();
    }

    public static void addTranslDownloadUnder24Hrs(Context ctx, String slug) {
        String key = makeKeyTranslDownloadTrack(slug);

        SharedPreferences sp = ctx.getSharedPreferences(SP_DOWNLOAD_TRACK, Context.MODE_PRIVATE);
        Set<String> totalDownloads = new HashSet<>(sp.getStringSet(key, new HashSet<>()));

        totalDownloads.add(String.valueOf(Calendar.getInstance().getTimeInMillis()));

        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(key, totalDownloads);
        editor.apply();
    }

    private static String makeKeyTranslDownloadTrack(String slug) {
        return KEY_DOWNLOAD_TRACK_TRANSLS + ":" + slug;
    }
}
