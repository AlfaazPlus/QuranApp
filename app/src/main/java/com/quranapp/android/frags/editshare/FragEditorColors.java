/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.adapters.editor.ADPEditShareColors;
import com.quranapp.android.databinding.FragEditorColorBinding;
import com.quranapp.android.databinding.LytReaderIndexTabBinding;
import com.quranapp.android.utils.extended.GapedItemDecoration;
import com.quranapp.android.utils.univ.SimpleTabSelectorListener;
import com.quranapp.android.views.helper.TabLayout2;

public class FragEditorColors extends FragEditorBase {
    private FragEditorColorBinding mBinding;

    public static FragEditorColors newInstance() {
        return new FragEditorColors();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mBinding == null) {
            mBinding = FragEditorColorBinding.inflate(inflater, container, false);
        }
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupTabs(mBinding.tabLayout);
        initColors(mBinding.colors);
    }

    private void setupTabs(TabLayout2 tabLayout) {
        String[] labels = {"Arabic", "Translation", "Reference"};

        for (int i = 0, l = labels.length; i < l; i++) {
            TabLayout.Tab tab = tabLayout.newTab();
            LytReaderIndexTabBinding binding = LytReaderIndexTabBinding.inflate(LayoutInflater.from(tabLayout.getContext()));
            tab.setCustomView(binding.getRoot());
            binding.tabTitle.setText(labels[i]);

            tabLayout.addTab(tab, i == 0);
        }

        tabLayout.addOnTabSelectedListener(new SimpleTabSelectorListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
            }
        });
    }

    private void initColors(RecyclerView rv) {
        Context context = rv.getContext();

        rv.setLayoutManager(new GridLayoutManager(context, 2, RecyclerView.HORIZONTAL, false));
        rv.addItemDecoration(new GapedItemDecoration(Dimen.dp2px(context, 3)));
        ADPEditShareColors adapter = new ADPEditShareColors(this);
        rv.setAdapter(adapter);
    }

    public void onColorSelect(int color) {
        mBinding.hexCode.setText(ColorUtils.colorIntToHex(color, true));
    }
}
