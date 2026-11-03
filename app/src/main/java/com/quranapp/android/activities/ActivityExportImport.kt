package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.export.ExportImportScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivityExportImport : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                ExportImportScreen()
            }
        }
    }
}
