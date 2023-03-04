package com.quranapp.android.utils.extensions

import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.core.view.children
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView


fun View.alphaDisableView(disable: Boolean) {
    alpha = if (disable) 0.5f else 1f
}

fun View.disableView(disable: Boolean) {
    isEnabled = !disable
    alphaDisableView(disable)
}

fun ViewGroup.disableViewRecursively(disable: Boolean) {
    isEnabled = !disable
    alphaDisableView(disable)

    children.forEach {
        if (it is ViewGroup) {
            it.disableViewRecursively(disable)
        }
    }
}

fun View.removeView() {
    if (parent is ViewGroup) {
        (parent as ViewGroup).removeView(this)
    }
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun RecyclerView.enableSnappingOnRecyclerView(): LinearSnapHelper {
    return LinearSnapHelper().apply {
        attachToRecyclerView(this@enableSnappingOnRecyclerView)
    }
}

fun getDeepChildOffset(mainParent: ViewGroup, parent: ViewParent, child: View, accumulatedOffset: Point) {
    val parentGroup = parent as ViewGroup
    accumulatedOffset.y += child.top

    child.layoutParams.safeCastTo<ViewGroup.MarginLayoutParams> {
        accumulatedOffset.y -= topMargin
    }

    if (parentGroup == mainParent) {
        return
    }
    getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
}

fun clipChildren(v: View, clip: Boolean) {
    if (v.parent == null) {
        return
    }
    if (v is ViewGroup) {
        v.clipChildren = clip
        v.setClipToOutline(clip)
    }
    if (v.parent is View) {
        clipChildren(v.parent as View, clip)
    }
}

fun getRelativeTopRecursive(view: View?, till: Class<*>, inclusiveTill: Boolean = false): Int {
    if (view == null) {
        return 0
    }
    return if (till.name.equals(view.javaClass.name, ignoreCase = true)) {
        if (inclusiveTill) {
            view.top
        } else 0
    } else {
        val top = view.top
        val parent = view.parent
        if (parent !is View || parent === view.rootView) {
            top
        } else top + getRelativeTopRecursive(parent as View, till, inclusiveTill)
    }
}

fun View.getLayoutInflater(): LayoutInflater = LayoutInflater.from(context)