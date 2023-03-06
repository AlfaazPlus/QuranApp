/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.TextAppearanceSpan
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.*

open class PageAlert : LinearLayout {
    private lateinit var mIconView: AppCompatImageView
    private lateinit var mTitleView: AppCompatTextView
    private lateinit var mBtnView: AppCompatTextView
    private var mActionListener: Runnable? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        initThis()

        mIconView = createIcon(context)
        mTitleView = createTitle(context)
        mBtnView = createButton(context)

        addView(mIconView.apply { visibility = GONE })
        addView(mTitleView.apply { visibility = GONE })
        addView(mBtnView.apply { visibility = GONE })

        mBtnView.setOnClickListener { mActionListener?.run() }
    }

    private fun initThis() {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        updatePaddings(context.dp2px(25F), context.dp2px(50F))

        setBackgroundColor(context.obtainWindowBackgroundColor())
        setOnTouchListener { _, _ -> true }
    }

    protected fun createIcon(context: Context): AppCompatImageView {
        val icon = AppCompatImageView(context).apply {
            val dimen = Dimen.dp2px(context, 50F)
            layoutParams = LayoutParams(dimen, dimen).apply {
                bottomMargin = Dimen.dp2px(context, 15F)
            }
        }
        return icon
    }

    protected fun createTitle(context: Context): AppCompatTextView {
        val title = AppCompatTextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                bottomMargin = Dimen.dp2px(context, 15F)
            }
        }
        return title
    }

    protected fun createButton(context: Context): AppCompatTextView {
        val btn = AppCompatTextView(ContextThemeWrapper(context, R.style.ButtonAction)).apply {
            minWidth = Dimen.dp2px(context, 150F)
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                topMargin = Dimen.dp2px(context, 5F)
            }
        }
        return btn
    }

    fun setIcon(iconRes: Int) {
        setIcon(ContextCompat.getDrawable(context, iconRes))
    }

    fun setIcon(iconDrawable: Drawable?) {
        mIconView.setImageDrawable(iconDrawable)
        mIconView.visibility = if (iconDrawable == null) GONE else VISIBLE
    }

    fun setMessage(titleRes: Int, msgRes: Int) {
        setMessage(context.getString(titleRes), context.getString(msgRes))
    }

    fun setMessage(title: CharSequence?, msg: CharSequence?) {
        setMessageSpannable(prepareMessage(title, msg))
    }

    fun setMessageSpannable(titlePlusMsg: CharSequence?) {
        mTitleView.text = titlePlusMsg
        mTitleView.visibility = if (TextUtils.isEmpty(titlePlusMsg)) GONE else VISIBLE
    }

    private fun prepareMessage(title: CharSequence?, msg: CharSequence?): CharSequence {
        val ssb = SpannableStringBuilder()

        val hasTitle = !TextUtils.isEmpty(title)
        if (hasTitle) {
            ssb.append(prepareTitleSpannable(title))
        }

        if (!TextUtils.isEmpty(msg)) {
            ssb.append(prepareMsgSpannable(msg, hasTitle))
        }

        return ssb
    }

    fun prepareMsgSpannable(msg: CharSequence?, hasTitle: Boolean): CharSequence {
        val msgSS = SpannableString(msg)
        val txtSize = context.getDimenPx(R.dimen.dmnCommonSize)
        val txtColor = ColorStateList.valueOf(context.color(R.color.colorText2))
        val span = TextAppearanceSpan("sans-serif", Typeface.NORMAL, txtSize, txtColor, null)
        msgSS.setSpan(span, 0, msgSS.length, SPAN_EXCLUSIVE_EXCLUSIVE)

        if (hasTitle) {
            val nlSS = SpannableString("\n\n")
            nlSS.setSpan(AbsoluteSizeSpan(10), 0, nlSS.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            return TextUtils.concat(nlSS, msgSS)
        }

        return msgSS
    }

    fun prepareTitleSpannable(title: CharSequence?): CharSequence {
        val titleSS = SpannableString(title)
        val txtSize = context.getDimenPx(R.dimen.dmnCommonSizeLarger)

        val span = TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSize, null, null)
        titleSS.setSpan(span, 0, titleSS.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        return titleSS
    }

    fun setActionButton(textRes: Int, actionListener: Runnable?) {
        setActionButton(context.getString(textRes), actionListener)
    }

    fun setActionButton(text: CharSequence?, actionListener: Runnable?) {
        mBtnView.text = text
        mActionListener = actionListener

        mBtnView.visibility = if (TextUtils.isEmpty(text)) GONE else VISIBLE
    }

    fun show(container: ViewGroup) {
        visibility = VISIBLE

        if (parent != null && parent == container) {
            return
        }

        remove()
        container.addView(this)
    }

    fun hide() {
        visibility = GONE
    }

    fun remove() {
        removeView()
    }

    fun setupForNoInternet(actionListener: Runnable?) {
        setIcon(R.drawable.dr_icon_no_internet)
        setMessage(
            context.getString(R.string.strTitleNoInternet),
            context.getString(R.string.strMsgNoInternetLong)
        )
        setActionButton(context.getString(R.string.strLabelRetry), actionListener)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        params?.width = MATCH_PARENT
        params?.height = MATCH_PARENT

        if (params is RelativeLayout.LayoutParams) {
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        }
        super.setLayoutParams(params)
    }
}
