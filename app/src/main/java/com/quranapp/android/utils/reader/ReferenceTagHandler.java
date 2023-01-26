package com.quranapp.android.utils.reader;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static com.quranapp.android.utils.quran.parser.QuranTranslParserJSON.FOOTNOTE_REF_ATTR_INDEX;
import static com.quranapp.android.utils.quran.parser.QuranTranslParserJSON.FOOTNOTE_REF_TAG;
import static com.quranapp.android.utils.quran.parser.QuranTranslParserJSON.REFERENCE_ATTR_CHAPTER_NO;
import static com.quranapp.android.utils.quran.parser.QuranTranslParserJSON.REFERENCE_ATTR_VERSES;
import static com.quranapp.android.utils.quran.parser.QuranTranslParserJSON.REFERENCE_TAG;

import android.text.Editable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.quranapp.android.utils.parser.HtmlParser;
import com.quranapp.android.utils.quran.parser.QuranTranslParserJSON;
import com.peacedesign.android.utils.span.RoundedBG_FGSpan;
import com.peacedesign.android.utils.span.SuperscriptSpan2;

import org.xml.sax.Attributes;

import java.util.Set;

public class ReferenceTagHandler implements HtmlParser.TagHandler {
    private final Set<String> mTranslSlugs;
    private final int mRefTxtColor;
    private final int mRefBGColor;
    private final int mRefBGColorPressed;
    private final OnReferenceTagClickCallback mRefTagClickCallback;
    private final OnFootnoteReferenceTagClickCallback mFootnoteRefTagClickCallback;
    private int refTagStartIndex = 0;
    private int footnoteRefTagStartIndex = 0;
    private String chapter = "-1";
    private String verses = "-1";
    private String footnoteNo = "-1";

    /**
     * @param translSlugs         Slug of the translation which created this reference.
     * @param refTagClickCallback Callback to invoke if the clicked tag is {@link QuranTranslParserJSON#REFERENCE_TAG}
     */

    public ReferenceTagHandler(Set<String> translSlugs, int refTxtColor, int refBGColor, int refBGColorPres, OnReferenceTagClickCallback refTagClickCallback) {
        this(translSlugs, refTxtColor, refBGColor, refBGColorPres, refTagClickCallback, null);
    }

    /**
     * @param translSlug          Slug of the translation which created this reference.
     * @param refTagClickCallback Callback to invoke if the clicked tag is {@link QuranTranslParserJSON#REFERENCE_TAG}
     * @param fnTagClickCallback  Callback to invoke if the clicked tag is {@link QuranTranslParserJSON#FOOTNOTE_REF_TAG}
     */
    public ReferenceTagHandler(Set<String> translSlug, int refTxtColor, int refBGColor, int refBGColorPres,
                               OnReferenceTagClickCallback refTagClickCallback, OnFootnoteReferenceTagClickCallback fnTagClickCallback) {
        mTranslSlugs = translSlug;
        mRefTxtColor = refTxtColor;
        mRefBGColor = refBGColor;
        mRefBGColorPressed = refBGColorPres;
        mRefTagClickCallback = refTagClickCallback;
        mFootnoteRefTagClickCallback = fnTagClickCallback;
    }

    @Override
    public boolean handleTag(boolean opening, String tag, Editable output, Attributes attributes) {
        if (REFERENCE_TAG.equals(tag)) {
            if (opening) {
                refTagStartIndex = output.length();
                chapter = HtmlParser.getValue(attributes, REFERENCE_ATTR_CHAPTER_NO);
                verses = HtmlParser.getValue(attributes, REFERENCE_ATTR_VERSES);
            } else {
                final int refTagEndIndex = output.length();
                setQuranRefSpans(output, refTagStartIndex, refTagEndIndex, Integer.parseInt(chapter), verses);
            }
        } else if (FOOTNOTE_REF_TAG.equals(tag)) {
            if (opening) {
                footnoteRefTagStartIndex = output.length();
                footnoteNo = HtmlParser.getValue(attributes, FOOTNOTE_REF_ATTR_INDEX);
            } else {
                final int footnoteRefTagEndIndex = output.length();
                setFootnoteRefSpans(output, footnoteRefTagStartIndex, footnoteRefTagEndIndex, footnoteNo);
            }
        }
        return true;
    }

    private void setQuranRefSpans(Editable output, int start, int end, int chapter, String verses) {
        final int flag = SPAN_EXCLUSIVE_EXCLUSIVE;
        final int[] bgColors = {mRefBGColor, mRefBGColorPressed};
        output.setSpan(new RoundedBG_FGSpan(bgColors, mRefTxtColor), start, end, flag);
        output.setSpan(new TypefaceSpan("sans-serif-medium"), start, end, flag);
        output.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (mRefTagClickCallback != null) {
                    mRefTagClickCallback.onClick(mTranslSlugs, chapter, verses);
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, start, end, flag);
    }


    private void setFootnoteRefSpans(Editable output, int start, int end, String footnoteNo) {
        try {
            final int footnoteNoInt = Integer.parseInt(footnoteNo);

            final int flag = SPAN_EXCLUSIVE_EXCLUSIVE;
            output.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    if (mFootnoteRefTagClickCallback != null) {
                        mFootnoteRefTagClickCallback.onClick(footnoteNoInt);
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            }, start, end, flag);
            output.setSpan(new SuperscriptSpan2(), start, end, flag); // set <sup>
            output.setSpan(new RelativeSizeSpan(0.8f), start, end, flag); // set size
            output.setSpan(new ForegroundColorSpan(mRefTxtColor), start, end, flag);// set color
        } catch (NumberFormatException e) {e.printStackTrace();}
    }

    public interface OnReferenceTagClickCallback {
        void onClick(Set<String> translSlugs, int chapter, String verses);
    }

    public interface OnFootnoteReferenceTagClickCallback {
        void onClick(int footnoteNo);
    }
}
