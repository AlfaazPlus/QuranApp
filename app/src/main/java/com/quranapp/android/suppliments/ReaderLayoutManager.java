package com.quranapp.android.suppliments;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.views.reader.VerseView;

public class ReaderLayoutManager extends LinearLayoutManager {
    private final Context mContext;

    public ReaderLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        mContext = context;
    }

    public void smoothScrollToPosition(int position, int verseNumber, boolean highlight) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(mContext) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            @Override
            protected void onTargetFound(View view, RecyclerView.State state, Action action) {
                super.onTargetFound(view, state, action);
                if (!highlight) {
                    return;
                }

                if (view instanceof VerseView) {
                    final VerseView verseView = (VerseView) view;
                    if (verseView.getVerse().verseNo == verseNumber) {
                        verseView.highlightOnScroll();
                    }
                }
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 1f / displayMetrics.densityDpi;
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}
