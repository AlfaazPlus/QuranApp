/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */

package com.quranapp.android.utils.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;

public class TranslDownloadReceiver extends BroadcastReceiver {
    public static final String KEY_TRANSL_BOOK_INFO = "key.translation_book_info";
    public static final String ACTION_TRANSL_DOWNLOAD_STATUS = "TranslDownloadService.action.status";
    public static final String ACTION_NO_MORE_DOWNLOADS = "TranslDownloadService.action.no_downloads";
    public static final String KEY_TRANSL_DOWNLOAD_STATUS = "TranslDownloadService.download.status";
    public static final String TRANSL_DOWNLOAD_STATUS_CANCELED = "TranslDownloadService.download.canceled";
    public static final String TRANSL_DOWNLOAD_STATUS_FAILED = "TranslDownloadService.download.failed";
    public static final String TRANSL_DOWNLOAD_STATUS_SUCCEED = "TranslDownloadService.download.succeed";
    private TranslDownloadStateListener mStateListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mStateListener == null || intent == null) {
            return;
        }

        if (ACTION_TRANSL_DOWNLOAD_STATUS.equals(intent.getAction())) {
            QuranTranslBookInfo bookInfo = (QuranTranslBookInfo) intent.getSerializableExtra(KEY_TRANSL_BOOK_INFO);
            String status = intent.getStringExtra(KEY_TRANSL_DOWNLOAD_STATUS);
            mStateListener.onTranslDownloadStatus(bookInfo, status);
        } else if (ACTION_NO_MORE_DOWNLOADS.equalsIgnoreCase(intent.getAction())) {
            mStateListener.onNoMoreDownloads();
        }
    }

    public void setDownloadStateListener(TranslDownloadStateListener listener) {
        mStateListener = listener;
    }

    public void removeListener() {
        mStateListener = null;
    }

    public interface TranslDownloadStateListener {
        void onTranslDownloadStatus(QuranTranslBookInfo translModel, String status);

        void onNoMoreDownloads();
    }
}