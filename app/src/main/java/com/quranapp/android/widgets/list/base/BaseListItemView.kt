package com.quranapp.android.widgets.list.base

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.utils.extensions.updateMarginHorizontal

@SuppressLint("ViewConstructor")
class BaseListItemView(context: Context, val item: BaseListItem) : FrameLayout(context) {
    val containerView = LinearLayout(resolveItemStyle())
    val labelCont = LinearLayout(context)

    var iconView: AppCompatImageView? = null
    var labelView: AppCompatTextView? = null
    var messageView: AppCompatTextView? = null

    init {
        initThis()
        initContainer()
        initIconView()
        initLabelCont()

        measure(0, 0)
    }

    private fun initThis() {
        updateDisability()
    }

    private fun initContainer() {
        containerView.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        addView(containerView)
    }

    private fun initIconView() {
        if (item.icon == 0) return

        val iconDrawable = context.drawable(item.icon)

        iconView = AppCompatImageView(resolveItemIconStyle()).apply {
            setImageDrawable(iconDrawable)

            val size = context.dp2px(40f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.TOP
            }
        }

        updateIcon()
        containerView.addView(iconView)
    }

    private fun initLabelCont() {
        labelCont.apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                updateMarginHorizontal(Dimen.dp2px(context, 10f))
                gravity = Gravity.CENTER_VERTICAL
            }
            orientation = LinearLayout.VERTICAL
        }

        initLabelView()
        initMessageView()

        containerView.addView(labelCont)
    }

    private fun initLabelView() {
        if (labelView != null || item.label.isNullOrEmpty()) return

        labelView = AppCompatTextView(resolveItemLabelStyle())
        updateLabel()

        labelCont.addView(labelView)
    }

    private fun initMessageView() {
        if (messageView != null || item.message.isNullOrEmpty()) {
            return
        }

        messageView = AppCompatTextView(resolveItemMassageStyle())

        /*mMessageView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorBodyTer1Text));
        mMessageView.setTextSize(Dimen.getDimenSp(getContext(), R.dimen.dmnCommonSizeTer));
        mMessageView.setMaxLines(2);
        mMessageView.setEllipsize(TextUtils.TruncateAt.END);*/

        updateDescription()
        labelCont.addView(messageView)
    }

    fun updateIcon() {
        if (item.icon == 0) {
            iconView.removeView()
            return
        }

        if (iconView == null) {
            initIconView()
            return
        }

        iconView?.setImageDrawable(context.drawable(item.icon))
    }

    fun updateLabel() {
        if (labelView == null) {
            initMessageView()
            return
        }

        if (item.label.isNullOrEmpty()) {
            labelView.removeView()
        } else {
            labelView!!.text = item.label
        }
    }

    private fun updateDescription() {
        if (messageView == null) {
            initMessageView()
            return
        }

        if (item.message.isNullOrEmpty()) {
            messageView.removeView()
        } else {
            messageView!!.text = item.message
        }
    }

    fun updateDisability() {
        isEnabled = item.enabled
        alpha = if (item.enabled) 1f else 0.5f
    }

    fun notifyForChange() {
        updateIcon()
        updateLabel()
        updateDescription()
        updateDisability()
        isSelected = item.selected
    }

    fun setItemBackground(background: Drawable?) {
        containerView.background = background
    }

    fun setItemBackground(@DrawableRes backgroundRes: Int) {
        containerView.setBackgroundResource(backgroundRes)
    }

    private fun resolveItemStyle(): Context {
        return ContextThemeWrapper(context, R.style.SimpleListItemStyle)
    }

    private fun resolveItemIconStyle(): Context {
        return ContextThemeWrapper(context, R.style.SimpleListItemIconStyle)
    }

    private fun resolveItemLabelStyle(): Context {
        return ContextThemeWrapper(context, R.style.SimpleListItemLabelStyle)
    }

    private fun resolveItemMassageStyle(): Context {
        return ContextThemeWrapper(context, R.style.SimpleListItemMessageStyle)
    }
}
