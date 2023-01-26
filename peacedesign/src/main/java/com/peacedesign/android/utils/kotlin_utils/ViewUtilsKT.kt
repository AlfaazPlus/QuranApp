@file:Suppress("NOTHING_TO_INLINE")

package com.peacedesign.android.utils.kotlin_utils

import android.graphics.Point
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.ResUtils
import com.peacedesign.android.utils.touchutils.HoverOpacityEffect
import com.peacedesign.android.utils.touchutils.HoverPushOpacityEffect

public inline fun View.setPaddings(padding: Int) {
    setPadding(padding, padding, padding, padding)
}


public inline fun View.setPaddings(horizontalPadding: Int, verticalPadding: Int) {
    setPaddingHorizontal(horizontalPadding)
    setPaddingVertical(verticalPadding)
}

public inline fun View.setPaddingHorizontal(padding: Int) {
    setPadding(padding, paddingTop, padding, paddingBottom)
}

public inline fun View.setPaddingHorizontal(paddingStart: Int, paddingEnd: Int) {
    setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom)
}

public inline fun View.setPaddingVertical(padding: Int) {
    setPadding(ViewCompat.getPaddingStart(this), padding, ViewCompat.getPaddingEnd(this), padding)
}

public inline fun View.setPaddingVertical(paddingTop: Int, paddingBottom: Int) {
    setPadding(ViewCompat.getPaddingStart(this), paddingTop, ViewCompat.getPaddingEnd(this), paddingBottom)
}

public inline fun View.setPaddingStart(padding: Int) {
    setPadding(padding, paddingTop, ViewCompat.getPaddingEnd(this), paddingBottom)
}

public inline fun View.setPaddingTop(padding: Int) {
    setPadding(ViewCompat.getPaddingStart(this), padding, ViewCompat.getPaddingEnd(this), paddingBottom)
}

public inline fun View.setPaddingEnd(padding: Int) {
    setPadding(ViewCompat.getPaddingStart(this), paddingTop, padding, paddingBottom)
}

public inline fun View.setPaddingBottom(padding: Int) {
    setPadding(ViewCompat.getPaddingStart(this), paddingTop, ViewCompat.getPaddingEnd(this), padding)
}

public inline fun ViewGroup.MarginLayoutParams.setMargins(margin: Int) {
    setMargins(margin, margin, margin, margin)
}

public inline fun ViewGroup.MarginLayoutParams.setMargins(marginH: Int, marginV: Int) {
    setMargins(marginH, marginV, marginH, marginV)
}

public inline fun ViewGroup.MarginLayoutParams.setMarginHorizontal(margin: Int) {
    MarginLayoutParamsCompat.setMarginStart(this, margin)
    MarginLayoutParamsCompat.setMarginEnd(this, margin)
}

public inline fun ViewGroup.MarginLayoutParams.setMarginVertical(margin: Int) {
    topMargin = margin
    bottomMargin = margin
}

public inline fun View.alphaDisableView(disable: Boolean) {
    alpha = if (disable) 0.5f else 1f
}

public inline fun View.disableView(disable: Boolean) {
    isEnabled = !disable
    alphaDisableView(disable)
}

public fun ViewGroup.disableViewRecursively(disable: Boolean) {
    isEnabled = !disable
    alphaDisableView(disable)

    children.forEach {
        if (it is ViewGroup) {
            it.disableViewRecursively(disable)
        }
    }
}

public inline fun View.removeView() {
    if (parent is ViewGroup) {
        (parent as ViewGroup).removeView(this)
    }
}

public inline fun View.gone() {
    visibility = View.GONE
}

public inline fun View.visible() {
    visibility = View.VISIBLE
}

public inline fun View.invisible() {
    visibility = View.INVISIBLE
}

public inline fun RecyclerView.enableSnappingOnRecyclerView(): LinearSnapHelper {
    val snapHelper = LinearSnapHelper()
    snapHelper.attachToRecyclerView(this)
    return snapHelper
}

fun addHoverPushOpacityEffect(vararg buttons: View) {
    for (button in buttons) {
        button.setOnTouchListener(HoverPushOpacityEffect())
    }
}

fun addHoverOpacityEffect(vararg buttons: View) {
    for (button in buttons) {
        button.setOnTouchListener(HoverOpacityEffect())
    }
}

public inline fun HorizontalScrollView.centerInHorizontalScrollView(child: View) {
    val center = scrollX + width / 2
    val left = child.left
    val childWidth = child.width
    if (center >= left && center <= left + childWidth) {
        scrollBy(left + childWidth / 2 - center, 0)
    }
}

inline fun ScrollView.scrollToViewVertically(view: View) {
    val childOffset = Point()
    getDeepChildOffset(this, view.parent, view, childOffset)
    this.smoothScrollTo(0, childOffset.y)
}

inline fun NestedScrollView.scrollToViewVertically(view: View) {
    val childOffset = Point()
    getDeepChildOffset(this, view.parent, view, childOffset)
    this.smoothScrollTo(0, childOffset.y)
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

inline fun View.addStrokedBGToHeader(@ColorRes bgColorRes: Int, @ColorRes borderColorRes: Int) {
    val ctx = context
    val bgColor = ContextCompat.getColor(ctx, bgColorRes)
    val borderColor = ContextCompat.getColor(ctx, borderColorRes)
    val strokeWidths = DimenKT.createBorderWidthsForBG(bottom = ctx.dp2px(1f))
    val bg = DrawableUtils.createBackgroundStroked(bgColor, borderColor, strokeWidths, null)
    background = bg
}

public inline fun TextView.setTextSizePx(@DimenRes dimenResId: Int) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDimenPx(context, dimenResId).toFloat())
}

public inline fun TextView.setTextColorResource(@ColorRes colorResId: Int) {
    setTextColor(ContextCompat.getColor(context, colorResId))
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