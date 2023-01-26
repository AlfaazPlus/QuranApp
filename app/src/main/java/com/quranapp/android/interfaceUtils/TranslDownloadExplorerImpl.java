/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.interfaceUtils;

import android.view.View;

import com.quranapp.android.adapters.transl.ADPDownloadTransls;
import com.quranapp.android.components.transls.TranslModel;

public interface TranslDownloadExplorerImpl {
    void onDownloadAttempt(ADPDownloadTransls.VHDownloadTransl vhTransl, View referencedView, TranslModel model);
}
