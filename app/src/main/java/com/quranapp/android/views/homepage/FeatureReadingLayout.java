/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 28/2/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.homepage;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.R;
import com.quranapp.android.adapters.ADPFeaturedQuran;
import com.quranapp.android.components.FeaturedQuranModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

public class FeatureReadingLayout extends HomepageCollectionLayoutBase {
    private final CallableTaskRunner<List<FeaturedQuranModel>> taskRunner = new CallableTaskRunner<>();
    private boolean mFirstTime = true;

    public FeatureReadingLayout(@NonNull Context context) {
        this(context, null);
    }

    public FeatureReadingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeatureReadingLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getHeaderTitle() {
        return R.string.strTitleFeaturedQuran;
    }

    @Override
    protected int getHeaderIcon() {
        return R.drawable.dr_icon_feature;
    }

    private void refreshFeatured(Context ctx, QuranMeta quranMeta) {
        taskRunner.callAsync(new BaseCallableTask<List<FeaturedQuranModel>>() {
            @Override
            public void preExecute() {
                if (mFirstTime) {
                    showLoader();
                    mFirstTime = false;
                }
            }

            @Override
            public List<FeaturedQuranModel> call() {
                List<FeaturedQuranModel> models = new ArrayList<>();
                TypedArray itemsArray = getResources().obtainTypedArray(R.array.arrFeaturedQuranItems);

                String chapNameFormat = ctx.getString(R.string.strLabelSurah);
                String verseNoFormat = ctx.getString(R.string.strLabelVerseNo);
                String versesFormat = ctx.getString(R.string.strLabelVerses);
                String miniInfoFormat = ctx.getString(R.string.strLabelVerseWithChapNameWithBar);
                String miniInfoChapFormat = ctx.getString(R.string.strLabelFeatureQuranMiniInfo);

                for (int i = 0, l = itemsArray.length(); i < l; i++) {
                    FeaturedQuranModel model = new FeaturedQuranModel();

                    String string = itemsArray.getString(i);
                    String[] splits = string.split(":");

                    final int chapterNo = Integer.parseInt(splits[0]);
                    final int[] verses = new int[2];

                    if (splits.length >= 2) {
                        String[] versesSplits = splits[1].split("[–-]");
                        verses[0] = Integer.parseInt(versesSplits[0]);

                        if (versesSplits.length >= 2) {
                            verses[1] = Integer.parseInt(versesSplits[1]);
                            model.name = String.format(chapNameFormat, quranMeta.getChapterName(ctx, chapterNo));
                            model.miniInfo = String.format(versesFormat, verses[0], verses[1]);
                        } else {
                            verses[1] = verses[0];

                            String chapterName = quranMeta.getChapterName(ctx, chapterNo);
                            if (chapterNo == 2 && verses[0] == 255) {
                                model.name = ctx.getString(R.string.strAyatulKursi);
                                model.miniInfo = String.format(miniInfoFormat, chapterName, 255);
                            } else {
                                model.name = String.format(chapNameFormat, chapterName);
                                model.miniInfo = String.format(verseNoFormat, verses[0]);
                            }
                        }
                    } else {
                        verses[0] = 1;
                        verses[1] = quranMeta.getChapterVerseCount(chapterNo);
                        model.name = String.format(chapNameFormat, quranMeta.getChapterName(ctx, chapterNo));
                        model.miniInfo = String.format(miniInfoChapFormat, chapterNo, 1, verses[1]);
                    }

                    model.chapterNo = chapterNo;
                    model.verseRange = new Pair<>(verses[0], verses[1]);
                    models.add(model);
                }

                itemsArray.recycle();
                return models;
            }

            @Override
            public void onComplete(List<FeaturedQuranModel> models) {
                hideLoader();
                resolveListView().setAdapter(new ADPFeaturedQuran(quranMeta, models));
            }
        });
    }

    public void refresh(QuranMeta quranMeta) {
        refreshFeatured(getContext(), quranMeta);
    }
}
