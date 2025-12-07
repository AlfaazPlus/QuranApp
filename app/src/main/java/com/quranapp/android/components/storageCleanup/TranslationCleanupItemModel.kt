/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.storageCleanup

import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslBaseModel

class TranslationCleanupItemModel(val bookInfo: TranslationBookInfoModel) : TranslBaseModel() {
    var isDeleted: Boolean = false
}
