package com.quranapp.android.readerhandler;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.ResUtils;
import com.quranapp.android.R;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.reader.ScriptUtils;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.verse.VerseUtils;

public class VerseDecorator implements Destroyable {
    private Context mContext;

    public int mTextColorArabic;
    public int mTextColorNonArabic;
    public int mTextColorAuthor;

    public float mTextSizeArabic;
    public float mTextSizeTransl;
    public float mTextSizeAuthor;

    private Typeface mFontArabic;
    private Typeface mFontVerseSerial;
    private Typeface mFontVerseSerialFallback;
    private Typeface mFontUrdu;

    private float savedTextSizeArabicMult;
    private float savedTextSizeTranslMult;

    public VerseDecorator(Context context) {
        mContext = context;

        collectValues();
    }

    private void collectValues() {
        mTextColorArabic = ContextCompat.getColor(mContext, R.color.colorTextArabic);
        mTextColorNonArabic = ContextCompat.getColor(mContext, R.color.colorTextNoArabic);
        mTextColorAuthor = ContextCompat.getColor(mContext, R.color.colorSecondary);

        mTextSizeTransl = ResUtils.getDimenPx(mContext, R.dimen.dmnReaderTextSizeTransl);
        mTextSizeAuthor = ResUtils.getDimenPx(mContext, R.dimen.dmnCommonSize3);
        mFontVerseSerial = ResUtils.getFont(mContext, R.font.uthmanic_hafs1);
        mFontVerseSerialFallback = ResUtils.getFont(mContext, R.font.traditional_arabic);

        mFontUrdu = ResUtils.getFont(mContext, R.font.font_urdu);

        onSharedPrefChanged();
    }

    public void onSharedPrefChanged() {
        savedTextSizeArabicMult = SPReader.getSavedTextSizeMultArabic(mContext);
        savedTextSizeTranslMult = SPReader.getSavedTextSizeMultTransl(mContext);

        String savedScript = SPReader.getSavedScript(mContext);

        mFontArabic = ResUtils.getFont(mContext, ScriptUtils.getScriptFontRes(savedScript));
        mTextSizeArabic = ResUtils.getDimenPx(mContext, ScriptUtils.getScriptFontDimenRes(savedScript));
    }

    public CharSequence setupArabicText(String arabicText, int verseNo) {
        return setupArabicText(arabicText, verseNo, -1);
    }

    public CharSequence setupArabicText(String arabicText, int verseNo, int textSize) {
        Typeface font = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? mFontVerseSerial : mFontVerseSerialFallback;
        return VerseUtils.decorateVerse(arabicText, verseNo, mFontArabic, font, textSize);
    }

    public CharSequence setupArabicTextQuranPage(String arabicText, int verseNo) {
        boolean isHigherAPI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        return VerseUtils.decorateVerseQuranPage(arabicText, verseNo, mFontArabic,
                isHigherAPI ? mFontVerseSerial : mFontVerseSerialFallback);
    }

    public SpannableString setupTranslText(String translText, boolean isUrdu) {
        return setupTranslText(translText, -1, -1, isUrdu);
    }

    public SpannableString setupTranslText(String translText, int translClr, int txtSize, boolean isUrdu) {
        Typeface fontTransl = isUrdu ? mFontUrdu : Typeface.SANS_SERIF;
        return VerseUtils.decorateSingleTranslSimple(translText, translClr, txtSize, fontTransl);
    }

    public SpannableString setupAuthorText(String author, boolean isUrdu) {
        return setupAuthorText(author, mTextColorAuthor, (int) mTextSizeAuthor, isUrdu);
    }

    public SpannableString setupAuthorText(String author, int authorClr, int txtSize, boolean isUrdu) {
        Typeface fontAuthor = isUrdu ? mFontUrdu : Typeface.SANS_SERIF;
        return VerseUtils.prepareTranslAuthorText(author, authorClr, txtSize, fontAuthor, false);
    }

    public void setTextSizeArabic(TextView textView) {
        setTextSizeArabic(textView, savedTextSizeArabicMult);
    }

    public void setTextSizeArabic(TextView textView, float mult) {
        float textSize = mTextSizeArabic * mult;
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    public void setTextSizeTransl(TextView textView) {
        setTextSizeTransl(textView, savedTextSizeTranslMult);
    }

    public void setTextSizeTransl(TextView textView, float mult) {
        float textSize = mTextSizeTransl * mult;
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    public void setFontArabic(TextView textView) {
        textView.setTypeface(mFontArabic);
    }

    public void setTextColorArabic(TextView arabicTextView) {
        arabicTextView.setTextColor(mTextColorArabic);
    }

    public void setTextColorNonArabic(TextView textView) {
        textView.setTextColor(mTextColorNonArabic);
    }

    @Override
    public void destroy() {
        mContext = null;
        mFontArabic = null;
        mFontVerseSerial = null;
        mFontVerseSerialFallback = null;
        mFontUrdu = null;
    }
}
