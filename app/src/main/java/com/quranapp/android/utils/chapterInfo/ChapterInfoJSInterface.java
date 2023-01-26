package com.quranapp.android.utils.chapterInfo;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.quranapp.android.activities.ActivityChapInfo;
import com.quranapp.android.components.quran.QuranMeta;
import com.peacedesign.android.utils.Log;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.univ.NotifUtils;

public class ChapterInfoJSInterface {
    private final ActivityChapInfo mActivity;

    public ChapterInfoJSInterface(ActivityChapInfo activityChapInfo) {
        mActivity = activityChapInfo;
    }

    @JavascriptInterface
    public void openReference(int chapterNo, int fromVerse, int toVerse) {
        QuranMeta quranMeta = mActivity.mQuranMeta;

        if (!QuranMeta.isChapterValid(chapterNo) || !quranMeta.isVerseRangeValid4Chapter(chapterNo, fromVerse, toVerse)) {
            Log.d(chapterNo, fromVerse, toVerse);
            NotifUtils.showRemovableText(mActivity, "Could not open references", Toast.LENGTH_LONG);
            return;
        }

        mActivity.showReferenceSingleVerseOrRange(TranslUtils.defaultTranslationSlugs(), chapterNo, new int[]{fromVerse, toVerse});
    }
}
