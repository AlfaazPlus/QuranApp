/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 20/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.reader.swipe;

import android.content.Context;
import android.widget.TextView;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;

import me.dkzwm.widget.srl.extra.IRefreshView;

public class ViewSwipeToPrevious extends BaseViewSwipeTo {
    public ViewSwipeToPrevious(Context context) {
        super(context);
    }

    @Override
    protected TextView createTitleView(Context ctx) {
        TextView titleView = super.createTitleView(ctx);
        LayoutParams p = (LayoutParams) titleView.getLayoutParams();
        p.bottomMargin = Dimen.dp2px(ctx, 5);
        titleView.setLayoutParams(p);
        return titleView;
    }

    @Override
    protected String getTitle(int readType) {
        switch (readType) {
            case READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelPreviousChapter);
            case READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelPreviousJuz);
            default:
                return null;
        }
    }

    @Override
    protected String getNoFurtherTitle(int readType) {
        switch (readType) {
            case READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelNoPreviousChapter);
            case READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelNoPreviousJuz);
            default:
                return null;
        }
    }

    @Override
    protected int getArrowResource() {
        return R.drawable.dr_icon_chevron_left;
    }

    @Override
    protected int getTitleIndex() {
        return 0;
    }

    @Override
    protected int getArrowIndex() {
        return 1;
    }

    @Override
    protected float getRotationBeforeReach() {
        return -90;
    }

    @Override
    public int getType() {
        return IRefreshView.TYPE_HEADER;
    }
}
