package com.quranapp.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;

public class IconedTextView extends AppCompatTextView {
    private float mDrawableStartDimen;
    private float mDrawableTopDimen;
    private float mDrawableEndDimen;
    private float mDrawableBottomDimen;

    public IconedTextView(@NonNull Context context) {
        this(context, null);
    }

    public IconedTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconedTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.IconedTextView, defStyleAttr, 0);
        mDrawableStartDimen = a.getDimensionPixelOffset(R.styleable.IconedTextView_drawableStartDimen, -1);
        mDrawableTopDimen = a.getDimensionPixelOffset(R.styleable.IconedTextView_drawableTopDimen, -1);
        mDrawableEndDimen = a.getDimensionPixelOffset(R.styleable.IconedTextView_drawableEndDimen, -1);
        mDrawableBottomDimen = a.getDimensionPixelOffset(R.styleable.IconedTextView_drawableBottomDimen, -1);
        int mDrawablePadding = a.getDimensionPixelOffset(R.styleable.IconedTextView_android_drawablePadding,
            Dimen.dp2px(context, 10));
        int mGravity = a.getInt(R.styleable.IconedTextView_android_gravity, Gravity.CENTER_VERTICAL);
        a.recycle();

        setGravity(mGravity);
        setCompoundDrawablePadding(mDrawablePadding);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Drawable[] relDrawables = adjust(getCompoundDrawablesRelative());
        setCompoundDrawablesRelative(relDrawables[0], relDrawables[1], relDrawables[2], relDrawables[3]);
    }

    private boolean hasStartDrawable() {
        return getStartDrawable() != null;
    }

    private boolean hasTopDrawable() {
        return getTopDrawable() != null;
    }

    private boolean hasEndDrawable() {
        return getEndDrawable() != null;
    }

    private boolean hasBottomDrawable() {
        return getBottomDrawable() != null;
    }

    private Drawable getStartDrawable() {
        return getCompoundDrawables()[0];
    }

    private Drawable getTopDrawable() {
        return getCompoundDrawables()[1];
    }

    private Drawable getEndDrawable() {
        return getCompoundDrawables()[2];
    }

    private Drawable getBottomDrawable() {
        return getCompoundDrawables()[3];
    }

    private Drawable[] adjust(Drawable[] drawables) {
        int startDimen = (int) mDrawableStartDimen;
        Drawable start = drawables[0];
        if (startDimen > 0 && start != null) {
            start.setBounds(0, 0, startDimen, startDimen);
            drawables[0] = start;
        }

        int tDimen = (int) mDrawableTopDimen;
        Drawable top = drawables[1];
        if (tDimen > 0 && top != null) {
            top.setBounds(0, 0, tDimen, tDimen);
            drawables[1] = top;
        }

        int endDimen = (int) mDrawableEndDimen;
        Drawable end = drawables[2];
        if (endDimen > 0 && end != null) {
            end.setBounds(0, 0, endDimen, endDimen);
            drawables[2] = end;
        }

        int bDimen = (int) mDrawableBottomDimen;
        Drawable bottom = drawables[3];
        if (bDimen > 0 && bottom != null) {
            bottom.setBounds(0, 0, bDimen, bDimen);
            drawables[3] = bottom;
        }

        return drawables;
    }

    public void setDrawables(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this, start, top, end, bottom);
    }

    public void setDrawableStartDimen(int startDimen) {
        mDrawableStartDimen = startDimen;
    }

    public void setDrawableTopDimen(int topDimen) {
        mDrawableTopDimen = topDimen;
    }

    public void setDrawableEndDimen(int endDimen) {
        mDrawableEndDimen = endDimen;
    }

    public void setDrawableBottomDimen(int bottomDimen) {
        mDrawableBottomDimen = bottomDimen;
    }
}
