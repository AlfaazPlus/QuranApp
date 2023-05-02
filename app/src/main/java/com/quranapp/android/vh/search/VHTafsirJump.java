package com.quranapp.android.vh.search;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.components.search.TafsirJumpModel;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.widgets.IconedTextView;

public class VHTafsirJump extends VHSearchResultBase {
    private final IconedTextView mTextView;

    public VHTafsirJump(IconedTextView textView, boolean applyMargins) {
        super(textView);
        mTextView = textView;

        ViewPaddingKt.updatePaddings(textView, Dimen.dp2px(textView.getContext(), 15),
            Dimen.dp2px(textView.getContext(), 10));
        setupJumperView(textView, applyMargins);
    }

    @Override
    public void bind(@NonNull SearchResultModelBase parentModel, int pos) {
        TafsirJumpModel model = (TafsirJumpModel) parentModel;

        mTextView.setDrawables(ContextKt.drawable(mTextView.getContext(), R.drawable.dr_icon_tafsir), null, null, null);
        mTextView.setText(makeName(itemView.getContext(), model.titleText, model.chapterNameText));
        mTextView.setOnClickListener(v -> ReaderFactory.startTafsir(v.getContext(), model.chapterNo, model.verseNo));
    }

    private CharSequence makeName(Context ctx, String text, String subtext) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        String SANS_SERIF = "sans-serif";
        ColorStateList nameClr = ContextCompat.getColorStateList(ctx, R.color.color_reader_spinner_item_label);

        SpannableString nameSS = new SpannableString(text);
        setSpan(nameSS, new TextAppearanceSpan(SANS_SERIF, Typeface.BOLD, -1, nameClr, null));
        ssb.append(nameSS);

        if (!TextUtils.isEmpty(subtext)) {
            ColorStateList translClr = ContextKt.colorStateList(ctx, R.color.color_reader_spinner_item_label2);
            int dimen2 = ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2);

            SpannableString translSS = new SpannableString(subtext);
            setSpan(translSS, new TextAppearanceSpan(SANS_SERIF, Typeface.NORMAL, dimen2, translClr, null));
            ssb.append("\n").append(translSS);
        }
        return ssb;
    }

    private void setSpan(SpannableString ss, Object obj) {
        ss.setSpan(obj, 0, ss.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
