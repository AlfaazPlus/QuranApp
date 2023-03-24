/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.transls

class TranslationGroupModel(
    val langCode: String,
) {
    var langName = ""
    var translations: ArrayList<TranslModel> = ArrayList()
    var isExpanded = false
}
