/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.interfaceUtils

import android.view.View
import com.quranapp.android.adapters.transl.ADPDownloadTranslations
import com.quranapp.android.adapters.transl.ADPDownloadTranslations.VHDownloadTransl
import com.quranapp.android.components.transls.TranslModel

interface TranslDownloadExplorerImpl {
    fun onDownloadAttempt(adapter: ADPDownloadTranslations, vhTransl: VHDownloadTransl, referencedView: View, model: TranslModel)
}
