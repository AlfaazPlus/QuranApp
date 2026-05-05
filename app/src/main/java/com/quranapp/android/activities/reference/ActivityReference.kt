package com.quranapp.android.activities.reference

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.compose.screens.reference.ReferenceScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import kotlinx.coroutines.flow.MutableStateFlow

class ActivityReference : BaseActivity() {
    val intentFlow = MutableStateFlow<Intent?>(null)

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        intentFlow.value = intent

        setContent {
            val currentIntent by intentFlow.collectAsState()

            if (currentIntent == null) {
                return@setContent
            }

            val referenceModel = remember(currentIntent) {
                ReferenceVerseModel.fromBundle(currentIntent!!.extras)
            } ?: return@setContent

            QuranAppTheme {
                ReferenceScreen(referenceModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        intentFlow.value = intent
    }
}
