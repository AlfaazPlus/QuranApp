package com.quranapp.android.views.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Overridden
 * {@link #isPaddingOffsetRequired},
 * {@link #getTopPaddingOffset} and
 * {@link #getBottomPaddingOffset}
 * to improve fading edge effect.
 */
public class RecyclerView2 extends RecyclerView {
    private RecyclerView.OnScrollListener mScrollListener;
    private InputMethodManager mImm;

    public RecyclerView2(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        enableDismissKeyboardOnScroll();
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return true;
    }

    @Override
    protected int getTopPaddingOffset() {
        return -getPaddingTop();
    }

    @Override
    protected int getBottomPaddingOffset() {
        return getPaddingBottom();
    }

    private void enableDismissKeyboardOnScroll() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            boolean isKeyboardDismissedByScroll;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int state) {
                switch (state) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        if (!isKeyboardDismissedByScroll) {
                            hideKeyboard();
                            isKeyboardDismissedByScroll = !isKeyboardDismissedByScroll;
                        }
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        isKeyboardDismissedByScroll = false;
                        break;
                }
            }
        };

        addOnScrollListener(mScrollListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mScrollListener != null) {
            addOnScrollListener(mScrollListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mScrollListener != null) {
            removeOnScrollListener(mScrollListener);
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null && imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
            clearFocus();
        }
    }

    public InputMethodManager getInputMethodManager() {
        if (mImm == null) {
            mImm = ContextCompat.getSystemService(getContext(), InputMethodManager.class);
        }

        return mImm;
    }
}
