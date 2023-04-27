package com.quranapp.android.widgets.compound

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import com.peacedesign.android.utils.span.TypefaceSpan2
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.utils.extensions.sp2px

abstract class PeaceCompoundButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Checkable, CompoundButton.OnCheckedChangeListener {
    companion object {
        const val COMPOUND_TEXT_LEFT = 0
        const val COMPOUND_TEXT_TOP = 1
        const val COMPOUND_TEXT_RIGHT = 2
        const val COMPOUND_TEXT_BOTTOM = 3

        const val COMPOUND_TEXT_GRAVITY_START = 0
        const val COMPOUND_TEXT_GRAVITY_END = 1
        const val COMPOUND_TEXT_GRAVITY_CENTER = 2
        const val COMPOUND_TEXT_GRAVITY_LEFT = 3
        const val COMPOUND_TEXT_GRAVITY_RIGHT = 4
    }

    protected var initialChecked = false

    @TextGravity
    private var textGravity = 0
    private var parent: PeaceCompoundButtonGroup? = null
    private var text: CharSequence? = null
    private var subText: CharSequence? = null

    private var compoundDirection = 0
    private var spaceBetween = 0
    private var textAppearanceResId = 0
    private var subTextAppearanceResId = 0

    var beforeCheckChangeListener: ((PeaceCompoundButton, Boolean) -> Boolean)? = null
    var onCheckChangedListener: ((PeaceCompoundButton, Boolean) -> Unit)? = null

