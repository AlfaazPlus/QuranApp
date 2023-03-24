/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.interfaceUtils;

import android.view.View;

import com.quranapp.android.adapters.transl.ADPDownloadTranslations;
import com.quranapp.android.components.transls.TranslModel;

public interface TranslDownloadExplorerImpl {
    void onDownloadAttempt(ADPDownloadTranslations adapter, ADPDownloadTranslations.VHDownloadTransl vhTransl, View referencedView, TranslModel model);
}
