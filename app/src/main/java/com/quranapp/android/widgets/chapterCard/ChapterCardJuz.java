/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/2/2022.
 * All rights reserved.
 */

package com.quranapp.android.widgets.chapterCard;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;

public class ChapterCardJuz extends ChapterCard {
    public ChapterCardJuz(@NonNull Context context) {
        this(context, null);
    }

    public ChapterCardJuz(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChapterCardJuz(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View createRightView() {
        AppCompatTextView v = new AppCompatTextView(getContext());
        v.setId(R.id.chapterCardVerseCount);

        LayoutParams p = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        p.setMarginStart(Dimen.dp2px(getContext(), 10));
        v.setLayoutParams(p);

        v.setGravity(Gravity.END);
        v.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_reader_spinner_item_label));
        addView(v);
        return v;
    }

    public void setVersesCount(String versesInJuzPrefix, int fromVerse, int toVerse) {
        View verseCount = findViewById(R.id.chapterCardVerseCount);
        if (verseCount instanceof TextView) {
            versesInJuzPrefix += ":\n";
            String versesInJuzCount = fromVerse + "-" + toVerse;

            SpannableString spannablePrefix = new SpannableString(versesInJuzPrefix);
            SpannableString spannableCount = new SpannableString(versesInJuzCount);

            spannablePrefix.setSpan(new RelativeSizeSpan(0.75f), 0, spannablePrefix.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableCount.setSpan(new TypefaceSpan("sans-serif-medium"), 0, spannableCount.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

            ((TextView) verseCount).setText(TextUtils.concat(spannablePrefix, spannableCount));
        }
    }
}
