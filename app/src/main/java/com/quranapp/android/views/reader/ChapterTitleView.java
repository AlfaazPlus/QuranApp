package com.quranapp.android.views.reader;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytChapterTitleBinding;
import com.quranapp.android.utils.extensions.LayoutParamsKt;

public class ChapterTitleView extends FrameLayout {
    private static final int SIZE_SMALL = 0;
    private static final int SIZE_MEDIUM = 1;
    private final LytChapterTitleBinding mBinding;
    private final int mSize;
    private int mChapterNo;

    public ChapterTitleView(Context context) {
        this(context, null);
    }

    public ChapterTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChapterTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChapterTitleView, defStyleAttr, 0);
        mChapterNo = a.getInt(R.styleable.ChapterTitleView_chapterNumber, 1);
        mSize = a.getInt(R.styleable.ChapterTitleView_chapterTitleSize, SIZE_MEDIUM);
        a.recycle();

        mBinding = LytChapterTitleBinding.inflate(LayoutInflater.from(context));
        addView(mBinding.getRoot());

        init();
    }

    private void init() {
        setChapterNumber(mChapterNo);
    }

    public void setChapterNumber(int chapterNo) {
        mChapterNo = chapterNo;
        mBinding.chapterIcon.setChapterNumber(chapterNo);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof MarginLayoutParams) {
            //            params.width = WRAP_CONTENT;
            LayoutParamsKt.updateMarginVertical((MarginLayoutParams) params, Dimen.dp2px(getContext(), 20));
        }
        super.setLayoutParams(params);
    }
}
