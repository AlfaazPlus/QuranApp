package com.quranapp.android.activities.reference

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.ADPQuranScience
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.views.BoldHeader
import org.json.JSONArray

class ActivityQuranScienceContent : BaseActivity() {
    override fun getLayoutResource() = R.layout.activity_exclusive_verses

    override fun shouldInflateAsynchronously() = true

    @SuppressLint("DiscouragedApi")
    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityExclusiveVersesBinding.bind(activityView)

        binding.header.setCallback { onBackPressedDispatcher.onBackPressed() }
    }
}