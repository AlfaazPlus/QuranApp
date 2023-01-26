/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 2/4/2022.
 * All rights reserved.
 */

package com.peacedesign.android.widget.compound;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatTextView;

import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.interfaceUtils.CompoundButtonGroup;
import com.peacedesign.android.utils.span.TypefaceSpan2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class PeaceCompoundButton extends LinearLayout implements Checkable, CompoundButton.OnCheckedChangeListener {
    public static final int COMPOUND_TEXT_LEFT = 0;
    public static final int COMPOUND_TEXT_TOP = 1;
    public static final int COMPOUND_TEXT_RIGHT = 2;
    public static final int COMPOUND_TEXT_BOTTOM = 3;

    public static final int COMPOUND_TEXT_GRAVITY_START = 0;
    public static final int COMPOUND_TEXT_GRAVITY_END = 1;
    public static final int COMPOUND_TEXT_GRAVITY_CENTER = 2;
    public static final int COMPOUND_TEXT_GRAVITY_LEFT = 3;
    public static final int COMPOUND_TEXT_GRAVITY_RIGHT = 4;

    protected final boolean mInitialChecked;
    @TextGravity
    private int mTextGravity;
    private CompoundButtonGroup mParent;
    private CharSequence mText;
    private CharSequence mSubText;
    @CompoundDirection
    private int mCompoundDirection;
    private int mSpaceBetween;
    private int mTextAppearanceResId;
    private int mSubTextAppearanceResId;

    private BeforeCompoundCheckChangeListener mBeforeCheckListener;
    private OnCompoundCheckChangedListener mCheckChangeListener;

    private AppCompatTextView mTxtView;

    public PeaceCompoundButton(Context context) {
        this(context, null);
    }

    public PeaceCompoundButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceCompoundButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceCompoundButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PeaceCompoundButton, defStyleAttr, defStyleRes);

        mText = a.getText(R.styleable.PeaceCompoundButton_android_text);
        mSubText = a.getText(R.styleable.PeaceCompoundButton_peaceComp_subText);
        mInitialChecked = a.getBoolean(R.styleable.PeaceCompoundButton_android_checked, false);
        mCompoundDirection = a.getInt(R.styleable.PeaceCompoundButton_peaceComp_direction, COMPOUND_TEXT_RIGHT);
        mSpaceBetween = a.getDimensionPixelSize(R.styleable.PeaceCompoundButton_peaceComp_spaceBetween, Dimen.dp2px(context, 10));
        mTextGravity = a.getInt(R.styleable.PeaceCompoundButton_peaceComp_textGravity, COMPOUND_TEXT_GRAVITY_START);
        mTextAppearanceResId = a.getResourceId(R.styleable.PeaceCompoundButton_android_textAppearance,
                R.style.PeaceRadioTextAppearance);
        mSubTextAppearanceResId = a.getResourceId(R.styleable.PeaceCompoundButton_peaceComp_subTextAppearance,
                R.style.PeaceRadioSubTextAppearance);

        a.recycle();
        init();
    }

    private void init() {
        initThis();
        makeComponents();
    }

    @CallSuper
    protected void initThis() {
        super.setOrientation(HORIZONTAL);
        super.setOnClickListener(v -> {
            if (mParent != null) mParent.check(getId());
            else toggle();
        });
    }

    @CallSuper
    protected void makeComponents() {
        makeTexts();
        addCompoundButton();
    }

    private void makeTexts() {
        mTxtView = new AppCompatTextView(getContext());
        LayoutParams paramsText = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        paramsText.weight = 1;
        mTxtView.setLayoutParams(paramsText);
        mTxtView.setGravity(resolveTextGravity(mTextGravity));

        setTextAppearancesInternal(mTextAppearanceResId, mSubTextAppearanceResId);
    }

    protected void addCompoundButton() {
        View compoundButton = getCompoundButton();

        ViewUtils.removeView(mTxtView);
        ViewUtils.removeView(compoundButton);

        switch (mCompoundDirection) {
            case COMPOUND_TEXT_LEFT:
            case COMPOUND_TEXT_TOP:
                addView(mTxtView, 0);
                if (compoundButton != null) {
                    addView(compoundButton, 1);
                }
                break;
            case COMPOUND_TEXT_RIGHT:
            case COMPOUND_TEXT_BOTTOM:
            default:
                if (compoundButton != null) {
                    addView(compoundButton, 0);
                }

                addView(mTxtView, compoundButton != null ? 1 : 0);
                break;
        }

        if (mCompoundDirection == COMPOUND_TEXT_TOP || mCompoundDirection == COMPOUND_TEXT_BOTTOM) {
            super.setOrientation(VERTICAL);
        } else {
            super.setOrientation(HORIZONTAL);
        }

        setSpaceBetweenInternal(mSpaceBetween);
    }

    protected abstract CompoundButton getCompoundButton();

    private int resolveTextGravity(@TextGravity int textGravity) {
        switch (textGravity) {
            case COMPOUND_TEXT_GRAVITY_CENTER:
                return Gravity.CENTER;
            case COMPOUND_TEXT_GRAVITY_END:
                return Gravity.END;
            case COMPOUND_TEXT_GRAVITY_LEFT:
                return Gravity.LEFT;
            case COMPOUND_TEXT_GRAVITY_RIGHT:
                return Gravity.RIGHT;
            case COMPOUND_TEXT_GRAVITY_START:
            default:
                return Gravity.START;
        }
    }

    private void setTextInternal(CharSequence text, CharSequence subtext) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        if (!TextUtils.isEmpty(text)) {
            SpannableString titleSS = new SpannableString(text);
            setTextAppearanceSpan(titleSS, mTextAppearanceResId, false);
            ssb.append(titleSS);
        }

        if (!TextUtils.isEmpty(subtext)) {
            if (!TextUtils.isEmpty(text)) ssb.append("\n");

            SpannableString subtitleSS = new SpannableString(subtext);
            setTextAppearanceSpan(subtitleSS, mSubTextAppearanceResId, true);
            ssb.append(subtitleSS);
        }

        mTxtView.setText(ssb);
        mTxtView.setVisibility(ssb.length() > 0 ? VISIBLE : GONE);
    }

    private void setTextAppearanceSpan(SpannableString ss, int styleId, boolean isSubText) {
        int txtSizeDef = Dimen.sp2px(getContext(), isSubText ? 14 : 16);
        TypedArray ta = getContext().obtainStyledAttributes(styleId, androidx.appcompat.R.styleable.TextAppearance);


        String family = "sans-serif";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Typeface font = ta.getFont(androidx.appcompat.R.styleable.TextAppearance_android_fontFamily);
            if (font != null) {
                ss.setSpan(new TypefaceSpan2(font), 0, ss.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                family = resolveFontFamily(ta.getString(androidx.appcompat.R.styleable.TextAppearance_android_fontFamily));
            }
        } else {
            family = resolveFontFamily(ta.getString(androidx.appcompat.R.styleable.TextAppearance_android_fontFamily));
        }


        int style = ta.getInt(androidx.appcompat.R.styleable.TextAppearance_android_textStyle,
                isSubText ? Typeface.NORMAL : Typeface.BOLD);
        int size = ta.getDimensionPixelSize(androidx.appcompat.R.styleable.TextAppearance_android_textSize, txtSizeDef);
        ColorStateList color = ta.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColor);
        TextAppearanceSpan txtApSpan = new TextAppearanceSpan(family, style, size, color, null);
        ss.setSpan(txtApSpan, 0, ss.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        ta.recycle();
    }

    private String resolveFontFamily(String s) {

        if ("sans-serif".equalsIgnoreCase(s) ||
                "sans-serif-light".equalsIgnoreCase(s) ||
                "sans-serif-condensed".equalsIgnoreCase(s) ||
                "sans-serif-black".equalsIgnoreCase(s) ||
                "sans-serif-thin".equalsIgnoreCase(s) ||
                "sans-serif-medium".equalsIgnoreCase(s)) {
            return s;
        }
        return "sans-serif";
    }

    private void setSpaceBetweenInternal(int spaceBetween) {
        LinearLayout.LayoutParams p = (LayoutParams) mTxtView.getLayoutParams();
        p.setMargins(0, 0, 0, 0);
        p.setMarginStart(0);
        p.setMarginEnd(0);

        switch (mCompoundDirection) {
            case COMPOUND_TEXT_LEFT:
                p.setMarginEnd(spaceBetween);
                break;
            case COMPOUND_TEXT_TOP:
                p.bottomMargin = spaceBetween;
                break;
            case COMPOUND_TEXT_RIGHT:
                p.setMarginStart(spaceBetween);
                break;
            case COMPOUND_TEXT_BOTTOM:
                p.topMargin = spaceBetween;
                break;
        }
        mTxtView.setLayoutParams(p);
    }

    private void setTextAppearancesInternal(int textAppearanceResId, int subTextAppearanceResId) {
        mTextAppearanceResId = textAppearanceResId;
        mSubTextAppearanceResId = subTextAppearanceResId;

        setTextInternal(mText, mSubText);
    }

    public void setTextAppearance(@StyleRes int styleResId) {
        mTextAppearanceResId = styleResId;
        setTextAppearances(styleResId, mSubTextAppearanceResId);
    }

    public void setTexts(@StringRes int textResId, @StringRes int subTextResId) {
        setTexts(getContext().getText(textResId), getContext().getText(subTextResId));
    }

    public void setTexts(CharSequence text, CharSequence subText) {
        mText = text;
        mSubText = subText;

        setTextInternal(text, subText);
    }

    public CharSequence getText() {
        return mText;
    }

    public void setText(@StringRes int resId) {
        setText(getContext().getText(resId));
    }

    public void setText(CharSequence text) {
        mText = text;

        setTextInternal(text, mSubText);
    }

    public CharSequence getSubText() {
        return mSubText;
    }

    public void setSubText(@StringRes int resId) {
        setSubText(getContext().getText(resId));
    }

    public void setSubText(CharSequence subText) {
        mSubText = subText;

        setTextInternal(mText, subText);
    }

    public void setCompoundDirection(@CompoundDirection int compoundDirection) {
        mCompoundDirection = compoundDirection;

        addCompoundButton();
    }

    public void setSpaceBetween(int spaceBetween) {
        mSpaceBetween = spaceBetween;
        setSpaceBetweenInternal(spaceBetween);
    }

    public void setTextAppearances(@StyleRes int textAppearanceResId, @StyleRes int subTextAppearanceResId) {
        setTextAppearancesInternal(textAppearanceResId, subTextAppearanceResId);
    }

    public void setSubTextAppearance(@StyleRes int styleResId) {
        mSubTextAppearanceResId = styleResId;
        setTextAppearances(mTextAppearanceResId, styleResId);
    }

    public void setForceTextGravity(@TextGravity int gravity) {
        mTextGravity = gravity;

        if (mTxtView != null) {
            mTxtView.setGravity(resolveTextGravity(gravity));
        }
    }

    public void setGroup(@NonNull CompoundButtonGroup radioGroupPro) {
        mParent = radioGroupPro;
    }

    public void setOnCheckedChangedListener(OnCompoundCheckChangedListener listener) {
        mCheckChangeListener = listener;
    }

    public void setOnBeforeCheckChangedListener(BeforeCompoundCheckChangeListener listener) {
        mBeforeCheckListener = listener;
    }

    @Override
    public boolean isChecked() {
        CompoundButton btn = getCompoundButton();
        if (btn == null) {
            return false;
        }
        return btn.isChecked();
    }

    @Override
    public void setChecked(boolean checked) {
        CompoundButton btn = getCompoundButton();
        if (btn == null) {
            return;
        }

        if (mBeforeCheckListener == null || mBeforeCheckListener.beforeCheck(this, checked)) {
            btn.setChecked(checked);
        }
    }

    @Override
    public void toggle() {
        CompoundButton btn = getCompoundButton();
        if (btn == null) {
            return;
        }
        if (mBeforeCheckListener == null || mBeforeCheckListener.beforeCheck(this, !isChecked())) {
            btn.toggle();
        }
    }

    @CallSuper
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Drawable background = getBackground();
        if (background != null) {
            int[] state = {isChecked ? android.R.attr.state_checked : -android.R.attr.state_checked};
            background.setState(state);
        }

        if (mCheckChangeListener != null) {
            mCheckChangeListener.onCheckChanged(this, isChecked);
        }
    }

    @Override
    public void setOrientation(int orientation) {
        // overridden
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return true;
    }

    @IntDef({COMPOUND_TEXT_LEFT, COMPOUND_TEXT_TOP, COMPOUND_TEXT_RIGHT, COMPOUND_TEXT_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CompoundDirection {}

    @IntDef({COMPOUND_TEXT_GRAVITY_START, COMPOUND_TEXT_GRAVITY_END, COMPOUND_TEXT_GRAVITY_CENTER, COMPOUND_TEXT_GRAVITY_LEFT, COMPOUND_TEXT_GRAVITY_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextGravity {}

    public interface OnCompoundCheckChangedListener {
        void onCheckChanged(PeaceCompoundButton button, boolean isChecked);
    }

    public interface BeforeCompoundCheckChangeListener {
        boolean beforeCheck(PeaceCompoundButton button, boolean newState);
    }
}
