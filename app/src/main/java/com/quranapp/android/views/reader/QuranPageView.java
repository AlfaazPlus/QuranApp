package com.quranapp.android.views.reader;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.reader.QuranPageModel;
import com.quranapp.android.components.reader.QuranPageSectionModel;
import com.quranapp.android.databinding.LytQuranPageBinding;
import com.quranapp.android.databinding.LytQuranPageSectionBinding;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.span.VerseArabicHighlightSpan;
import com.quranapp.android.utils.univ.SelectableLinkMovementMethod;
import com.quranapp.android.utils.univ.SpannableFactory;

import java.util.concurrent.atomic.AtomicReference;

public class QuranPageView extends FrameLayout {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private LytQuranPageBinding mBinding;

    private int mBGHighlightBGColor;
    private ActivityReader mActivity;

    public QuranPageView(@NonNull ActivityReader activity) {
        this(activity, null);
        mActivity = activity;
    }

    public QuranPageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuranPageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        mBinding = LytQuranPageBinding.inflate(LayoutInflater.from(getContext()));
        addView(mBinding.getRoot());

        setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        mBGHighlightBGColor = ContextCompat.getColor(getContext(), R.color.colorPrimaryAlpha30);
    }

    public void setPageModel(QuranPageModel pageModel) {
        initWithModel(pageModel);
    }

    private void initWithModel(QuranPageModel pageModel) {
        mBinding.header.initWithPageModel(pageModel);

        mBinding.sectionContainer.removeAllViews();

        for (QuranPageSectionModel section : pageModel.getSections()) {
            QuranPageSectionView sectionView = new QuranPageSectionView(getContext());

            if (section.getChapterNo() == pageModel.getScrollHighlightPendingChapterNo()) {
                section.setScrollHighlightPendingVerseNo(pageModel.getScrollHighlightPendingVerseNo());
                pageModel.setScrollHighlightPendingChapterNo(-1);
            }

            section.setSectionView(sectionView);
            sectionView.setSectionModel(section);
            mBinding.sectionContainer.addView(sectionView);
        }
    }

    public class QuranPageSectionView extends FrameLayout {
        private final AtomicReference<VerseArabicHighlightSpan> verseSpanToAnimate = new AtomicReference<>(null);
        public LytQuranPageSectionBinding mBinding;
        private boolean mScrollHighlightInProgress;
        private ValueAnimator mBGAnimator;
        private final Runnable mBGAnimationRunnable = this::animateBG;
        private int recitingVerseNo;

        public QuranPageSectionView(@NonNull Context context) {
            this(context, null);
        }

        public QuranPageSectionView(@NonNull Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public QuranPageSectionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            init();
        }

        private void init() {
            mBinding = LytQuranPageSectionBinding.inflate(LayoutInflater.from(getContext()));
            addView(mBinding.getRoot());
            setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            initBGAnimator();
            initContentView();
        }

        private void initContentView() {
            mBinding.content.setSpannableFactory(new SpannableFactory());
            mBinding.content.setMovementMethod(SelectableLinkMovementMethod.getInstance());
            //            mBinding.content.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        }

        private void initBGAnimator() {
            mBGAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), mBGHighlightBGColor, Color.TRANSPARENT);
            mBGAnimator.setDuration(1500);
            mBGAnimator.addUpdateListener(animation -> {
                VerseArabicHighlightSpan span = verseSpanToAnimate.get();
                if (span != null) {
                    span.setBackgroundColor((int) animation.getAnimatedValue());
                    mBinding.content.invalidate();
                }
            });
            mBGAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    VerseArabicHighlightSpan span = verseSpanToAnimate.get();
                    if (span != null) {
                        span.setBackgroundColor(
                            span.verseNo == recitingVerseNo /*|| isVerseQuickActionsOpened*/ ? mBGHighlightBGColor : Color.TRANSPARENT);
                    }
                }
            });
        }

        public void setSectionModel(QuranPageSectionModel pageModel) {
            initWithModel(pageModel);
        }

        private void initWithModel(QuranPageSectionModel sectionModel) {
            mBinding.chapterTitle.setVisibility(sectionModel.showTitle() ? VISIBLE : GONE);
            mBinding.bismillah.setVisibility(sectionModel.showBismillah() ? VISIBLE : GONE);
            mBinding.chapterTitle.setChapterNumber(sectionModel.getChapterNo());
            setupArabicTextSize();

            initVerses(sectionModel);

            post(() -> {
                if (
                    sectionModel.hasVerse(sectionModel.getScrollHighlightPendingVerseNo())) {
                    highlightOnScroll(sectionModel);
                    sectionModel.setScrollHighlightPendingVerseNo(-1);
                }
            });
        }

        private void setupArabicTextSize() {
            if (mActivity != null) {
                mActivity.mVerseDecorator.setTextSizeArabic(mBinding.content);
            }
        }

        private void highlightOnScroll(QuranPageSectionModel sectionModel) {
            if (recitingVerseNo != -1) {
                return;
            }

            mBinding.content.post(() -> {
                VerseArabicHighlightSpan verseSpan = findVerseSpan(sectionModel.getScrollHighlightPendingVerseNo());
                if (verseSpan == null) {
                    return;
                }
                highlightVerseSpan(verseSpan);
            });
        }

        private void initVerses(QuranPageSectionModel sectionModel) {
            setSpannable(sectionModel.getContentSpannable());

            if (!mScrollHighlightInProgress) {
                Spannable spannable2 = (Spannable) mBinding.content.getText();
                VerseArabicHighlightSpan[] spans = spannable2.getSpans(0, spannable2.length(),
                    VerseArabicHighlightSpan.class);

                boolean anyReciting = false;
                for (VerseArabicHighlightSpan span : spans) {
                    if (mActivity.mPlayer != null) {
                        anyReciting = mActivity.mPlayer.isReciting(sectionModel.getChapterNo(), span.verseNo);
                    }
                    span.setBackgroundColor(anyReciting ? mBGHighlightBGColor : Color.TRANSPARENT);

                    if (anyReciting) {
                        recitingVerseNo = span.verseNo;
                    }
                }
                if (!anyReciting) {
                    recitingVerseNo = -1;
                }
            }
        }

        private void setSpannable(CharSequence spannable) {
            mBinding.content.setText(spannable, TextView.BufferType.SPANNABLE);
        }


        public void highlightVerseSpan(VerseArabicHighlightSpan verseSpan) {
            verseSpan.setBackgroundColor(mBGHighlightBGColor);
            mBinding.content.invalidate();

            mScrollHighlightInProgress = true;

            verseSpanToAnimate.set(verseSpan);
            mHandler.removeCallbacks(mBGAnimationRunnable);
            mHandler.postDelayed(mBGAnimationRunnable, 2000);
        }

        public VerseArabicHighlightSpan findVerseSpan(int verseNo) {
            VerseArabicHighlightSpan span = null;
            CharSequence verseText = mBinding.content.getText();
            if (!(verseText instanceof Spannable) || TextUtils.isEmpty(verseText)) {
                return null;
            }

            Spannable spannableStr = (Spannable) verseText;
            final VerseArabicHighlightSpan[] spans = spannableStr.getSpans(0, spannableStr.length(),
                VerseArabicHighlightSpan.class);

            if (spans == null) {
                return null;
            }

            for (VerseArabicHighlightSpan span1 : spans) {
                if (span1.verseNo == verseNo) {
                    span = span1;
                    break;
                }
            }
            return span;
        }

        public int getVerseTopOffset(VerseArabicHighlightSpan verseSpan) {
            Rect rectWithinSection = new Rect();
            TextView tv = mBinding.content;

            Spannable text = (Spannable) tv.getText();
            Layout layout = tv.getLayout();

            int lineForStart = layout.getLineForOffset(text.getSpanStart(verseSpan));
            layout.getLineBounds(lineForStart, rectWithinSection);

            rectWithinSection.top += ViewKt.getRelativeTopRecursive(tv, QuranPageView.class, false);

            return rectWithinSection.top;
        }

        private void animateBG() {
            if (mBGAnimator.isRunning()) {
                mBGAnimator.cancel();
            }
            mBGAnimator.start();
        }

        private void removeHighlight(int verseNo) {
            CharSequence verseText = mBinding.content.getText();
            if (!(verseText instanceof Spannable) || TextUtils.isEmpty(verseText)) {
                return;
            }

            SpannableString spannableStr = (SpannableString) verseText;
            final VerseArabicHighlightSpan[] spans = spannableStr.getSpans(0, spannableStr.length(),
                VerseArabicHighlightSpan.class);

            if (spans == null) {
                return;
            }

            for (VerseArabicHighlightSpan span : spans) {
                if (span.verseNo == verseNo) {
                    span.setBackgroundColor(0);
                    mBinding.content.invalidate();
                    break;
                }
            }
        }
    }
}
