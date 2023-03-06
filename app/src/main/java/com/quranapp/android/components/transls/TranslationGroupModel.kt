/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.transls

import android.view.ViewGroup

class TranslationGroupModel(
    val langCode: String,
    val langName: String,
    val transls: List<TranslModel>
) {
    var view: ViewGroup? = null
}