    private var txtView: AppCompatTextView? = null

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.PeaceCompoundButton,
            defStyleAttr,
            0
        )

        text = a.getText(R.styleable.PeaceCompoundButton_android_text)
        subText = a.getText(R.styleable.PeaceCompoundButton_peaceComp_subText)
        initialChecked = a.getBoolean(R.styleable.PeaceCompoundButton_android_checked, false)
        compoundDirection = a.getInt(
            R.styleable.PeaceCompoundButton_peaceComp_direction,
            COMPOUND_TEXT_RIGHT
        )
        spaceBetween = a.getDimensionPixelSize(
            R.styleable.PeaceCompoundButton_peaceComp_spaceBetween,
            context.dp2px(10f)
        )
        textGravity = a.getInt(
            R.styleable.PeaceCompoundButton_peaceComp_textGravity,
            COMPOUND_TEXT_GRAVITY_START
        )
        textAppearanceResId = a.getResourceId(
            R.styleable.PeaceCompoundButton_android_textAppearance,
            R.style.PeaceRadioTextAppearance
        )
        subTextAppearanceResId = a.getResourceId(
            R.styleable.PeaceCompoundButton_peaceComp_subTextAppearance,
            R.style.PeaceRadioSubTextAppearance
        )

        a.recycle()
        init()
    }

    private fun init() {
        initThis()
        makeComponents()
    }

    @CallSuper
    protected open fun initThis() {
        super.setOrientation(HORIZONTAL)
        super.setOnClickListener { if (parent != null) parent!!.check(id) else toggle() }
    }

    @CallSuper
    protected open fun makeComponents() {
        makeTexts()
        addCompoundButton()
    }

    private fun makeTexts() {
        txtView = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
            gravity = resolveTextGravity(textGravity)
        }
        setTextAppearancesInternal(textAppearanceResId, subTextAppearanceResId)
    }

    protected open fun addCompoundButton() {
        val compoundButton = getCompoundButton()
        txtView!!.removeView()
        compoundButton?.removeView()

        when (compoundDirection) {
            COMPOUND_TEXT_LEFT, COMPOUND_TEXT_TOP -> {
                addView(txtView, 0)
                if (compoundButton != null) {
                    addView(compoundButton, 1)
                }
            }

            COMPOUND_TEXT_RIGHT, COMPOUND_TEXT_BOTTOM -> {
                if (compoundButton != null) {
                    addView(compoundButton, 0)
                }
                addView(txtView, if (compoundButton != null) 1 else 0)
            }

            else -> {
                if (compoundButton != null) {
                    addView(compoundButton, 0)
                }
                addView(txtView, if (compoundButton != null) 1 else 0)
            }
        }
        if (compoundDirection == COMPOUND_TEXT_TOP || compoundDirection == COMPOUND_TEXT_BOTTOM) {
            super.setOrientation(VERTICAL)
        } else {
            super.setOrientation(HORIZONTAL)
        }
        setSpaceBetweenInternal(spaceBetween)
    }

    abstract fun getCompoundButton(): CompoundButton?

    @SuppressLint("RtlHardcoded")
    private fun resolveTextGravity(@TextGravity textGravity: Int): Int {
        return when (textGravity) {
            COMPOUND_TEXT_GRAVITY_CENTER -> Gravity.CENTER
            COMPOUND_TEXT_GRAVITY_END -> Gravity.END
            COMPOUND_TEXT_GRAVITY_LEFT -> Gravity.LEFT
            COMPOUND_TEXT_GRAVITY_RIGHT -> Gravity.RIGHT
            COMPOUND_TEXT_GRAVITY_START -> Gravity.START
            else -> Gravity.START
        }
    }

    private fun setTextInternal(text: CharSequence?, subtext: CharSequence?) {
        val ssb = SpannableStringBuilder()

        if (!text.isNullOrEmpty()) {
            val titleSS = SpannableString(text)
            setTextAppearanceSpan(titleSS, textAppearanceResId, false)
            ssb.append(titleSS)
        }
        if (!subtext.isNullOrEmpty()) {
            if (!TextUtils.isEmpty(text)) ssb.append("\n")
            val subtitleSS = SpannableString(subtext)
            setTextAppearanceSpan(subtitleSS, subTextAppearanceResId, true)
            ssb.append(subtitleSS)
        }

        txtView!!.text = ssb
        txtView!!.visibility = if (ssb.isNotEmpty()) VISIBLE else GONE
    }

    @SuppressLint("CustomViewStyleable")
    private fun setTextAppearanceSpan(ss: SpannableString, styleId: Int, isSubText: Boolean) {
        val txtSizeDef = context.sp2px(if (isSubText) 14F else 16.toFloat())
        val ta = context.obtainStyledAttributes(styleId, R.styleable.TextAppearance)

        var family = "sans-serif"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val font = ta.getFont(R.styleable.TextAppearance_android_fontFamily)
            if (font != null) {
                ss.setSpan(TypefaceSpan2(font), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                family = resolveFontFamily(
                    ta.getString(R.styleable.TextAppearance_android_fontFamily)
                )
            }
        } else {
            family = resolveFontFamily(ta.getString(R.styleable.TextAppearance_android_fontFamily))
        }

        val style = ta.getInt(
            R.styleable.TextAppearance_android_textStyle,
            if (isSubText) Typeface.NORMAL else Typeface.BOLD
        )

        val size = ta.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, txtSizeDef)
        val color = ta.getColorStateList(R.styleable.TextAppearance_android_textColor)
        val txtApSpan = TextAppearanceSpan(family, style, size, color, null)

        ss.setSpan(txtApSpan, 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ta.recycle()
    }

    private fun resolveFontFamily(s: String?): String {
        return if ("sans-serif".equals(s, ignoreCase = true) ||
            "sans-serif-light".equals(s, ignoreCase = true) ||
            "sans-serif-condensed".equals(s, ignoreCase = true) ||
            "sans-serif-black".equals(s, ignoreCase = true) ||
            "sans-serif-thin".equals(s, ignoreCase = true) ||
            "sans-serif-medium".equals(s, ignoreCase = true)
        ) {
            s!!
        } else {
            "sans-serif"
        }
    }

    private fun setSpaceBetweenInternal(spaceBetween: Int) {
        (txtView!!.layoutParams as LayoutParams).apply {
            setMargins(0, 0, 0, 0)
            marginStart = 0
            marginEnd = 0

            when (compoundDirection) {
                COMPOUND_TEXT_LEFT -> marginEnd = spaceBetween
                COMPOUND_TEXT_TOP -> bottomMargin = spaceBetween
                COMPOUND_TEXT_RIGHT -> marginStart = spaceBetween
                COMPOUND_TEXT_BOTTOM -> topMargin = spaceBetween
            }
        }
        txtView!!.requestLayout()
    }

    private fun setTextAppearancesInternal(textAppearanceResId: Int, subTextAppearanceResId: Int) {
        this.textAppearanceResId = textAppearanceResId
        this.subTextAppearanceResId = subTextAppearanceResId
        setTextInternal(text, subText)
    }

    fun setTextAppearance(@StyleRes styleResId: Int) {
        textAppearanceResId = styleResId
        setTextAppearances(styleResId, subTextAppearanceResId)
    }

    fun setTexts(@StringRes textResId: Int, @StringRes subTextResId: Int) {
        setTexts(context.getText(textResId), context.getText(subTextResId))
    }

    fun setTexts(text: CharSequence?, subText: CharSequence?) {
        this.text = text
        this.subText = subText

        setTextInternal(text, subText)
    }

    fun getText(): CharSequence? {
        return text
    }

    fun setText(@StringRes resId: Int) {
        setText(context.getText(resId))
    }

    fun setText(text: CharSequence) {
        this.text = text
        setTextInternal(text, subText)
    }

    fun getSubText(): CharSequence? {
        return subText
    }

    fun setSubText(@StringRes resId: Int) {
        setSubText(context.getText(resId))
    }

    fun setSubText(subText: CharSequence) {
        this.subText = subText
        setTextInternal(text, subText)
    }

    fun setCompoundDirection(@CompoundDirection compoundDirection: Int) {
        this.compoundDirection = compoundDirection
        addCompoundButton()
    }

    fun setSpaceBetween(spaceBetween: Int) {
        this.spaceBetween = spaceBetween
        setSpaceBetweenInternal(spaceBetween)
    }

    fun setTextAppearances(
        @StyleRes textAppearanceResId: Int,
        @StyleRes subTextAppearanceResId: Int
    ) {
        setTextAppearancesInternal(textAppearanceResId, subTextAppearanceResId)
    }

    fun setSubTextAppearance(@StyleRes styleResId: Int) {
        subTextAppearanceResId = styleResId
        setTextAppearances(textAppearanceResId, styleResId)
    }

    fun setForceTextGravity(@TextGravity gravity: Int) {
        textGravity = gravity
        txtView?.gravity = resolveTextGravity(gravity)
    }

    override fun setTextAlignment(alignment: Int) {
        txtView?.textAlignment = alignment
    }

    fun setGroup(radioGroupPro: PeaceCompoundButtonGroup) {
        parent = radioGroupPro
    }

    override fun isChecked(): Boolean {
        val btn = getCompoundButton() ?: return false
        return btn.isChecked
    }

    override fun setChecked(checked: Boolean) {
        val btn = getCompoundButton() ?: return
        if (beforeCheckChangeListener == null || beforeCheckChangeListener!!.invoke(this, checked)) {
            btn.isChecked = checked
        }
    }

    override fun toggle() {
        val btn = getCompoundButton() ?: return
        if (beforeCheckChangeListener == null || beforeCheckChangeListener!!.invoke(
                this,
                !isChecked
            )
        ) {
            btn.toggle()
        }
    }

    @CallSuper
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        background?.state = intArrayOf(
            if (isChecked) android.R.attr.state_checked else -android.R.attr.state_checked
        )

        onCheckChangedListener?.invoke(this, isChecked)
    }

    override fun setOrientation(orientation: Int) {
        // overridden
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    @IntDef(COMPOUND_TEXT_LEFT, COMPOUND_TEXT_TOP, COMPOUND_TEXT_RIGHT, COMPOUND_TEXT_BOTTOM)
    @Retention(AnnotationRetention.SOURCE)
    annotation class CompoundDirection

    @IntDef(
        COMPOUND_TEXT_GRAVITY_START,
        COMPOUND_TEXT_GRAVITY_END,
        COMPOUND_TEXT_GRAVITY_CENTER,
        COMPOUND_TEXT_GRAVITY_LEFT,
        COMPOUND_TEXT_GRAVITY_RIGHT
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class TextGravity
}
