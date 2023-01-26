/*
 * (c) Faisal Khan. Created on 29/9/2021.
 */

package com.quranapp.android.utils.reader.recitation;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.app.PendingIntent.getActivity;
import static android.app.PendingIntent.getBroadcast;
import static com.quranapp.android.utils.univ.Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION;
import static com.quranapp.android.utils.app.NotificationUtils.createEmptyNotif;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_NEXT_VERSE;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PAUSE;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PLAY;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PLAY_CONTROL;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PREVIOUS_VERSE;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_STOP;
import static com.quranapp.android.utils.univ.Codes.NOTIF_ID_REC_PLAYER;
import static com.quranapp.android.utils.univ.Codes.REQ_CODE_REC_PLAYER;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;

public class RecitationNotificationService extends Service {
    public static final String KEY_TITLE = "title";
    public static final String KEY_RECITER = "reciter";
    public static final String KEY_PLAYING = "playing";
    private static RecitationNotificationService sInstance;

    public static boolean isInstanceCreated() {
        return sInstance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = createEmptyNotif(this, getString(R.string.strNotifChannelIdRecitation));
            startForeground(NOTIF_ID_REC_PLAYER, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Notification notification = createEmptyNotif(this, getString(R.string.strNotifChannelIdRecitation));
            startForeground(NOTIF_ID_REC_PLAYER, notification);
            finish();
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_PLAY:
            case ACTION_PAUSE:
                showNotification(intent);
                break;
            case ACTION_STOP:
                finish();
                break;
        }
        return START_STICKY;
    }

    private void showNotification(Intent intent) {
        String CHANNEL_ID = getString(R.string.strNotifChannelIdRecitation);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        builder.setShowWhen(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSmallIcon(R.drawable.dr_icon_play2);
        } else {
            builder.setSmallIcon(R.drawable.dr_logo);
        }
        builder.setCustomBigContentView(createRemoteViews(intent));
        builder.setCategory(Notification.CATEGORY_SERVICE);

        int flag = FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag |= FLAG_IMMUTABLE;
        }
        builder.setContentIntent(createContentIntent(flag));

        startForeground(NOTIF_ID_REC_PLAYER, builder.build());
    }

    private void finish() {
        stopForeground(true);
        stopSelf();
    }

    private RemoteViews createRemoteViews(Intent intent) {
        final String title = intent.getStringExtra(KEY_TITLE);
        final String reciter = intent.getStringExtra(KEY_RECITER);
        final boolean playing = intent.getBooleanExtra(KEY_PLAYING, false);

        RemoteViews bigViews = new RemoteViews(getPackageName(), R.layout.lyt_recitation_notification);

        int flag = FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag |= FLAG_IMMUTABLE;
        }

        Intent playContIntent = new Intent(ACTION_PLAY_CONTROL);
        PendingIntent playContPIntent = getBroadcast(this, 0, playContIntent, flag);

        Intent prevIntent = new Intent(ACTION_PREVIOUS_VERSE);
        PendingIntent prevPIntent = getBroadcast(this, 0, prevIntent, flag);

        Intent nextIntent = new Intent(ACTION_NEXT_VERSE);
        PendingIntent nextPIntent = getBroadcast(this, 0, nextIntent, flag);

        bigViews.setImageViewResource(R.id.playControl, playing ? R.drawable.dr_icon_pause2 : R.drawable.dr_icon_play2);
        bigViews.setTextViewText(R.id.title, title);
        bigViews.setTextViewText(R.id.text, reciter);

        bigViews.setOnClickPendingIntent(R.id.playControl, playContPIntent);
        bigViews.setOnClickPendingIntent(R.id.prevVerse, prevPIntent);
        bigViews.setOnClickPendingIntent(R.id.nextVerse, nextPIntent);
        return bigViews;
    }

    private PendingIntent createContentIntent(int flag) {
        Intent openUI = new Intent(this, ActivityReader.class);
        openUI.putExtra(KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, true);
        openUI.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return getActivity(this, REQ_CODE_REC_PLAYER, openUI, flag);
    }
}
