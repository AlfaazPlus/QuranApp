package com.quranapp.android.activities.reference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.reference.ExclusiveVersesListScreen
import com.quranapp.android.compose.screens.reference.ExclusiveVersesScreenKind
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.univ.Keys

class ActivityExclusiveVerses : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

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

    companion object {
        fun intent(context: Context, kind: ExclusiveVersesScreenKind): Intent =
            Intent(context, ActivityExclusiveVerses::class.java).apply {
                putExtra(Keys.KEY_EXCLUSIVE_VERSES_KIND, kind.name)
            }
    }
}
