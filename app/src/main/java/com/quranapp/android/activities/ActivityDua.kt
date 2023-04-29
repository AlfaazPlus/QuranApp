package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.adapters.ADPDua
import com.quranapp.android.components.quran.QuranDua
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.VerseReference
import com.quranapp.android.databinding.ActivityDuaBinding
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.extended.GapedItemDecoration

class ActivityDua : BaseActivity() {
    override fun getStatusBarBG(): Int {
        return color(R.color.colorBGHomePageItem)
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return true
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_dua
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityDuaBinding.bind(activityView)

        QuranMeta.prepareInstance(this, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                QuranDua.prepareInstance(this@ActivityDua, r) { duas ->
                    initContent(binding, duas)
                }
            }
        })
    }

    private fun initContent(binding: ActivityDuaBinding, duas: List<VerseReference>) {
        binding.header.setBGColor(R.color.colorBGHomePageItem)
        binding.header.setTitleText(R.string.strTitleFeaturedDuas)
        binding.header.setCallback {
            onBackPressedDispatcher.onBackPressed()
        }

        initDuas(binding, duas)
    }

    private fun initDuas(binding: ActivityDuaBinding, duas: List<VerseReference>) {
        val spanCount = if (WindowUtils.isLandscapeMode(this)) 3 else 2

        binding.list.let {
            it.addItemDecoration(GapedItemDecoration(dp2px(3f)))
            it.layoutManager = GridLayoutManager(this, spanCount)
            it.adapter = ADPDua(this, ViewGroup.LayoutParams.MATCH_PARENT, duas)
        }
    }
}