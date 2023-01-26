package com.peacedesign.android.widget.list.singleChoice;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.widget.list.base.BaseListItem;
import com.peacedesign.android.widget.list.base.BaseListView;
import com.peacedesign.android.widget.radio.PeaceRadioGroup;

public class SingleChoiceListView extends BaseListView {
    @Nullable
    private OnItemClickListener mSelectionChangeListener;

    public SingleChoiceListView(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected ViewGroup createItemsContainer(@NonNull Context context) {
        PeaceRadioGroup radioGroup = new PeaceRadioGroup(context);
        radioGroup.setOnCheckedChangedListener((button, checkedId) -> {
            if (mSelectionChangeListener != null) {
                BaseListItem item = (BaseListItem) button.getTag();
                if (item != null) {
                    mSelectionChangeListener.onItemClick(item);
                }
            }
        });
        return radioGroup;
    }

    @Override
    public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
        mSelectionChangeListener = listener;
    }
}
