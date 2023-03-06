package com.quranapp.android.adapters.utility;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

import java.util.List;

public class TopicFilterSpinnerAdapter extends SpinnerAdapter2<SpinnerItem> {
    public TopicFilterSpinnerAdapter(@NonNull Context context, int itemLytRes, int textViewId, @NonNull List<SpinnerItem> objects) {
        super(context, itemLytRes, textViewId, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        ViewPaddingKt.updatePaddingVertical(view, Dimen.dp2px(parent.getContext(), 10));
        ViewPaddingKt.updatePaddingHorizontal(view, Dimen.dp2px(parent.getContext(), 15));
        return view;
    }
}
