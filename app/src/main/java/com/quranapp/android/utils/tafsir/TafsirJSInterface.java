package com.quranapp.android.utils.tafsir;

import android.content.ComponentName;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ActivityTafsir2;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.univ.Codes;
import com.quranapp.android.utils.univ.Keys;

public class TafsirJSInterface {
    private final ActivityTafsir2 mActivityTafsir;

    public TafsirJSInterface(ActivityTafsir2 activityTafsir) {
        mActivityTafsir = activityTafsir;
    }

    @JavascriptInterface
    public void goToVerse() {
        Intent intent = ReaderFactory.prepareSingleVerseIntent(mActivityTafsir.mChapterNo, mActivityTafsir.mVerseNo);

        ComponentName callingActivity = mActivityTafsir.getCallingActivity();
        if (callingActivity != null && ActivityReader.class.getName().equals(callingActivity.getClassName())) {
            mActivityTafsir.setResult(Codes.OPEN_REFERENCE_RESULT_CODE, intent);
            mActivityTafsir.finish();
        } else {
            intent = intent.setClass(mActivityTafsir, ActivityReader.class);
            mActivityTafsir.startActivity(intent);
        }
    }

    @JavascriptInterface
    public void previousTafsir() {
        if (mActivityTafsir.mVerseNo == 1) {
            return;
        }
        Intent intent = mActivityTafsir.getIntent();
        intent.putExtra(Keys.READER_KEY_VERSE_NO, mActivityTafsir.mVerseNo - 1);
        intent.setAction(null);
        mActivityTafsir.startActivity(intent);
    }

    @JavascriptInterface
    public void nextTafsir() {
        if (mActivityTafsir.mVerseNo == mActivityTafsir.mQuranMeta.getChapterVerseCount(mActivityTafsir.mChapterNo)) {
            return;
        }

        Intent intent = mActivityTafsir.getIntent();
        intent.putExtra(Keys.READER_KEY_VERSE_NO, mActivityTafsir.mVerseNo + 1);
        intent.setAction(null);
        mActivityTafsir.startActivity(intent);
    }
}
