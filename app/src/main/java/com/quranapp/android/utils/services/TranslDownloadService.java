/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */

package com.quranapp.android.utils.services;

import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.KEY_TRANSL_DOWNLOAD_STATUS;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.utils.app.AppActions;
import com.quranapp.android.utils.app.NotificationUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslFactory;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.sp.SPDownloadTrack;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class TranslDownloadService extends Service {
    private final TranslationDownloadServiceBinder mBinder = new TranslationDownloadServiceBinder();
    private final Set<String> mCurrentDownloads = new HashSet<>();
    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>();
    private static boolean mStartedByActivity;

    public static void startDownloadService(ContextWrapper context, QuranTranslBookInfo bookInfo) {
        mStartedByActivity = true;
        Intent service = new Intent(context, TranslDownloadService.class);
        service.putExtra(KEY_TRANSL_BOOK_INFO, bookInfo);
        ContextCompat.startForegroundService(context, service);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mStartedByActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = NotificationUtils.createEmptyNotif(this,
                    getString(R.string.strNotifChannelIdDownloads));
            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStartedByActivity = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int flag = START_NOT_STICKY;

        if (intent == null) {
            Notification notification = NotificationUtils.createEmptyNotif(this,
                    getString(R.string.strNotifChannelIdDownloads));
            startForeground(1, notification);
            finish();
            return flag;
        }

        QuranTranslBookInfo bookInfo = (QuranTranslBookInfo) intent.getSerializableExtra(KEY_TRANSL_BOOK_INFO);

        mCurrentDownloads.add(bookInfo.getSlug());

        NotificationCompat.Builder notifBuilder = prepareNotification(bookInfo);

        NotificationManagerCompat notifManager = NotificationManagerCompat.from(this);
        showNotification(bookInfo.getSlug().hashCode(), notifBuilder.build(), notifManager);

        startDownload(bookInfo, notifBuilder, notifManager);

        return flag;
    }

    private void showNotification(int notifId, Notification notification, NotificationManagerCompat notifManager) {
        notifManager.cancel(1);
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
        startForeground(notifId, notification);
    }

    private void startDownload(QuranTranslBookInfo bookInfo, NotificationCompat.Builder notifBuilder, NotificationManagerCompat notifManager) {
        LoadSingleTranslTask loadSingleTranslTask = new LoadSingleTranslTask(this, bookInfo, notifBuilder,
                notifManager);
        TranslUtils.prepareSingleTranslUrlFB(bookInfo, uri -> {
            loadSingleTranslTask.setUrl(uri.toString());
            mTaskRunner.callAsync(loadSingleTranslTask);
        }, e -> {
            loadSingleTranslTask.onFailed(e);
            loadSingleTranslTask.postExecute();
        });
    }

    private NotificationCompat.Builder prepareNotification(QuranTranslBookInfo bookInfo) {
        String channelId = getString(R.string.strNotifChannelIdDownloads);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setSmallIcon(R.drawable.dr_logo);
        builder.setContentTitle(bookInfo.getBookName());
        builder.setSubText("Downloading");
        builder.setCategory(NotificationCompat.CATEGORY_PROGRESS);

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag |= PendingIntent.FLAG_IMMUTABLE;
        }
        Intent activityIntent = new Intent(this, ActivitySettings.class);
        activityIntent.putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, ActivitySettings.SETTINGS_TRANSL_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, bookInfo.getSlug().hashCode(), activityIntent,
                flag);
        builder.setContentIntent(pendingIntent);

        return builder;
    }

    public boolean isDownloading(String slug) {
        return mCurrentDownloads.contains(slug);
    }

    public void removeDownload(String slug) {
        mCurrentDownloads.remove(slug);

        if (mCurrentDownloads.size() == 0) {
            sendBroadcast(new Intent(ACTION_NO_MORE_DOWNLOADS));

            finish();
        }
    }

    private void finish() {
        stopForeground(true);
        stopSelf();
    }

    public class TranslationDownloadServiceBinder extends Binder {
        public TranslDownloadService getService() {
            return TranslDownloadService.this;
        }
    }

    private static class LoadSingleTranslTask extends SimpleDataLoaderTask {
        private final TranslDownloadService mService;
        private final QuranTranslBookInfo bookInfo;
        private final NotificationCompat.Builder mNotifBuilder;
        private final NotificationManagerCompat mNotificationManager;
        private final Handler mProgressHandler = new Handler();
        private boolean mProgressUpdatable = true;
        private QuranTranslFactory mTranslFactory;

        public LoadSingleTranslTask(TranslDownloadService service, QuranTranslBookInfo bookInfo, NotificationCompat.Builder notifBuilder, NotificationManagerCompat notifManager) {
            super(null);
            mService = service;
            this.bookInfo = bookInfo;
            mNotifBuilder = notifBuilder;

            mNotificationManager = notifManager;
        }

        @Override
        public void preExecute() {
            mTranslFactory = new QuranTranslFactory(mService);
            mNotificationManager.notify(bookInfo.getSlug().hashCode(), mNotifBuilder.build());
        }

        @Override
        public String call() throws Exception {
            String data = super.call();

            if (TextUtils.isEmpty(data)) {
                onProgress(100, 100);
                throw new IllegalStateException("Returned translation data is empty.");
            }

            mTranslFactory.getDbHelper().storeTranslation(bookInfo, data);

            return null;
        }

        @Override
        public void onProgress(long downloaded, long total) {
            if (!mProgressUpdatable) {
                return;
            }

            mProgressUpdatable = false;
            AtomicBoolean addedToDownloadTracker = new AtomicBoolean(false);
            mProgressHandler.postDelayed(() -> {
                if (isDone()) {
                    return;
                }
                int percentage = (int) ((downloaded * 100) / total);
                mNotifBuilder.setProgress(100, percentage, false);
                mNotifBuilder.setContentText(percentage + "%");

                if (!addedToDownloadTracker.get() && percentage >= 50) {
                    addedToDownloadTracker.set(true);
                    SPDownloadTrack.addTranslDownloadUnder24Hrs(mService, bookInfo.getSlug());
                }

                mNotificationManager.notify(bookInfo.getSlug().hashCode(), mNotifBuilder.build());

                mProgressUpdatable = true;
            }, 10);
        }

        @Override
        public void onComplete(String result) {
            sendBroadcast(TRANSL_DOWNLOAD_STATUS_SUCCEED);

            SPAppActions.removeFromPendingAction(mService, AppActions.APP_ACTION_TRANSL_UPDATE, bookInfo.getSlug());

            String slug = bookInfo.getSlug();

            Set<String> savedTranslations = SPReader.getSavedTranslations(mService);
            if (savedTranslations.remove(slug)) {
                SPReader.setSavedTranslations(mService, savedTranslations);
            }
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            super.onFailed(e);
            sendBroadcast(TRANSL_DOWNLOAD_STATUS_FAILED);
        }

        @Override
        public void postExecute() {
            if (mTranslFactory != null) {
                mTranslFactory.close();
            }

            mService.removeDownload(bookInfo.getSlug());
            mNotificationManager.cancel(bookInfo.getSlug().hashCode());
        }

        private void sendBroadcast(String status) {
            Intent intent = new Intent(ACTION_TRANSL_DOWNLOAD_STATUS);

            intent.putExtra(KEY_TRANSL_BOOK_INFO, bookInfo);
            intent.putExtra(KEY_TRANSL_DOWNLOAD_STATUS, status);

            mService.sendBroadcast(intent);
        }
    }
}
