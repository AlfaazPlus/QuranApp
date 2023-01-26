package com.peacedesign.android.widget.radio;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.R;
import com.peacedesign.android.utils.interfaceUtils.CompoundButtonGroup;


/**
 * This layout can only contain children of instance {@link PeaceRadioGroup}.
 * Any other child is ignored.
 */
public class PeaceRadioGroup extends LinearLayout implements CompoundButtonGroup {
    private final int mInitialOrientation;
    private int mCheckedId;
    private boolean mProtectFromCheckedChange = false;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private BeforeCheckedChangeListener mBeforeCheckedChangeListener;
    private PassThroughHierarchyChangeListener mPassThroughListener;

    public PeaceRadioGroup(@NonNull Context context) {
        this(context, null);
    }

    public PeaceRadioGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceRadioGroup(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PeaceRadioGroup, defStyleAttr, 0);

        mInitialOrientation = a.getInt(R.styleable.PeaceRadioGroup_android_orientation, VERTICAL);
        mCheckedId = a.getResourceId(R.styleable.PeaceRadioGroup_android_checkedButton, View.NO_ID);

        a.recycle();

        init();
    }

    private void init() {
        super.setOrientation(mInitialOrientation);
        mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(mPassThroughListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnHierarchyChangeListener(@NonNull OnHierarchyChangeListener listener) {
        mPassThroughListener.mOnHierarchyChangeListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mCheckedId != View.NO_ID) {
            mProtectFromCheckedChange = true;
            setCheckedForView(mCheckedId, true);
            setCheckedId(mCheckedId);
            mProtectFromCheckedChange = false;
        }
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (child instanceof PeaceRadioButton) {
            final PeaceRadioButton button = (PeaceRadioButton) child;
            button.setGroup(this);
            if (button.isChecked()) {
                mProtectFromCheckedChange = true;
                if (mCheckedId != -1) setCheckedForView(mCheckedId, false);
                setCheckedId(button.getId());
                mProtectFromCheckedChange = false;
            }
            super.addView(child, index, params);
        }
    }

    @Override
    public void clearCheck() {
        check(View.NO_ID);
    }

    /**
     * Check a button without invoking the {@link #mOnCheckedChangeListener} or {@link #mBeforeCheckedChangeListener}.
     *
     * @param id Id of the {@link PeaceRadioButton} to be checked.
     */
    @Override
    public void checkSansInvocation(@IdRes int id) {
        mProtectFromCheckedChange = true;
        check(id);
        mProtectFromCheckedChange = false;
    }

    @Override
    public void checkAtPosition(int position) {
        View view = getChildAt(position);
        if (view == null) {
            return;
        }

        check(view.getId());
    }

    @Override
    public void check(@IdRes int id) {
        if (id != View.NO_ID && (id == mCheckedId)) return;

        if (mProtectFromCheckedChange || mBeforeCheckedChangeListener == null) {
            checkInternal(id);
        } else if (mBeforeCheckedChangeListener.beforeCheckedChanged(this, id)) {
            checkInternal(id);
        }
    }

    private void checkInternal(@IdRes int id) {
        setCheckedForView(mCheckedId, false); // uncheck previously checked button
        setCheckedForView(id, true); // check the new button
        setCheckedId(id);
    }

    private void setCheckedForView(@IdRes int id, boolean checked) {
        if (id == View.NO_ID) return;

        View radioButtonPro = findViewById(id);
        if (radioButtonPro instanceof PeaceRadioButton) {
            ((PeaceRadioButton) radioButtonPro).setChecked(checked);
        }
    }

    private void setCheckedId(@IdRes int id) {
        mCheckedId = id;
        if (!mProtectFromCheckedChange && mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(findViewById(mCheckedId), mCheckedId);
        }
    }

    /**
     * <p>Register a callback to be invoked when the checked radio button
     * changes in this group.</p>
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangedListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /**
     * <p>Register a callback to be invoked before the checked radio button
     * changes in this group.</p>
     *
     * @param listener the callback to call before checked state change
     */
    public void setBeforeCheckedChangeListener(@Nullable BeforeCheckedChangeListener listener) {
        mBeforeCheckedChangeListener = listener;
    }

    public int getCheckedRadioId() {
        return mCheckedId;
    }

    public interface OnCheckedChangeListener {
        /**
         * <p>Called when the checked radio button has changed. When the
         * selection is cleared, checkedId is -1.</p>
         *
         * @param button    The newly checked radio button
         * @param checkedId The unique identifier of the newly checked radio button
         */
        void onCheckedChanged(PeaceRadioButton button, int checkedId);
    }

    public interface BeforeCheckedChangeListener {
        /**
         * <p>Called before the checked radio button has changed. When the
         * selection is cleared, checkedId is -1.</p>
         *
         * @param group       the group in which the checked radio button is to be changed
         * @param newButtonId the unique identifier of the newly checked radio button
         * @return Return false to block check change.
         */
        boolean beforeCheckedChanged(@NonNull PeaceRadioGroup group, @IdRes int newButtonId);
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
            if (parent == PeaceRadioGroup.this && child instanceof PeaceRadioButton) {
                int id = child.getId();
                // generates an id if it's missing
                if (id == View.NO_ID) {
                    id = View.generateViewId();
                    child.setId(id);
                }
            }

            if (mOnHierarchyChangeListener != null) {
                mOnHierarchyChangeListener.onChildViewAdded(parent, child);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onChildViewRemoved(View parent, View child) {
            if (parent == PeaceRadioGroup.this && child instanceof PeaceRadioButton) {
                if (mOnHierarchyChangeListener != null) {
                    mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
                }
            }
        }
    }
}
