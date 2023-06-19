package com.quranapp.android.utils.extended;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GapedItemDecoration extends RecyclerView.ItemDecoration {
    private final int gap;

    public GapedItemDecoration(int gap) {
        this.gap = gap;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(gap, gap, gap, gap);
    }
}