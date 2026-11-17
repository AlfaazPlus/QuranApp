package com.quranapp.android.activities.reference

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.models.QuranicTopicsStart
import com.quranapp.android.screens.quranicTopics.QuranicTopicsScreen
import com.quranapp.android.theme.QuranAppTheme
import com.quranapp.android.utils.miscellenous.Keys

class ActivityQuranicTopics : BaseActivity() {

    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val screenType = intent.getStringExtra(Keys.KEY_QURANIC_TOPICS_SCREEN_TYPE)?.let {
            runCatching { QuranicTopicsStart.valueOf(it) }.getOrNull()
        } ?: QuranicTopicsStart.Ontology
        val initialTopicId = intent.getIntExtra(Keys.KEY_QURANIC_TOPICS_TOPIC_ID, -1)
            .takeIf { it > 0 }
        val initialTrail = intent.getStringExtra(Keys.KEY_QURANIC_TOPICS_TOPIC_TRAIL)
            .orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }

        setContent {
            QuranAppTheme {
                QuranicTopicsScreen(
                    start = screenType,
                    initialTopicId = initialTopicId,
                    initialTrail = initialTrail,
                )
            }
        }
    }
}
