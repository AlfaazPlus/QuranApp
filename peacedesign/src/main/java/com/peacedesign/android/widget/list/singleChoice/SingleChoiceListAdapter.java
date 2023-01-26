package com.peacedesign.android.widget.list.singleChoice;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.peacedesign.android.widget.list.base.BaseListAdapter;
import com.peacedesign.android.widget.list.base.BaseListItem;
import com.peacedesign.android.widget.radio.PeaceRadioButton;
import com.peacedesign.android.widget.radio.PeaceRadioGroup;

public class SingleChoiceListAdapter extends BaseListAdapter<BaseListItem> {
    public SingleChoiceListAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected View onCreateItemView(@NonNull BaseListItem item, int position) {
        PeaceRadioButton radio = new PeaceRadioButton(getContext());
        radio.setTag(item);
        radio.setTexts(item.getLabel(), item.getMessage());

        if (item.getId() != View.NO_ID) {
            radio.setId(item.getId());
        }

        return radio;
    }

    @Override
    protected void onAppendItemView(@NonNull ViewGroup container, @NonNull View itemView, int position) {
        super.onAppendItemView(container, itemView, position);
        if (getItem(position).isSelected() && container instanceof PeaceRadioGroup) {
            ((PeaceRadioGroup) container).check(itemView.getId());
        }
    }
}
