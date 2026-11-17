package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.screens.collections.CollectionsScreen
import com.quranapp.android.theme.QuranAppTheme
import com.quranapp.android.utils.miscellenous.Keys.COLLECTION_ID

class ActivityCollections : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val openId = intent.getLongExtra(COLLECTION_ID, -1L).takeIf { it > 0 }

        setContent {
            QuranAppTheme {
                CollectionsScreen(initialCollectionId = openId)
            }
        }
    }
}
