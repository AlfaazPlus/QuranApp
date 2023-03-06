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

import com.quranapp.android.activities.ActivityOnboarding;
import com.quranapp.android.databinding.LytThemeExplorerBinding;
import com.quranapp.android.utils.app.ThemeUtils;
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs;

import kotlin.Unit;

public class FragOnBoardThemes extends FragOnBoardBase {
    public static FragOnBoardThemes newInstance() {
        return new FragOnBoardThemes();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LytThemeExplorerBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LytThemeExplorerBinding mThemeBinding = LytThemeExplorerBinding.bind(view);
        setupThemeBinding(mThemeBinding);
    }

    private void setupThemeBinding(LytThemeExplorerBinding binding) {
        binding.themeGroup.setOnCheckChangedListener((button, checkedId) -> {
            if (getActivity() instanceof ActivityOnboarding) {
                ((ActivityOnboarding) getActivity()).mThemeChanged = true;
            }
            final String themeMode = ThemeUtils.resolveThemeModeFromId(checkedId);
            SPAppConfigs.setThemeMode(button.getContext(), themeMode);

            return Unit.INSTANCE;
        });
    }
}
