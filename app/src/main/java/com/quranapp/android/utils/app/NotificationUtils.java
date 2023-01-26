/*
 * (c) Faisal Khan. Created on 18/2/2022.
 */

package com.quranapp.android.utils.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.quranapp.android.R;

public final class NotificationUtils {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannels(Context ctx) {
        NotificationManager manager = ctx.getSystemService(NotificationManager.class);

        NotificationChannel defChannel = createDefaultChannel(ctx);
        NotificationChannel votdChannel = createVOTDChannel(ctx);
        NotificationChannel downloadsChannel = createDownloadsChannel(ctx);
        NotificationChannel recitationPlayerChannel = createRecitationChannel(ctx);

        // Register the channels with the system;
        manager.createNotificationChannel(defChannel);
        manager.createNotificationChannel(votdChannel);
        manager.createNotificationChannel(downloadsChannel);
        manager.createNotificationChannel(recitationPlayerChannel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createDefaultChannel(Context ctx) {
        String CHANNEL_ID = ctx.getString(R.string.strNotifChannelIdDefault);
        CharSequence name = ctx.getString(R.string.strNotifChannelDefault);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, IMPORTANCE_DEFAULT);
        channel.setDescription(null);
        channel.enableLights(true);
        channel.setShowBadge(true);
        channel.enableVibration(true);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        long[] pattern = {500, 500};
        channel.setVibrationPattern(pattern);

        AudioAttributes.Builder attr = new AudioAttributes.Builder();
        attr.setUsage(AudioAttributes.USAGE_NOTIFICATION);
        attr.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
        channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attr.build());

        return channel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createVOTDChannel(Context ctx) {
        String CHANNEL_ID = ctx.getString(R.string.strNotifChannelIdVOTD);
        CharSequence name = ctx.getString(R.string.strNotifChannelVOTD);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, IMPORTANCE_DEFAULT);
        channel.setDescription(null);
        channel.enableLights(true);
        channel.setShowBadge(true);
        channel.enableVibration(true);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        long[] pattern = {500, 500};
        channel.setVibrationPattern(pattern);

        AudioAttributes.Builder attr = new AudioAttributes.Builder();
        attr.setUsage(AudioAttributes.USAGE_NOTIFICATION);
        attr.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
        channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attr.build());

        return channel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createDownloadsChannel(Context ctx) {
        String CHANNEL_ID = ctx.getString(R.string.strNotifChannelIdDownloads);
        CharSequence name = ctx.getString(R.string.strNotifChannelDownloads);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, IMPORTANCE_DEFAULT);
        channel.setDescription(null);
        channel.enableLights(false);
        channel.setSound(null, null);
        channel.setVibrationPattern(null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        return channel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createRecitationChannel(Context ctx) {
        String CHANNEL_ID = ctx.getString(R.string.strNotifChannelIdRecitation);
        CharSequence name = "Recitation Player";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, IMPORTANCE_DEFAULT);
        channel.setDescription(null);
        channel.enableLights(false);
        channel.setSound(null, null);
        channel.setVibrationPattern(null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        return channel;
    }


    public static Notification createEmptyNotif(Context ctx, String channelId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId);
        builder.setContentTitle("");
        builder.setSmallIcon(R.mipmap.icon_launcher);
        builder.setContentText("");
        return builder.build();
    }
}
