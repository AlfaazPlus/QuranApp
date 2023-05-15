package com.quranapp.android.views.homepage2

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytHomepageTitledItemTitleBinding
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.utils.extensions.updateMarginVertical
import com.quranapp.android.utils.extensions.updatePaddingVertical
import com.quranapp.android.utils.extensions.updatePaddings
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect

abstract class HomepageCollectionLayoutBase @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    @CallSuper
    open fun initialize() {
        initThis()
        initTitle()
    }

    private fun initThis() {
        orientation = VERTICAL
        setBackgroundColor(context.color(R.color.colorBGHomePageItem))
        updatePaddingVertical(context.dp2px(10f))
    }

    private fun initTitle() {
        val binding = LytHomepageTitledItemTitleBinding.inflate(LayoutInflater.from(context))
        val headerIcon = getHeaderIcon()
        if (headerIcon != 0) {
            binding.titleIcon.setImageResource(headerIcon)
        }

        val headerTitle = getHeaderTitle()
        if (headerTitle != 0) {
            binding.titleText.setText(headerTitle)
        }

        if (showViewAllBtn()) {
            binding.viewAll.let {
                it.visibility = VISIBLE
                it.setOnTouchListener(HoverPushOpacityEffect())
                it.setOnClickListener { onViewAllClick() }
            }
        } else {
            binding.viewAll.visibility = GONE
        }

        setupHeader(binding)
        addView(binding.root, 0)
    }


    protected open fun setupHeader(header: LytHomepageTitledItemTitleBinding) {}

    abstract fun refresh(quranMeta: QuranMeta)
    protected open fun showViewAllBtn(): Boolean {
        return true
    }


    protected open fun onViewAllClick() {}

    @StringRes
    protected abstract fun getHeaderTitle(): Int

    @DrawableRes
    protected abstract fun getHeaderIcon(): Int


    protected open fun showLoader() {
        if (findViewById<View?>(R.id.loader) != null) {
            return
        }

        val size = context.dp2px(35f)
        addView(
            ProgressBar(context).apply {
                id = R.id.loader
                updatePaddings(context.dp2px(15f))
            },
            LayoutParams(size, size)
        )
    }

    protected open fun hideLoader() {
        findViewById<View?>(R.id.loader)?.removeView()
    }


    @CallSuper
    protected open fun resolveListView(): RecyclerView {
        var list = findViewById<RecyclerView>(R.id.list)

        if (list == null) {
            list = makeRecView(context)
            addView(list)
        }

        return list
    }

    private fun makeRecView(ctx: Context): RecyclerView? {
        return RecyclerView(ctx).apply {
            updatePaddings(Dimen.dp2px(ctx, 10f))

            id = R.id.list
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false)
            addItemDecoration(GapedItemDecoration(Dimen.dp2px(ctx, 5f)))
        }
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        if (params is MarginLayoutParams) {
            params.updateMarginVertical(Dimen.dp2px(context, 3f))
        }
        super.setLayoutParams(params)
    }
}