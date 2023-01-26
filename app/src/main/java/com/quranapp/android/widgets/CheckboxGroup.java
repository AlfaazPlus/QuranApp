package com.quranapp.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CheckboxGroup extends LinearLayout {
    private boolean mProtectFromInvocation = false;
    private OnItemCheckedChangeListener mOnItemCheckedChangeListener;

    public CheckboxGroup(Context context) {
        this(context, null);
    }

    public CheckboxGroup(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckboxGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckboxGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (child instanceof CheckBox) {
            final CheckBox checkBox = (CheckBox) child;

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!mProtectFromInvocation && mOnItemCheckedChangeListener != null) {
                    mOnItemCheckedChangeListener.onCheckedChanged(checkBox, isChecked);
                }
            });
        }
        super.addView(child, index, params);
    }

    public void clearCheck() {
        for (int i = 0, l = getChildCount(); i < l; i++) {
            View child = getChildAt(i);
            if (child instanceof CheckBox) {
                ((CheckBox) child).setChecked(false);
            }
        }
    }

    public void checkSansInvocation(@IdRes int id, boolean checked) {
        mProtectFromInvocation = true;
        check(id, checked);
        mProtectFromInvocation = false;
    }

    public void check(@IdRes int id, boolean checked) {
        setCheckedForView(id, checked);
    }


    private void setCheckedForView(@IdRes int id, boolean checked) {
        if (id == View.NO_ID) return;

        View checkBox = findViewById(id);
        if (checkBox instanceof CheckBox) {
            ((CheckBox) checkBox).setChecked(checked);
        }
    }

    public void setOnItemCheckedChangeListener(@Nullable OnItemCheckedChangeListener listener) {
        mOnItemCheckedChangeListener = listener;
    }


    public interface OnItemCheckedChangeListener {
        void onCheckedChanged(CheckBox checkBox, boolean isChecked);
    }
}
