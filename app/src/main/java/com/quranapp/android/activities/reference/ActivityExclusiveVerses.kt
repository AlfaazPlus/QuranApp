package com.quranapp.android.activities.reference

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.ExclusiveVersesScreenKind
import com.quranapp.android.screens.reference.ExclusiveVersesListScreen
import com.quranapp.android.theme.QuranAppTheme
import com.quranapp.android.utils.miscellenous.Keys

class ActivityExclusiveVerses : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val kind = intent.getStringExtra(Keys.KEY_EXCLUSIVE_VERSES_KIND).let { name ->
            ExclusiveVersesScreenKind.entries.find { it.name == name }
                ?: ExclusiveVersesScreenKind.Dua
        }

        setContent {
            QuranAppTheme {
                ExclusiveVersesListScreen(kind = kind)
            }
        }
    }

}
