/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.interfaceUtils;

import android.net.Uri;
import android.widget.Toast;

import com.peacedesign.android.utils.Log;
import com.quranapp.android.exc.HttpNotFoundException;
import com.quranapp.android.views.reader.RecitationPlayer;

import java.io.File;

public class PlayerVerseLoadCallback {
    private final RecitationPlayer mPlayer;
    private String mSlug;
    private int mChapterNo;
    private int mVerseNo;

    public PlayerVerseLoadCallback(RecitationPlayer player) {
        mPlayer = player;
        setCurrentVerse(null, -1, -1);
    }

    public void setCurrentVerse(String slug, int chapterNo, int verseNo) {
        mSlug = slug;
        mChapterNo = chapterNo;
        mVerseNo = verseNo;
    }

    public void preLoad() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.reveal();
        mPlayer.setupOnLoadingInProgress(true);
    }

    public void onProgress(String progress) {
        if (mPlayer == null || mSlug == null || mChapterNo == -1 || mVerseNo == -1) {
            return;
        }
        mPlayer.progressText.setText(progress);
    }

    public void onLoaded(File file) {
        if (mPlayer == null || mSlug == null || mChapterNo == -1 || mVerseNo == -1) {
            return;
        }
        Log.d("Verse loaded! - " + mChapterNo + ":" + mVerseNo);
        Uri audioURI = mPlayer.fileUtils.getFileURI(file);
        mPlayer.prepareMediaPlayer(audioURI, mSlug, mChapterNo, mVerseNo, true);
    }

    public void onFailed(Exception e, File file) {
        if (file != null) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        if (mPlayer == null) {
            return;
        }

        if (e instanceof HttpNotFoundException || e.getCause() instanceof HttpNotFoundException) {
            // Audio was unable to load from the url because url was not found,
            // may be recitation manifest has a new url. So force update the manifest file.
            mPlayer.mForceManifestFetch = true;
        }

        mPlayer.release();
        mPlayer.updateProgressBar();
        mPlayer.updateTimelineText();

        if (!mPlayer.mReaderChanging) {
            mPlayer.popMiniMsg("Something happened wrong while loading the verse.", Toast.LENGTH_LONG);
        }

        mPlayer.mReaderChanging = false;

        Log.d("Verse failed to load! - " + mChapterNo + ":" + mVerseNo);
    }

    public void postLoad() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.setupOnLoadingInProgress(false);
    }
}
