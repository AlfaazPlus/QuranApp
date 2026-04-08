package com.quranapp.android.reader_managers;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.quran.subcomponents.Footnote;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.thread.runner.RunnableTaskRunner;
import com.quranapp.android.views.reader.dialogs.FootnotePresenter;
import com.quranapp.android.widgets.dialog.loader.PeaceProgressDialog;

import java.util.Set;

import kotlin.Pair;

public class ActionController implements Destroyable {
    private final ReaderPossessingActivity mActivity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RunnableTaskRunner taskRunner = new RunnableTaskRunner(handler);
    private final FootnotePresenter mFootnotePresenter = new FootnotePresenter();
    private final PeaceProgressDialog mProgressDialog;

    public ActionController(ReaderPossessingActivity readerPossessingActivity) {
        mActivity = readerPossessingActivity;

        mProgressDialog = new PeaceProgressDialog(mActivity);
        mProgressDialog.setDimAmount(0);
        mProgressDialog.setElevation(0);
    }

    public void showFootnote(Verse verse, Footnote footnote, boolean isUrduSlug) {
        mFootnotePresenter.present(mActivity, verse, footnote, isUrduSlug);
    }

    public void showFootnotes(Verse verse) {
        mFootnotePresenter.present(mActivity, verse);
    }

    public void openVerseOptionDialog(Verse verse, @Nullable BookmarkCallbacks bookmarkCallbacks) {
//        mVOD.open(mActivity, verse, bookmarkCallbacks);
    }

    public void openShareDialog(int chapterNo, int verseNo) {
//        mVOD.openShareDialog(mActivity, chapterNo, verseNo);
    }

    public void dismissShareDialog() {
//        mVOD.dismissShareDialog();
    }

    public void showVerseReference(Set<String> translSlugs, int chapterNo, String verses) {
       /* try {
            mQuickReference.show(mActivity, translSlugs, chapterNo, verses);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }*/
    }

    public void showReferenceSingleVerseOrRange(
        Set<String> translSlugs,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        // TODO
//        mQuickReference.showSingleVerseOrRange(mActivity, translSlugs, chapterNo, verseRange);
    }

    public void openVerseReference(int chapterNo, Pair<Integer, Integer> verseRange) {
        // TODO
        /*if (mActivity instanceof ActivityReader) {
            openVerseReferenceWithinReader((ActivityReader) mActivity, chapterNo, verseRange);
        } else {
            ReaderFactory.startVerseRange(mActivity, chapterNo, verseRange);
        }*/

        closeDialogs(true);
    }


    public void onVerseRecite(int chapterNo, int verseNo, boolean isReciting) {
       /* mVOD.onVerseRecite(chapterNo, verseNo, isReciting);
        mPageVerseDialog.onVerseRecite(chapterNo, verseNo, isReciting);*/
    }

    public void showLoader() {
        mProgressDialog.show();
    }

    public void dismissLoader() {
        mProgressDialog.dismiss();
    }

    public void closeDialogs(boolean closeForReal) {
        mProgressDialog.dismiss();

        if (closeForReal || mActivity.isFinishing()) {
           /* mQuickReference.dismiss();
            mVOD.dismiss();
            mVOD.dismissShareDialog();
            mFootnotePresenter.dismiss();
            mPageVerseDialog.dismiss();*/
        }
    }

    @Override
    public void destroy() {
        taskRunner.cancel();

        closeDialogs(false);
    }
}
