package com.quranapp.android.activities.reference

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.reference.OntologyExplorerScreen
import com.quranapp.android.compose.screens.reference.ThematicTopicsScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivityQuranicTopics : BaseActivity() {
    enum class TopicsScreenType() {
        ONTOLOGY_EXPLORER,
        THEMATIC_TOPICS,
    }

    companion object {
        const val KEY_SCREEN_TYPE = "screen_type"
    }


    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val screenType =
            intent.getStringExtra(KEY_SCREEN_TYPE)?.let { TopicsScreenType.valueOf(it) }

        setContent {
            QuranAppTheme {
                if (screenType == TopicsScreenType.ONTOLOGY_EXPLORER) {
                    OntologyExplorerScreen()
                } else {
                    ThematicTopicsScreen()
                }
            }
        }
    }
}
