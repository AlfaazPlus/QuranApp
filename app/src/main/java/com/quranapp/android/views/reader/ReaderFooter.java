package com.quranapp.android.views.reader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_VERSES;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytReaderFooterBinding;
import com.quranapp.android.reader_managers.Navigator;
import com.quranapp.android.reader_managers.ReaderParams;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.quran.QuranUtils;

import kotlin.Pair;

@SuppressLint("ViewConstructor")
public class ReaderFooter extends FrameLayout {
    private final LytReaderFooterBinding mBinding;
    private final ActivityReader mActivity;
    private final ReaderParams mReaderParams;
    private final Navigator mNavigator;
    private int mFullChapterLabelRes;

    public ReaderFooter(@NonNull ActivityReader activity, Navigator navigator) {
        super(activity);

        mActivity = activity;
        mReaderParams = activity.mReaderParams;
        mNavigator = navigator;

        mBinding = LytReaderFooterBinding.inflate(LayoutInflater.from(activity));
        addView(mBinding.getRoot());
        init(mBinding);
    }

    private void init(LytReaderFooterBinding binding) {
        setupIcons(getContext(), binding);

        binding.prevChapterName.setSelected(true);
        binding.nextChapterName.setSelected(true);

        binding.btnPrevChapter.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.previousChapter();
            }
        });
        binding.btnNextChapter.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.nextChapter();
            }
        });
        binding.btnChapterTop.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.scrollToTop();
            }
        });
        binding.btnPrevVerse.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.previousVerse();
            }
        });
        binding.btnNextVerse.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.nextVerse();
            }
        });
        binding.fullChapter.setOnClickListener(v -> {
            if (mNavigator != null) {
                if (mFullChapterLabelRes == R.string.strLabelFullChapter) {
                    mNavigator.readFullChapter();
                } else if (mFullChapterLabelRes == R.string.strLabelContinueChapter) {
                    mNavigator.continueChapter();
                }
            }
        });

        binding.btnPrevJuz.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.previousJuz();
            }
        });
        binding.btnNextJuz.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.nextJuz();
            }
        });
        binding.btnJuzTop.setOnClickListener(v -> {
            if (mNavigator != null) {
                mNavigator.scrollToTop();
            }
        });
    }

    private void setupIcons(Context context, LytReaderFooterBinding binding) {
        boolean isRTL = context.getResources().getBoolean(R.bool.isRTL);

        binding.textPrevChapter.setDrawables(getStartPointingArrow(context, isRTL), null, null, null);
        binding.textNextChapter.setDrawables(null, null, getEndPointingArrow(context, isRTL), null);
        binding.btnChapterTop.setDrawables(null, getTopPointingArrow(context), null, null);

        binding.btnPrevVerse.setDrawables(null, null, null, getStartPointingArrow(context, isRTL));
        binding.btnNextVerse.setDrawables(null, null, null, getEndPointingArrow(context, isRTL));

        binding.btnPrevJuz.setDrawables(null, null, null, getStartPointingArrow(context, isRTL));
        binding.btnNextJuz.setDrawables(null, null, null, getEndPointingArrow(context, isRTL));
        binding.btnJuzTop.setDrawables(null, getTopPointingArrow(context), null, null);
    }

    private Drawable getStartPointingArrow(Context context, boolean isRTL) {
        Drawable arrowLeft = ContextKt.drawable(context, R.drawable.dr_icon_arrow_left);

        if (!isRTL) return arrowLeft;
        return DrawableUtils.rotate(context, arrowLeft, 180);
    }

    private Drawable getEndPointingArrow(Context context, boolean isRTL) {
        Drawable arrowLeft = ContextKt.drawable(context, R.drawable.dr_icon_arrow_left);

        if (isRTL) {
            return arrowLeft;
        }

        return DrawableUtils.rotate(context, arrowLeft, 180);
    }

    private Drawable getTopPointingArrow(Context context) {
        Drawable arrowLeft = ContextKt.drawable(context, R.drawable.dr_icon_arrow_left);
        if (arrowLeft == null) return null;
        return DrawableUtils.rotate(context, arrowLeft, 90);
    }

    public void clearParent() {
        ViewKt.removeView(this);
    }

    public void setupBottomNavigator() {
        final QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
        switch (mReaderParams.readType) {
            case READER_READ_TYPE_CHAPTER: setupFooterForChapter();
                break;
            case READER_READ_TYPE_VERSES:
                Pair<Integer, Integer> range = mReaderParams.verseRange;
                if (QuranUtils.doesRangeDenoteSingle(range)) {
                    setupFooterForSingle();
                } else {
                    setupFooterForVersesRange(range);
                }
                break;
            case READER_READ_TYPE_JUZ: setupFooterForJuz();
                break;
            default: setupFooterForNone();
                break;
        }

        if (mReaderParams.readType == READER_READ_TYPE_JUZ) {
            String juzNoFormat = getContext().getString(R.string.strLabelJuzNo);

            setPrevJuz(mNavigator.getCurrJuzNo() == 1 ? null : String.format(juzNoFormat, mNavigator.getPrevJuzNo()));
            setNextJuz(mNavigator.getCurrJuzNo() == QuranMeta.totalJuzs()
                ? null : String.format(juzNoFormat, mNavigator.getNextJuzNo()));

            return;
        }

        Pair<Integer, Integer> verseRange = mReaderParams.verseRange;
        int chapterNo = mReaderParams.currChapter.getChapterNumber();
        if (verseRange.getSecond() < quranMeta.getChapterVerseCount(chapterNo)) {
            mFullChapterLabelRes = R.string.strLabelContinueChapter;
        } else {
            mFullChapterLabelRes = R.string.strLabelFullChapter;
        }

        SpannableString SSFullChapTitle = new SpannableString(getContext().getString(mFullChapterLabelRes));
        SSFullChapTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, SSFullChapTitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        int txtSizeFullChapName = ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize3_5);
        int txtClrFullChapName = ContextKt.color(getContext(), R.color.colorIcon);
        SpannableString SSFullChapName = new SpannableString(mReaderParams.currChapter.getName());
        SSFullChapName.setSpan(new AbsoluteSizeSpan(txtSizeFullChapName), 0, SSFullChapName.length(),
            SPAN_EXCLUSIVE_EXCLUSIVE);
        SSFullChapName.setSpan(new ForegroundColorSpan(txtClrFullChapName), 0, SSFullChapName.length(),
            SPAN_EXCLUSIVE_EXCLUSIVE);

        mBinding.fullChapter.setText(TextUtils.concat(SSFullChapTitle, "\n", SSFullChapName));

        if (mNavigator.getCurrChapterNo() == 1) {
            setPrevChapter(null);
        } else {
            setPrevChapter(quranMeta.getChapterName(getContext(), mNavigator.getPrevChapterNo()));
        }
        if (mNavigator.getCurrChapterNo() == QuranMeta.totalChapters()) {
            setNextChapter(null);
        } else {
            setNextChapter(quranMeta.getChapterName(getContext(), mNavigator.getNextChapterNo()));
        }

        String verseNoFormat = getContext().getString(R.string.strLabelVerseNo);

        if (mNavigator.getCurrVerseNo() == 1) {
            setPrevVerse(null);
        } else {
            setPrevVerse(String.format(verseNoFormat, mNavigator.getPrevVerseNo()));
        }
        if (mNavigator.getCurrVerseNo() == quranMeta.getChapterVerseCount(mNavigator.getCurrChapterNo())) {
            setNextVerse(null);
        } else {
            setNextVerse(String.format(verseNoFormat, mNavigator.getNextVerseNo()));
        }
    }

    private void setupFooterForSingle() {
        setupFooterForNone();
        mBinding.verseNavigator.setVisibility(VISIBLE);
        mBinding.btnPrevVerse.setVisibility(VISIBLE);
        mBinding.btnNextVerse.setVisibility(VISIBLE);
    }

    private void setupFooterForChapter() {
        setupFooterForNone();
        mBinding.chapterNavigator.setVisibility(VISIBLE);
    }

    private void setupFooterForVersesRange(Pair<Integer, Integer> range) {
        setupFooterForNone();
        mBinding.verseNavigator.setVisibility(VISIBLE);
        mBinding.rangeMessage.setVisibility(VISIBLE);

        String rangeText = range.getFirst() + "-" + range.getSecond();
        SpannableString rangeTextSpannable = new SpannableString(rangeText);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(
            ContextCompat.getColor(getContext(), R.color.colorPrimary));
        rangeTextSpannable.setSpan(new TypefaceSpan("sans-serif-black"), 0, rangeText.length(),
            SPAN_EXCLUSIVE_EXCLUSIVE);
        rangeTextSpannable.setSpan(colorSpan, 0, rangeText.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence textFin = TextUtils.concat(getContext().getString(R.string.strMsgYouAreReadingVerses), " ",
            rangeTextSpannable);
        mBinding.rangeMessage.setText(textFin);
    }

    private void setupFooterForJuz() {
        setupFooterForNone();
        mBinding.juzNavigator.setVisibility(VISIBLE);
    }

    private void setupFooterForNone() {
        mBinding.chapterNavigator.setVisibility(GONE);
        mBinding.verseNavigator.setVisibility(GONE);
        mBinding.btnPrevVerse.setVisibility(GONE);
        mBinding.btnNextVerse.setVisibility(GONE);
        mBinding.rangeMessage.setText(null);
        mBinding.rangeMessage.setVisibility(GONE);
        mBinding.juzNavigator.setVisibility(GONE);
    }

    private void setButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1 : 0.4f);
    }

    private void setPrevChapter(String chapterName) {
        boolean hasChapterName = !TextUtils.isEmpty(chapterName);
        setButtonEnabled(mBinding.btnPrevChapter, hasChapterName);
        mBinding.prevChapterName.setText(hasChapterName ? chapterName : "");
        mBinding.prevChapterName.setVisibility(hasChapterName ? VISIBLE : GONE);
    }

    private void setNextChapter(String chapterName) {
        boolean hasChapterName = !TextUtils.isEmpty(chapterName);
        setButtonEnabled(mBinding.btnNextChapter, hasChapterName);
        mBinding.nextChapterName.setText(hasChapterName ? chapterName : "");
        mBinding.nextChapterName.setVisibility(hasChapterName ? VISIBLE : GONE);
    }

    private void setPrevVerse(String verseName) {
        boolean hasVerseName = !TextUtils.isEmpty(verseName);
        setButtonEnabled(mBinding.btnPrevVerse, hasVerseName);
        mBinding.btnPrevVerse.setText(hasVerseName ? verseName : "");
    }

    private void setNextVerse(String verseName) {
        boolean hasVerseName = !TextUtils.isEmpty(verseName);
        setButtonEnabled(mBinding.btnNextVerse, hasVerseName);
        mBinding.btnNextVerse.setText(hasVerseName ? verseName : "");
    }

    private void setPrevJuz(String verseName) {
        boolean hasJuzName = !TextUtils.isEmpty(verseName);
        setButtonEnabled(mBinding.btnPrevJuz, hasJuzName);
        mBinding.btnPrevJuz.setText(hasJuzName ? verseName : "");
    }

    private void setNextJuz(String verseName) {
        boolean hasJuzName = !TextUtils.isEmpty(verseName);
        setButtonEnabled(mBinding.btnNextJuz, hasJuzName);
        mBinding.btnNextJuz.setText(hasJuzName ? verseName : "");
    }
}
