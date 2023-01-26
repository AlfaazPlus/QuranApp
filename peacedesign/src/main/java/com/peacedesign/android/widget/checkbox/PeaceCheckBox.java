package com.peacedesign.android.widget.checkbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.CompoundButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.peacedesign.R;
import com.peacedesign.android.widget.compound.PeaceCompoundButton;

public class PeaceCheckBox extends PeaceCompoundButton {
    private boolean mHasInitialButtonDrawable = false;
    private Drawable mCheckBoxButtonCompat;
    private CheckBoxHelper mCheckBox;

    public PeaceCheckBox(Context context) {
        this(context, null);
    }

    public PeaceCheckBox(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceCheckBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PeaceCompoundButton, defStyleAttr, defStyleRes);

        if (a.hasValue(R.styleable.PeaceCompoundButton_peaceComp_buttonCompat)) {
            mHasInitialButtonDrawable = true;
            mCheckBoxButtonCompat = a.getDrawable(R.styleable.PeaceCompoundButton_peaceComp_buttonCompat);
        }

        a.recycle();
    }

    @Override
    protected void initThis() {
        super.initThis();
        super.setOnClickListener(v -> toggle());
    }

    @Override
    protected void makeComponents() {
        makeCheckBox();
        super.makeComponents();
    }

    private void makeCheckBox() {
        mCheckBox = new CheckBoxHelper(getContext());
        mCheckBox.setOnCheckedChangeListener(this);

        mCheckBox.setChecked(mInitialChecked);
        if (mHasInitialButtonDrawable) {
            setButtonDrawable(mCheckBoxButtonCompat);
        }
    }

    @Override
    protected CompoundButton getCompoundButton() {
        return mCheckBox;
    }

    public void setButtonDrawable(Drawable buttonDrawable) {
        mCheckBox.setButtonDrawable(buttonDrawable);
    }

    public void setButtonDrawable(@DrawableRes int resId) {
        setButtonDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        throw new IllegalStateException("Use setOnCheckedChangedListener instead.");
    }

    class CheckBoxHelper extends AppCompatCheckBox {
        public CheckBoxHelper(@NonNull Context context) {
            super(new ContextThemeWrapper(context, R.style.PeaceCheckBox), null, 0);
            setPaddingRelative(0, 0, 0, 0);
            setChecked(mInitialChecked);
        }
    }
}
