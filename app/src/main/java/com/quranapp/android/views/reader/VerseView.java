package com.quranapp.android.views.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.components.reader.ChapterVersePair;
import com.quranapp.android.databinding.LytReaderVerseBinding;
import com.quranapp.android.databinding.LytReaderVerseQuickActionsBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.univ.SelectableLinkMovementMethod;

@SuppressLint("ViewConstructor")
public class VerseView extends FrameLayout implements BookmarkCallbacks {
    public final ReaderVerseDecorator mVerseDecorator;
    public final ReaderPossessingActivity mActivity;
    private final LytReaderVerseBinding mBinding;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final boolean mIsShowingAsReference;
    private Verse mVerse;
    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            if (!mIsShowingAsReference && mActivity != null && mVerse != null) {
                mActivity.openVerseOptionDialog(mVerse, VerseView.this);
            }
        }
    };
    private final int mHighlightedBGColor;
    private final int mUnhighlightedBGColor;
    private ValueAnimator mBGAnimator;
    private final Runnable mBGAnimationRunnable = this::animateBG;
    private int mCurrentlyHighlightingVerseNo = -1;
    private boolean mScrollHighlightAnimationStarted;
    private boolean mScrollHighlightInProgress;
    private boolean mIsReciting;
    private boolean mRequireBottomBorder = true;


    public VerseView(ReaderPossessingActivity activity, ViewGroup parent, Verse verse, boolean showAsReference) {
        super(activity);
        mActivity = activity;
        mVerseDecorator = activity.mVerseDecorator;
        mHighlightedBGColor = mActivity.mVerseHighlightedBGColor;
        mUnhighlightedBGColor = showAsReference ? Color.TRANSPARENT : mActivity.mVerseUnhighlightedBGColor;

        mIsShowingAsReference = showAsReference;

        mVerse = verse;

        mBinding = LytReaderVerseBinding.inflate(LayoutInflater.from(getContext()), parent, false);
        addView(mBinding.getRoot());

        init();

        if (verse != null) {
            initWithVerse(verse);
        }
    }

    private void init() {
        initThis();
        initAnimator();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initThis() {
        // for savedInstance
        setId(View.generateViewId());

        GestureDetector detector = new GestureDetector(getContext(), gestureListener, mHandler);
        mBinding.getRoot().setOnTouchListener((v, event) -> detector.onTouchEvent(event));

        mBinding.bottomBorder.setVisibility(mRequireBottomBorder ? VISIBLE : GONE);
    }

    private void initAnimator() {
        mBGAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), mHighlightedBGColor, mUnhighlightedBGColor);
        mBGAnimator.setDuration(1000);
        mBGAnimator.addUpdateListener(animation -> {
            if (mActivity.isDestroyed2()) {
                mBGAnimator.cancel();
                return;
            }

            // Necessary to prevent the problem in recyclerView when fast scrolled
            if (mVerse != null && mVerse.verseNo == mCurrentlyHighlightingVerseNo) {
                setBackgroundColor((int) animation.getAnimatedValue());
            }
        });
        mBGAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mScrollHighlightAnimationStarted = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollHighlightInProgress = false;
                mScrollHighlightAnimationStarted = false;
                VerseView.super.setBackgroundColor(mIsReciting ? mHighlightedBGColor : mUnhighlightedBGColor);
                mCurrentlyHighlightingVerseNo = -1;
            }
        });
    }

    private void initActionsButtons() {
        LytReaderVerseQuickActionsBinding verseHeader = mBinding.verseHeader;

        if (mIsShowingAsReference) {
            verseHeader.verseActionContainer.setVisibility(GONE);
            return;
        }

        Context ctx = getContext();

        verseHeader.verseActionContainer.setVisibility(VISIBLE);

        boolean recitationSupported = RecitationUtils.isRecitationSupported() && mActivity instanceof ActivityReader;
        verseHeader.btnVerseRecitation.setVisibility(!recitationSupported ? GONE : VISIBLE);

        verseHeader.btnVerseOptions.setOnClickListener(v -> {
            if (mActivity != null) {
                mActivity.openVerseOptionDialog(mVerse, this);
            }
        });

        int chapterNo = mVerse.chapterNo;
        int verseNo = mVerse.verseNo;

        verseHeader.btnVerseRecitation.setOnClickListener(v -> {
            if (mActivity instanceof ActivityReader) {
                ActivityReader reader = (ActivityReader) mActivity;
                if (reader.mPlayer != null) {
                    reader.mPlayer.reciteControl(new ChapterVersePair(chapterNo, verseNo));
                }
            }
        });

        verseHeader.btnTafsir.setOnClickListener(v -> ReaderFactory.startTafsir(ctx, chapterNo, verseNo));
        ViewCompat.setTooltipText(verseHeader.btnTafsir, ctx.getString(R.string.strTitleTafsir));

        onBookmarkChanged(mActivity.isBookmarked(chapterNo, verseNo, verseNo));
        verseHeader.btnBookmark.setOnClickListener(v -> {
            if (mActivity.isBookmarked(chapterNo, verseNo, verseNo)) {
                mActivity.onBookmarkView(chapterNo, verseNo, verseNo, this);
            } else {
                mActivity.addVerseToBookmark(chapterNo, verseNo, verseNo, this);
            }
        });
        ViewCompat.setTooltipText(verseHeader.btnBookmark, ctx.getString(R.string.strLabelBookmark));
    }

    private void onBookmarkChanged(boolean isBookmarked) {
        final int filter = ContextCompat.getColor(getContext(),
            isBookmarked ? R.color.colorPrimary : R.color.colorIcon2);
        mBinding.verseHeader.btnBookmark.setColorFilter(filter);

        final int res = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        mBinding.verseHeader.btnBookmark.setImageResource(res);
    }

    public Verse getVerse() {
        return mVerse;
    }

    public void setVerse(Verse verse) {
        mVerse = verse;

        if (mScrollHighlightInProgress && !mScrollHighlightAnimationStarted
            && mCurrentlyHighlightingVerseNo == verse.verseNo) {
            setBackgroundColor(mHighlightedBGColor);
        } else {
            setBackgroundColor(mUnhighlightedBGColor);
        }

        initWithVerse(verse);
    }

    private void initWithVerse(Verse verse) {
        initActionsButtons();
        mapAyahContents(verse);
    }

    private void mapAyahContents(Verse verse) {
        setupWithDecorator();

        int chapterNo = verse.chapterNo;
        int verseNo = verse.verseNo;

        final String verseSerial;
        final String verseSerialDesc;
        if (verse.getIncludeChapterNameInSerial()) {
            String name = mActivity.mQuranMetaRef.get().getChapterName(getContext(), chapterNo);
            verseSerial = getContext().getString(R.string.strLabelVerseSerialWithChapter, name, chapterNo, verseNo);
            verseSerialDesc = getContext().getString(R.string.strDescVerseNoWithChapter, name, verseNo);
        } else {
            verseSerial = getContext().getString(R.string.strLabelVerseSerial, chapterNo, verseNo);
            verseSerialDesc = getContext().getString(R.string.strDescVerseNo, verseNo);
        }

        mBinding.verseHeader.verseSerial.setContentDescription(verseSerialDesc);
        mBinding.verseHeader.verseSerial.setText(verseSerial);

        mBinding.textArabic.setVisibility(verse.arabicTextSpannable != null ? VISIBLE : GONE);
        if (verse.arabicTextSpannable != null) {
            mBinding.textArabic.setText(verse.arabicTextSpannable);
        }
        mBinding.translTextSpannable.setText(verse.translTextSpannable);
        mBinding.translTextSpannable.setMovementMethod(SelectableLinkMovementMethod.getInstance());
    }

    public void setupWithDecorator() {
        mVerseDecorator.setTextColorArabic(mBinding.textArabic);
        mVerseDecorator.setTextSizeArabic(mBinding.textArabic);
        mVerseDecorator.setTextSizeTransl(mBinding.translTextSpannable);

        mVerseDecorator.setTextColorNonArabic(mBinding.translTextSpannable);
    }

    public void highlightOnScroll() {
        if (mIsReciting) {
            return;
        }

        setBackgroundColor(mActivity.mVerseHighlightedBGColor);

        if (mVerse != null) {
            mCurrentlyHighlightingVerseNo = mVerse.verseNo;
        }

        mScrollHighlightInProgress = true;

        mHandler.removeCallbacks(mBGAnimationRunnable);
        mHandler.postDelayed(mBGAnimationRunnable, 1000);
    }

    private void animateBG() {
        if (mBGAnimator.isRunning()) {
            mBGAnimator.cancel();
        }
        mBGAnimator.start();
    }

    public void onRecite(boolean isReciting) {
        mIsReciting = isReciting;

        int resId = isReciting ? R.drawable.dr_icon_pause2 : R.drawable.dr_icon_play2;
        mBinding.verseHeader.btnVerseRecitation.setImageResource(resId);

        if (!mScrollHighlightInProgress) {
            int bgColor = isReciting ? mActivity.mVerseHighlightedBGColor : Color.TRANSPARENT;
            setBackgroundColor(bgColor);
        }
    }

    public void setRequireBottomBorder(boolean requireBorder) {
        mRequireBottomBorder = requireBorder;
        mBinding.bottomBorder.setVisibility(requireBorder ? VISIBLE : GONE);
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        onBookmarkChanged(false);
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        onBookmarkChanged(true);
    }
}
