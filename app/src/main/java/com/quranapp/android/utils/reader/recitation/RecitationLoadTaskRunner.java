/*
 * (c) Faisal Khan. Created on 25/11/2021.
 */

package com.quranapp.android.utils.reader.recitation;

import androidx.annotation.NonNull;

import com.quranapp.android.interfaceUtils.PlayerVerseLoadCallback;
import com.quranapp.android.utils.thread.runner.MultiTaskRunner;
import com.quranapp.android.utils.thread.tasks.recitation.LoadRecitationAudioTask;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RecitationLoadTaskRunner extends MultiTaskRunner<String, Void, LoadRecitationAudioTask> {
    private final Map<String, PlayerVerseLoadCallback> mCallbacks = new HashMap<>();

    public void addTask(File verseFile, String url, String slug, int chapterNo, int verseNo, PlayerVerseLoadCallback callback) {
        load(verseFile, url, slug, chapterNo, verseNo, callback);
    }

    private void load(File verseFile, String url, String slug, int chapterNo, int verseNo, PlayerVerseLoadCallback callback) {
        final String key = makeKey(slug, chapterNo, verseNo);

        addCallback(slug, chapterNo, verseNo, callback);

        LoadRecitationAudioTask task = new LoadRecitationAudioTask(verseFile, url) {
            @Override
            public void onProgress(long currentLength, long totalLength) {
                PlayerVerseLoadCallback callback = mCallbacks.get(key);

                if (callback != null) {
                    int progress = (int) (((float) currentLength / totalLength) * 100);
                    String progressText = String.format(Locale.getDefault(), "%d%%", progress);
                    callback.onProgress(progressText);
                }
            }

            @Override
            public void onComplete(Void result) {
                publishVerseLoadStatus(verseFile, slug, chapterNo, verseNo, true, null);
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                e.printStackTrace();
                publishVerseLoadStatus(verseFile, slug, chapterNo, verseNo, false, e);
            }
        };

        addTask(key, task);
    }

    private String makeKey(String slug, int chapterNo, int verseNo) {
        return slug + ":" + chapterNo + ":" + verseNo;
    }

    private void publishVerseLoadStatus(File verseFile, String slug, int chapterNo, int verseNo, boolean loaded, Exception e) {
        String key = makeKey(slug, chapterNo, verseNo);

        PlayerVerseLoadCallback callback = mCallbacks.get(key);
        publishVerseLoadStatus(verseFile, callback, loaded, e);
        removeCallback(slug, chapterNo, verseNo);
    }

    public void publishVerseLoadStatus(File verseFile, PlayerVerseLoadCallback callback, boolean loaded, Exception e) {
        if (callback == null) {
            return;
        }

        if (loaded) {
            callback.onLoaded(verseFile);
        } else {
            callback.onFailed(e, verseFile);
        }
        callback.postLoad();
    }

    public boolean isPending(String slug, int chapterNo, int verseNo) {
        return !isDone(makeKey(slug, chapterNo, verseNo));
    }

    public void addCallback(String slug, int chapterNo, int verseNo, PlayerVerseLoadCallback callback) {
        String key = makeKey(slug, chapterNo, verseNo);
        if (callback == null) {
            mCallbacks.remove(key);
        } else {
            callback.setCurrentVerse(slug, chapterNo, verseNo);
            mCallbacks.put(key, callback);
        }
    }

    public void removeCallback(String slug, int chapterNo, int verseNo) {
        mCallbacks.remove(makeKey(slug, chapterNo, verseNo));
    }
}
