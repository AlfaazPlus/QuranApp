package com.quranapp.android.views.reader;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.quranapp.android.R;
import com.quranapp.android.databinding.LytPageNoBinding;

public class PageNoView extends FrameLayout {
    private static final int SIZE_SMALL = 0;
    private static final int SIZE_MEDIUM = 1;
    private final LytPageNoBinding mBinding;
    private int mSize;
    private int mPageNo;

    public PageNoView(Context context) {
        this(context, null);
    }

    public PageNoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageNoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageNoView, defStyleAttr, 0);
        mPageNo = a.getInt(R.styleable.PageNoView_pageNumber, 1);
        a.recycle();

        mBinding = LytPageNoBinding.inflate(LayoutInflater.from(context));
        addView(mBinding.getRoot());

        init();
    }

    private void init() {
        setPageNumber(mPageNo);
    }

    public void setPageNumber(int pageNumber) {
        mPageNo = pageNumber;
        final String pageNo = "Page: " + pageNumber;
        mBinding.textView.setText(pageNo);
    }
}
