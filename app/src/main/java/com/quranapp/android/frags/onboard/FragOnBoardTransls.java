/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 31/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.onboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.adapters.transl.ADPTransls;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.frags.settings.FragSettingsTransl;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.univ.FileUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragOnBoardTransls extends FragOnBoardBase {
    private final CallableTaskRunner<List<TranslBaseModel>> mTranslTaskRunner = new CallableTaskRunner<>();
    private final Set<String> mTranslSlugs = new HashSet<>();

    public static FragOnBoardTransls newInstance() {
        return new FragOnBoardTransls();
    }

    @Override
    public void onDestroy() {
        mTranslTaskRunner.cancel();
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return new RecyclerView(inflater.getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView list = (RecyclerView) view;
        initTranslations(list);
    }


    private void initTranslations(RecyclerView list) {
        list.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        showTransls(list);
    }

    private void showTransls(RecyclerView list) {
        mTranslTaskRunner.callAsync(
            new FragSettingsTransl.LoadTranslsTask(FileUtils.newInstance(list.getContext()), mTranslSlugs) {
                @Override
                public void onComplete(List<TranslBaseModel> translItems) {
                    if (!translItems.isEmpty()) {
                        populateTransls(list, translItems);
                    }
                }
            });
    }

    private void populateTransls(RecyclerView list, List<TranslBaseModel> translItems) {
        ADPTransls adapter = new ADPTransls(list.getContext(), translItems, false,
            (ctx, model, isSelected) -> TranslUtils.resolveSelectionChange(ctx, mTranslSlugs, model, isSelected, true));
        list.setAdapter(adapter);
    }
}
