/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 13/3/2022.
 * All rights reserved.
 */

package com.peacedesign.android.widget.sheet;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.widget.list.base.BaseListAdapter;
import com.peacedesign.android.widget.list.base.BaseListItem;
import com.peacedesign.android.widget.list.base.BaseListView;
import com.peacedesign.android.widget.list.simple.SimpleListView;
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListAdapter;
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListView;

public class PeaceBottomSheetMenu extends PeaceBottomSheet {
    private OnItemClickListener mItemClickListener;
    private BaseListAdapter<? extends BaseListItem> mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If savedInstanceState != null, it means onCreate was called after a onSavedInstanceState.
        // So we will dismiss the menu because we are currently not supporting state saving.
        if (savedInstanceState != null) {
            try {
                dismiss();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void setupContentView(@NonNull LinearLayout dialogLayout, PeaceBottomSheetParams params) {
        if (mAdapter == null) {
            return;
        }

        View adapterView = createAdapterView(mAdapter);
        dialogLayout.addView(adapterView);
    }

    @NonNull
    protected View createAdapterView(@NonNull BaseListAdapter<? extends BaseListItem> listAdapter) {
        final BaseListView listView;
        if (listAdapter instanceof SingleChoiceListAdapter) {
            listView = new SingleChoiceListView(getContext());
        } else {
            listView = new SimpleListView(getContext());
        }
        listView.setOnItemClickListener(item -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(this, item);
            }
        });

        listView.post(() -> listView.setAdapter(mAdapter));
        return listView;
    }

    public void setAdapter(@NonNull BaseListAdapter<? extends BaseListItem> listAdapter) {
        mAdapter = listAdapter;
    }

    public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(@NonNull PeaceBottomSheetMenu dialog, @NonNull BaseListItem item);
    }
}
