package com.quranapp.android.utils.univ;

import android.text.Spannable;

public class SpannableFactory extends Spannable.Factory {
    @Override
    public Spannable newSpannable(CharSequence source) {
        if (source instanceof Spannable) {
            return (Spannable) source;
        }

        return super.newSpannable(source);
    }
}
