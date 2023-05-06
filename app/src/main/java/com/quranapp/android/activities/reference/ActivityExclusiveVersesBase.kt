package com.quranapp.android.activities.reference

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.QuranMetaPossessingActivity
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.utils.extended.GapedItemDecoration

abstract class ActivityExclusiveVersesBase : QuranMetaPossessingActivity() {
    override fun getStatusBarBG(): Int {
        return color(R.color.colorBGHomePageItem)
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return true
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_exclusive_verses
    }

    protected fun initContent(
        binding: ActivityExclusiveVersesBinding,
        verses: List<ExclusiveVerse>,
        titleRes: Int
    ) {
        binding.header.setBGColor(R.color.colorBGHomePageItem)
        binding.header.setTitleText(titleRes)
        binding.header.setCallback {
            onBackPressedDispatcher.onBackPressed()
        }

        initVerses(binding, verses)
    }

    private fun initVerses(binding: ActivityExclusiveVersesBinding, duas: List<ExclusiveVerse>) {
        binding.list.let {
            it.addItemDecoration(GapedItemDecoration(dp2px(3f)))
            it.layoutManager = getLayoutManager()
            it.adapter = getAdapter(this, ViewGroup.LayoutParams.MATCH_PARENT, duas)
        }
    }

    protected open fun getLayoutManager(): RecyclerView.LayoutManager {
        val spanCount = if (WindowUtils.isLandscapeMode(this)) 3 else 2
        return GridLayoutManager(this, spanCount)
    }

    abstract fun getAdapter(
        context: Context,
        width: Int,
        exclusiveVerses: List<ExclusiveVerse>
    ): RecyclerView.Adapter<*>
}
