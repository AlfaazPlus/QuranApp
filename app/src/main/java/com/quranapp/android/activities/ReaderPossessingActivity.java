package com.quranapp.android.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.R;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ReaderPossessingActivity extends QuranMetaPossessingActivity {
    public final AtomicReference<Quran> mQuranRef = new AtomicReference<>();
    public QuranTranslationFactory mTranslFactory;

    public int mColorSecondary;
    public int mVerseHighlightedBGColor;
    public int mVerseUnhighlightedBGColor = Color.TRANSPARENT;
    public int mRefHighlightTxtColor;
    public int mRefHighlightBGColor;
    public int mRefHighlightBGColorPres;
    public int mAuthorTextSize;
    public Typeface mUrduTypeface;

    @Override
    protected void onDestroy() {
        if (mTranslFactory != null) {
            mTranslFactory.close();
        }

        super.onDestroy();
    }

    @CallSuper
    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        mTranslFactory = new QuranTranslationFactory(this);

        mColorSecondary = color(R.color.colorSecondary);
        mVerseHighlightedBGColor = color(R.color.colorBGReaderVerseSelected);
        mRefHighlightTxtColor = color(R.color.colorSecondary);
        mRefHighlightBGColor = color(R.color.colorPrimaryAlpha10);
        mRefHighlightBGColorPres = color(R.color.colorPrimaryAlpha50);
        mAuthorTextSize = dimen(R.dimen.dmnCommonSize3);
        mUrduTypeface = font(R.font.noto_nastaliq_urdu_variable);
    }

    @Override
    protected void preQuranMetaPrepare(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        preReaderReady(activityView, getIntent(), savedInstanceState);
    }

    @Override
    protected final void onQuranMetaReady(@NotNull View activityView, @NotNull Intent intent, @org.jetbrains.annotations.Nullable Bundle savedInstanceState, @NotNull QuranMeta quranMeta) {
        Quran.prepareInstance(this, quranMeta, quran -> {
            mQuranRef.set(quran);
            onReaderReady(getIntent(), savedInstanceState);
        });
    }

    protected abstract void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState);

    protected abstract void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState);

    protected void reparseQuran() {
        Quran.prepareInstance(this, mQuranMetaRef.get(), quran -> {
            mQuranRef.set(quran);
            onQuranReParsed(quran);
        });
    }

    protected void onQuranReParsed(Quran quran) {
    }

    @CallSuper
    @Override
    protected void onActivityResult2(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
//            mActionController.dismissShareDialog();
        }
    }
}
