package com.quranapp.android.utils.span;

import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;

public class VerseArabicHighlightSpan extends BackgroundColorSpan {
    private int BGColor;
    public int verseNo;

    public VerseArabicHighlightSpan(int verseNo) {
        super(0);
        this.verseNo = verseNo;
    }

    public VerseArabicHighlightSpan(Parcel src) {
        super(src);
        BGColor = src.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(BGColor);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.bgColor = getBackgroundColor();
    }


    public void setBackgroundColor(int backgroundColor) {
        BGColor = backgroundColor;
    }

    @Override
    public int getBackgroundColor() {
        return BGColor;
    }
}