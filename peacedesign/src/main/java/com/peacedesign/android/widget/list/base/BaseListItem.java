package com.peacedesign.android.widget.list.base;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.ComponentBase;

public class BaseListItem extends ComponentBase {
    private int mIconRes;
    private CharSequence mLabel;
    private CharSequence mMessage;
    private BaseListAdapter<? extends BaseListItem> mAdapter;
    private View mItemView;

    public BaseListItem() {
        this(null);
    }

    public BaseListItem(@Nullable CharSequence label) {
        this(0, label);
    }

    public BaseListItem(@DrawableRes int icon) {
        this(icon, null);
    }

    public BaseListItem(@DrawableRes int icon, @Nullable CharSequence label) {
        this(icon, label, null);
    }

    public BaseListItem(@DrawableRes int icon, @Nullable CharSequence label, @Nullable CharSequence message) {
        mIconRes = icon;
        mLabel = label;
        mMessage = message;
    }

    @DrawableRes
    public int getIcon() {
        return mIconRes;
    }

    public void setIcon(@DrawableRes int icon) {
        mIconRes = icon;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public void setLabel(@NonNull CharSequence label) {
        mLabel = label;
    }

    @Nullable
    public CharSequence getMessage() {
        return mMessage;
    }

    public void setMessage(@NonNull CharSequence message) {
        mMessage = message;
    }

    @Nullable
    public BaseListAdapter<? extends BaseListItem> getAdapter() {
        return mAdapter;
    }

    public void setAdapter(@NonNull BaseListAdapter<? extends BaseListItem> mMenuAdapter) {
        mAdapter = mMenuAdapter;
    }

    @Nullable
    public View getItemView() {
        return mItemView;
    }

    public void setItemView(@NonNull View view) {
        mItemView = view;
    }
}
