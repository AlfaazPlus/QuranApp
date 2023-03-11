/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 19/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.activities.test;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.databinding.ActivityExperimentBinding;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.views.BoldHeader;

import java.util.Collections;
import java.util.List;

public class ActivityExperiment extends BaseActivity {
    private ActivityExperimentBinding mBinding;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_experiment;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityExperimentBinding.bind(activityView);
        initHeader(mBinding.header);
        mBinding.reader.setOnClickListener(v -> ReaderFactory.startChapter(this, 1));
        mBinding.perform.setOnClickListener(v -> perform());
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(this::onBackPressed);
        header.setTitleText("Experiment");

        header.setShowRightIcon(false);

        header.setBGColor(R.color.colorBGPage);
    }

    private void perform() {
        QuranTranslationFactory translFactory = new QuranTranslationFactory(this);

        List<Translation> transls = translFactory.getTranslationsSingleVerse(1, 1);
        Log.d(transls);

        int[] ints = {1, 7, 2, 3};
        Log.d(translFactory.getTranslationsDistinctVerses(
            Collections.singleton(TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN), 88,
            ints));

        Log.d(translFactory.getTranslationsVerseRange(2, 10, 12));
        Log.d(translFactory.getTranslationBookInfo(TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN));
    }
}