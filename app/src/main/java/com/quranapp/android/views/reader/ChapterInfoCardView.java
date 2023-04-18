package com.quranapp.android.views.reader;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.RoundedBG_FGSpan;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityChapInfo;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytChapterInfocardBinding;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.univ.Keys;

import java.util.Locale;
import java.util.Objects;

public class ChapterInfoCardView extends CardView {
    private final LytChapterInfocardBinding mBinding;
    private boolean mIsExpanded;
    private final int spanBGColor;
    private final int spanTxtColor;
    private QuranMeta.ChapterMeta mChapterInfoMeta;
    private ActivityReader mActivityReader;
    private final boolean mIsRTL;

    public ChapterInfoCardView(@NonNull ActivityReader activityReader) {
        this(activityReader, null);
        mActivityReader = activityReader;
    }

    public ChapterInfoCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChapterInfoCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mIsRTL = getContext().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        spanBGColor = ContextCompat.getColor(context, R.color.colorBGPage2);
        spanTxtColor = ContextCompat.getColor(context, R.color.colorText);

        mBinding = LytChapterInfocardBinding.inflate(LayoutInflater.from(context));

        expand(false);
        setVisibility(GONE);

        mBinding.header.setOnClickListener(v -> expand(!mIsExpanded));
        mBinding.content.setOnClickListener(v -> {
            if (mChapterInfoMeta != null) {
                Intent intent = new Intent(getContext(), ActivityChapInfo.class);
                intent.putExtra(Keys.READER_KEY_CHAPTER_NO, mChapterInfoMeta.chapterNo);
                intent.putExtra(Keys.KEY_LANGUAGE, Locale.getDefault().toLanguageTag());
                mActivityReader.startActivity4Result(intent, null);
            }
        });

        setRadius(Dimen.dp2px(getContext(), 10));
        setCardElevation(Dimen.dp2px(getContext(), 4));
        setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorBGChapterInfoCard));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LayoutParamsKt.updateMargins(p, Dimen.dp2px(getContext(), 5));
        p.bottomMargin = Dimen.dp2px(getContext(), 10);
        setLayoutParams(p);
        addView(mBinding.getRoot());
    }

    private void expand(boolean expand) {
        mIsExpanded = expand;


        int defDegree = mIsRTL ? 180 : 0;
        mBinding.arrow.setRotation(mIsExpanded ? -90 : defDegree);

        ViewGroup.LayoutParams params = mBinding.content.getLayoutParams();
        params.height = expand ? WRAP_CONTENT : 0;
        mBinding.content.requestLayout();
    }

    public void setInfo(QuranMeta.ChapterMeta chapterInfoMeta) {
        mChapterInfoMeta = chapterInfoMeta;

        if (chapterInfoMeta == null) {
            setVisibility(GONE);
            return;
        } else {
            setVisibility(VISIBLE);
        }

        boolean isMeccan = Objects.equals(chapterInfoMeta.revelationType, "meccan");

        String nameTrans = getContext().getString(R.string.strLabelSurah, chapterInfoMeta.getName());
        mBinding.title.setText(nameTrans);
        mBinding.revlType.setText(isMeccan ? R.string.strTitleMakki : R.string.strTitleMadani);

        mBinding.image.setImageResource(isMeccan ? R.drawable.dr_makkah_old : R.drawable.dr_madina_old);

        String verses = mActivityReader.str(R.string.strTitleChapInfoVerses) + ": " + chapterInfoMeta.verseCount;
        String rukus = mActivityReader.str(R.string.strTitleChapInfoRukus) + ": " + chapterInfoMeta.rukuCount;
        String order = mActivityReader.str(R.string.strLabelOrder) + ": " + chapterInfoMeta.revelationOrder;

        CharSequence finalText = TextUtils.concat(setSpans(verses), " ", setSpans(rukus), " ", setSpans(order));
        mBinding.info.setText(finalText);
    }

    private Spannable setSpans(String string) {
        SpannableString ss = new SpannableString(string);
        RoundedBG_FGSpan span = new RoundedBG_FGSpan(spanBGColor, spanTxtColor);
        span.setPaddingH(Dimen.dp2px(getContext(), 5));
        ss.setSpan(span, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }
}
