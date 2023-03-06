/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 17/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.adapters.quranIndex;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.frags.readerindex.BaseFragReaderIndex;

public abstract class ADPReaderIndexBase<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final BaseFragReaderIndex mFragment;
    private final boolean mReversed;

    protected ADPReaderIndexBase(BaseFragReaderIndex fragment, boolean reverse) {
        mFragment = fragment;
        mReversed = reverse;
    }

    protected void initADP(Context ctx) {
        prepareList(ctx, mReversed);
        setHasStableIds(true);
    }

    protected abstract void prepareList(Context ctx, boolean reverse);

    @Override
    public long getItemId(int position) {
        return position;
    }
}
