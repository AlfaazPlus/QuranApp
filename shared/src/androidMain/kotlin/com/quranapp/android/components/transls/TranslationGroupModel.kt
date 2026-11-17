package com.quranapp.android.components.transls

data class TranslationGroupModel(
    val langCode: String,
    var langName: String = "",
    var translations: ArrayList<TranslModel> = ArrayList(),
    var isExpanded: Boolean = false,
) {
}
