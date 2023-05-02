package com.quranapp.android.activities.reference

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.QuranMetaPossessingActivity
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.extensions.getDimenPx

abstract class ActivityExclusiveVersesBase : QuranMetaPossessingActivity() {
    private var txtSize: Int = 0
    private var txtSizeName: Int = 0
    private lateinit var titleColor: ColorStateList
    private lateinit var infoColor: ColorStateList

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
        duas: List<ExclusiveVerse>,
        titleRes: Int
    ) {
        binding.header.setBGColor(R.color.colorBGHomePageItem)
        binding.header.setTitleText(titleRes)
        binding.header.setCallback {
            onBackPressedDispatcher.onBackPressed()
        }

        txtSize = getDimenPx(R.dimen.dmnCommonSize2)
        txtSizeName = getDimenPx(R.dimen.dmnCommonSizeLarge)
        titleColor = ColorStateList.valueOf(color(R.color.white))
        infoColor = ColorStateList.valueOf(Color.parseColor("#D0D0D0"))

        initVerses(binding, duas)
    }

    private fun initVerses(binding: ActivityExclusiveVersesBinding, duas: List<ExclusiveVerse>) {
        val spanCount = if (WindowUtils.isLandscapeMode(this)) 3 else 2

        binding.list.let {
            it.addItemDecoration(GapedItemDecoration(dp2px(3f)))
            it.layoutManager = GridLayoutManager(this, spanCount)
            it.adapter = getAdapter(this, ViewGroup.LayoutParams.MATCH_PARENT, duas)
        }
    }

    abstract fun getAdapter(
        context: Context,
        width: Int,
        exclusiveVerses: List<ExclusiveVerse>
    ): RecyclerView.Adapter<*>
}
