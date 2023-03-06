package com.quranapp.android.views.reader;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.utils.quran.QuranUtils;

public class ChapterIcon extends AppCompatTextView {
    private int mChapterNumber;
    private boolean mIncludePrefix;
    private ColorStateList mColor;
    private final int mTextSize;

    public ChapterIcon(@NonNull Context context) {
        this(context, null);
    }

    public ChapterIcon(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChapterIcon(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChapterIcon, defStyleAttr, 0);
        mChapterNumber = a.getInteger(R.styleable.ChapterIcon_chapterNumber, -1);
        mIncludePrefix = a.getBoolean(R.styleable.ChapterIcon_includePrefix, true);
        mColor = a.getColorStateList(R.styleable.ChapterIcon_android_textColor);
        mTextSize = a.getDimensionPixelSize(R.styleable.ChapterIcon_android_textSize, Dimen.sp2px(getContext(), 31));
        a.recycle();

        init();
    }

    private void init() {
        TextViewCompat.setTextAppearance(this, R.style.TextAppearanceChapterIcon);

        if (mColor == null) {
            mColor = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorText));
        }

        setTextColor(mColor);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        setupChapterIcon();
    }


    private void setupChapterIconNew() {
        setText(QuranUtils.getChapterIconValue(mChapterNumber));
    }

    private void setupChapterIcon() {
        String chapterName = null;

        String unicode = QuranUtils.getChapterIconUnicode(mChapterNumber);
        if (unicode != null) {
            chapterName = unicode;
            if (mIncludePrefix) {
                chapterName += QuranUtils.getChapterIconUnicode(0);
            }
        }

        setText(chapterName);
    }

    public void setChapterNumber(int chapterNumber, boolean withSurahPrefix) {
        mChapterNumber = chapterNumber;
        mIncludePrefix = withSurahPrefix;

        setupChapterIcon();
    }

    public void setChapterNumber(int chapterNumber) {
        setChapterNumber(chapterNumber, true);
    }

    public void setChapterAppearance(int textAppearanceResId) {
        TextViewCompat.setTextAppearance(this, textAppearanceResId);
    }
}
