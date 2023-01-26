package com.quranapp.android.utils;

import static androidx.recyclerview.widget.RecyclerView.LayoutManager;
import static androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Custom Scroll Listener for recycler view to listen to up and down scroll
 */
@SuppressWarnings("FieldCanBeLocal")
public abstract class RVLazyLoadListener extends OnScrollListener {
    private final int mTHRESHOLD;
    private final boolean isStaggeredGLM;
    private final boolean isNormalGLM;
    private final boolean isLLM;
    private StaggeredGridLayoutManager staggeredGLM;
    private GridLayoutManager GLM;
    private LinearLayoutManager LLM;

    public RVLazyLoadListener(@NonNull LayoutManager layoutManager, int threshold) {
        mTHRESHOLD = threshold;
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            isStaggeredGLM = true;
            isNormalGLM = false;
            isLLM = false;
            staggeredGLM = (StaggeredGridLayoutManager) layoutManager;
        } else if (layoutManager instanceof GridLayoutManager) {
            isStaggeredGLM = false;
            isNormalGLM = true;
            isLLM = false;
            GLM = (GridLayoutManager) layoutManager;
        } else if (layoutManager instanceof LinearLayoutManager) {
            isStaggeredGLM = false;
            isNormalGLM = false;
            isLLM = true;
            LLM = (LinearLayoutManager) layoutManager;
        } else {
            isStaggeredGLM = false;
            isNormalGLM = false;
            isLLM = false;
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy <= 0) return;
        onCheckLazyLoad();
    }

    public void onCheckLazyLoad() {
        if (isStaggeredGLM) {
            onStaggeredGLMScroll();
        } else if (isLLM) {
            onLLMScroll();
        }
    }

    private void onStaggeredGLMScroll() {
        int[] items = new int[staggeredGLM.getSpanCount()];
        staggeredGLM.findFirstVisibleItemPositions(items);
        if (items.length == 0) return;

        int totalItems = staggeredGLM.getItemCount();
        int totalVisibleItems = staggeredGLM.getChildCount();
        int pastVisibleItems = items[0];
        if ((totalVisibleItems + pastVisibleItems) >= totalItems - mTHRESHOLD) onLoadMore();
    }

    private void onLLMScroll() {
        int position = LLM.findFirstVisibleItemPosition();
        int totalItems = LLM.getItemCount();
        int totalVisibleItems = LLM.getChildCount();
        if ((totalVisibleItems + position) >= totalItems - mTHRESHOLD) onLoadMore();
    }

    public abstract void onLoadMore();
}