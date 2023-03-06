package com.quranapp.android.widgets.list.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.peacedesign.android.utils.ArrayListPro
import com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType

open class BaseListAdapter(val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val items = ArrayListPro<BaseListItem>()
    private val animationDuration = 300L

    private var itemsAnimatable = true

    var container: ViewGroup? = null
    var listView: BaseListView? = null

    init {
        items.addObserver { _, updateType ->
            if (updateType == UpdateType.REMOVE || updateType == UpdateType.ADD) {
                for (i in items.indices) {
                    items[i].position = i
                }
            }
        }
    }

    open fun onCreateItem(item: BaseListItem, index: Int) {
        val itemView = onCreateItemView(item, index) ?: return

        handler.post { itemView.isSelected = item.selected }

        itemView.setOnClickListener { dispatchClick(item) }

        item.itemView = itemView
        item.adapter = this

        getContainerInternal()?.let {
            onAppendItemView(it, itemView, index)
        }
    }

    protected open fun onCreateItemView(item: BaseListItem, position: Int): View? {
        return BaseListItemView(context, item)
    }

    protected open fun onAppendItemView(container: ViewGroup, itemView: View, position: Int) {
        container.addView(itemView, position)
    }

    fun drainItems() {
        items.clear()
        container?.removeAllViews()
    }

    fun setItems(items: ArrayList<BaseListItem>) {
        val oldAnimateItemsFlag = itemsAnimatable

        itemsAnimatable = false
        drainItems()
        itemsAnimatable = oldAnimateItemsFlag

        this.items.addAll(items)

        listView?.onCreate()
    }

    fun addItem(item: BaseListItem) {
        addItem(item, items.size)
    }

    fun addItem(item: BaseListItem, index: Int) {
        item.position = index

        items.add(index, item)
        if (isLaidOut()) {
            onCreateItem(item, index)

            if (itemsAnimatable) {
                val itemView = item.itemView
                if (itemView != null) {
                    addViewWithAnimation(itemView)
                }
            }
        }
    }

    private fun isLaidOut(): Boolean {
        return container?.isLaidOut == true
    }

    fun removeItem(index: Int) {
        removeItem(items[index])
    }

    fun removeItem(item: BaseListItem) {
        item.itemView?.let {
            remove(it, item, itemsAnimatable)
        }
    }

    private fun remove(view: View, item: BaseListItem, animate: Boolean) {
        if (animate) removeWithAnimation(view, item) else removeFinal(view, item)
    }

    fun removeFinal(view: View, item: BaseListItem) {
        (view.parent as ViewGroup).removeView(view)
        removeMenuItemInternal(item)
    }

    private fun removeMenuItemInternal(item: BaseListItem) {
        items.remove(item)
    }

    private fun removeWithAnimation(view: View, item: BaseListItem) {
        val params = view.layoutParams

        val alphaAnimator = ValueAnimator().apply {
            setFloatValues(view.alpha, 0f)
            addUpdateListener { animation -> view.alpha = animation.animatedValue as Float }
        }

        val heightAnimator = ValueAnimator().apply {
            setIntValues(view.height, 0)
            addUpdateListener { animation ->
                params.height = animation.animatedValue as Int
                view.requestLayout()
            }
        }

        val animatorSet = AnimatorSet()
        animatorSet.play(heightAnimator).with(alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                removeFinal(view, item)
            }
        })
        animatorSet.setDuration(animationDuration).start()
    }

    private fun addViewWithAnimation(itemView: View) {
        itemView.measure(0, 0)

        val params = itemView.layoutParams.apply {
            height = 0
        }

        val alphaAnimator = ValueAnimator().apply {
            setFloatValues(0f, itemView.alpha)
            addUpdateListener { animation ->
                itemView.alpha = animation.animatedValue as Float
            }
        }

        val heightAnimator = ValueAnimator().apply {
            setIntValues(0, itemView.measuredHeight)
            addUpdateListener { animation ->
                params.height = animation.animatedValue as Int
                itemView.requestLayout()
            }
        }

        val animatorSet = AnimatorSet()
        animatorSet.play(heightAnimator).with(alphaAnimator)
        animatorSet.setDuration(animationDuration).start()
    }

    fun getItem(index: Int): BaseListItem {
        val count = itemsCount()
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException(
                String.format(
                    "Adapter indexOutOfBound. Index: %d, Size: %d ",
                    index,
                    count
                )
            )
        }

        return items[index]
    }

    fun itemsCount() = items.size

    private fun getContainerInternal() = container

    protected open fun dispatchClick(item: BaseListItem) {
        listView?.dispatchItemClick(item)
    }

    fun notifyItemChanged(position: Int) {
        val itemView = getItem(position).itemView
        if (itemView is BaseListItemView) {
            itemView.notifyForChange()
        }
    }
}
