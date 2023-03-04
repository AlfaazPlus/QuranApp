package com.quranapp.android.adapters.extended;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.list.base.BaseListAdapter;
import com.peacedesign.android.widget.list.base.BaseListItem;
import com.peacedesign.android.widget.list.base.BaseListItemView;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

public class PeaceBottomSheetMenuAdapter extends BaseListAdapter<BaseListItem> {
    private final int mMessageColor;

    public PeaceBottomSheetMenuAdapter(@NonNull Context context) {
        super(context);
        mMessageColor = ContextCompat.getColor(context, R.color.colorText2);
    }

    @Override
    protected View onCreateItemView(@NonNull BaseListItem item, int position) {
        BaseListItemView view = (BaseListItemView) super.onCreateItemView(item, position);
        if (TextUtils.isEmpty(item.getMessage())) {
            ViewPaddingKt.updatePaddingVertical(view.mContainerView, Dimen.dp2px(getContext(), 3));
        } else {
            view.mMessageView.setTextColor(mMessageColor);
        }
        return view;
    }
}
