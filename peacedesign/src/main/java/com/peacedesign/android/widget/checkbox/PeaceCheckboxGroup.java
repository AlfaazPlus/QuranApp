package com.peacedesign.android.widget.checkbox;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PeaceCheckboxGroup extends LinearLayout {
    private boolean mProtectFromInvocation = false;
    private PeaceCheckboxGroup.PassThroughHierarchyChangeListener mPassThroughListener;
    private OnItemCheckedChangeListener mOnItemCheckedChangeListener;
    private final List<PeaceCheckBox> mCheckedBoxes = new ArrayList<>();

    public PeaceCheckboxGroup(Context context) {
        this(context, null);
    }

    public PeaceCheckboxGroup(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceCheckboxGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceCheckboxGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        mPassThroughListener = new PeaceCheckboxGroup.PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(mPassThroughListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnHierarchyChangeListener(@NonNull OnHierarchyChangeListener listener) {
        mPassThroughListener.mOnHierarchyChangeListener = listener;
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (child instanceof PeaceCheckBox) {
            final PeaceCheckBox checkBox = (PeaceCheckBox) child;

            if (checkBox.isChecked()) {
                checkedChangedInternal(checkBox, true);
            }

            checkBox.setOnCheckedChangedListener((buttonView, isChecked) -> {
                checkedChangedInternal(checkBox, isChecked);

                if (!mProtectFromInvocation && mOnItemCheckedChangeListener != null) {
                    mOnItemCheckedChangeListener.onCheckedChanged(checkBox, isChecked);
                }
            });
        }
        super.addView(child, index, params);
    }

    private void checkedChangedInternal(PeaceCheckBox checkBox, boolean isChecked) {
        if (isChecked) {
            mCheckedBoxes.add(checkBox);
        } else {
            mCheckedBoxes.remove(checkBox);
        }
    }

    public void clearCheck(boolean invokeListener) {
        mProtectFromInvocation = !invokeListener;
        for (int i = 0, l = getChildCount(); i < l; i++) {
            View child = getChildAt(i);
            if (child instanceof PeaceCheckBox) {
                ((PeaceCheckBox) child).setChecked(false);
            }
        }
        mProtectFromInvocation = false;
    }

    public void checkSansInvocation(@IdRes int id, boolean checked) {
        mProtectFromInvocation = true;
        checkById(id, checked);
        mProtectFromInvocation = false;
    }

    public void checkById(@IdRes int id, boolean checked) {
        setCheckedForView(id, checked);
    }

    public void checkByIndex(int index, boolean checked) {
        View checkBox = getChildAt(index);
        if (checkBox instanceof PeaceCheckBox) {
            ((PeaceCheckBox) checkBox).setChecked(checked);
        }
    }

    private void setCheckedForView(@IdRes int id, boolean checked) {
        if (id == View.NO_ID) return;

        View checkBox = findViewById(id);
        if (checkBox instanceof PeaceCheckBox) {
            ((PeaceCheckBox) checkBox).setChecked(checked);
        }
    }

    public void setOnItemCheckedChangeListener(@Nullable OnItemCheckedChangeListener listener) {
        mOnItemCheckedChangeListener = listener;
    }

    public List<PeaceCheckBox> getCheckedBoxes() {
        return mCheckedBoxes;
    }

    public interface OnItemCheckedChangeListener {
        void onCheckedChanged(PeaceCheckBox checkBox, boolean isChecked);
    }

    /**
     * <p>A pass-through listener acts upon the events and dispatches them
     * to another listener. This allows the table layout to set its own internal
     * hierarchy change listener without preventing the user to setup his.</p>
     */
    class PassThroughHierarchyChangeListener implements OnHierarchyChangeListener {
        OnHierarchyChangeListener mOnHierarchyChangeListener;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onChildViewAdded(View parent, View child) {
            if (parent == PeaceCheckboxGroup.this && child instanceof PeaceCheckBox) {
                if (mOnHierarchyChangeListener != null) {
                    mOnHierarchyChangeListener.onChildViewAdded(parent, child);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onChildViewRemoved(View parent, View child) {
            if (parent == PeaceCheckboxGroup.this && child instanceof PeaceCheckBox) {
                checkedChangedInternal((PeaceCheckBox) child, false);
                if (mOnHierarchyChangeListener != null) {
                    mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
                }
            }
        }
    }
}
