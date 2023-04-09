/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/2/2022.
 * All rights reserved.
 */

package com.quranapp.android.widgets.chapterCard;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChapterCardWithoutIcon extends ChapterCard {
    public ChapterCardWithoutIcon(@NonNull Context context) {
        super(context);
    }

    @Nullable
    @Override
    protected View createRightView() {
        return null;
    }

    @Nullable
    @Override
    protected View createFavIcon() {
        return null;
    }
}
