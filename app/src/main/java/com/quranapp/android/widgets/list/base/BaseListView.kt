package com.quranapp.android.widgets.list.base

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updatePaddingVertical

open class BaseListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {
    companion object {
        const val DEFAULT_ITEM_ANIMATOR = -1
    }

    val container = createItemsContainer(context)
    private var mAdapter: BaseListAdapter? = null
    private var onItemClickListener: OnItemClickListener? = null
    var supportsShowAnimation = false
    var itemAnimator = DEFAULT_ITEM_ANIMATOR

    init {
        isVerticalScrollBarEnabled = false
        clipToPadding = false
        addView(container)
    }

    protected open fun createItemsContainer(context: Context): ViewGroup {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            updatePaddingVertical(context.dp2px(10f))
        }
    }

    fun getItem(position: Int): BaseListItem? {
        return mAdapter?.getItem(position)
    }

    open fun onCreate() {
        create()
    }

    private fun create() {
        container.removeAllViews()

        if (mAdapter == null) return

        setContainer(container)

        for (i in 0 until mAdapter!!.itemsCount()) {
            onCreateItem(mAdapter!!.getItem(i), i)
        }
    }

    protected open fun onCreateItem(item: BaseListItem, position: Int) {
        mAdapter?.onCreateItem(item, position)
        val itemView = item.itemView ?: return

        if (supportsShowAnimation) {
            initPendingItemAnimation(itemView, position)
        }
    }

    protected open fun setContainer(container: ViewGroup) {
        mAdapter?.container = container
    }

    fun getAdapter(): BaseListAdapter? {
        return mAdapter
    }

    fun setAdapter(adapter: BaseListAdapter?) {
        mAdapter = adapter

        adapter?.listView = this

        onCreate()
    }

    private fun resolveAnimation(): Animation? {
        return if (itemAnimator == 0) {
            null
        } else {
            try {
                AnimationUtils.loadAnimation(context, itemAnimator)
            } catch (e: Exception) {
                AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            }
        }
    }

    private fun initPendingItemAnimation(itemView: View, itemIndex: Int) {
        val animation = resolveAnimation() ?: return

        animation.apply {
            fillAfter = true
            fillBefore = true
            startOffset = (itemIndex * 40).toLong()
            duration = 300
        }

        itemView.post { itemView.startAnimation(animation) }
    }

    open fun dispatchItemClick(item: BaseListItem) {
        onItemClickListener?.onItemClick(item)
    }

    open fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(item: BaseListItem)
    }
}
