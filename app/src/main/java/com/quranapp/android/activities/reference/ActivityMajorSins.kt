package com.quranapp.android.activities.reference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.adapters.reference.ADPMajorSins
import com.quranapp.android.adapters.reference.ADPSolutionVerses
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.QuranMajorSins
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.SituationVerse
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding

class ActivityMajorSins : ActivityExclusiveVersesBase() {
    override fun onQuranMetaReady(
        activityView: View,
        intent: Intent,
        savedInstanceState: Bundle?,
        quranMeta: QuranMeta
    ) {
        QuranMajorSins.prepareInstance(this, quranMeta) { references ->
            initContent(ActivityExclusiveVersesBinding.bind(activityView), references, R.string.strTitleMajorSins)
        }
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(this)
    }

    override fun getAdapter(
        context: Context,
        width: Int,
        exclusiveVerses: List<ExclusiveVerse>
    ): RecyclerView.Adapter<*> {
        return ADPMajorSins(context, ViewGroup.LayoutParams.WRAP_CONTENT,width, exclusiveVerses)
    }
}