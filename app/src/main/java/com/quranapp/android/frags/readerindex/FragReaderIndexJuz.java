package com.quranapp.android.frags.readerindex;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.quranapp.android.adapters.quranIndex.ADPJuzList;
import com.quranapp.android.views.helper.RecyclerView2;

public class FragReaderIndexJuz extends BaseFragReaderIndex {
    public FragReaderIndexJuz() {
    }

    public static FragReaderIndexJuz newInstance() {
        return new FragReaderIndexJuz();
    }

    @Override
    protected void initList(RecyclerView2 list, Context ctx) {
        super.initList(list, ctx);

        mHandler.post(() -> {
            LinearLayoutManager layoutManager = new LinearLayoutManager(ctx);
            list.setLayoutManager(layoutManager);
        });

        resetAdapter(list, ctx, false);
    }

    @Override
    protected void resetAdapter(RecyclerView2 list, Context ctx, boolean reverse) {
        super.resetAdapter(list, ctx, reverse);
        ADPJuzList adapter = new ADPJuzList(this, ctx, reverse);
        mHandler.post(() -> list.setAdapter(adapter));
    }
}