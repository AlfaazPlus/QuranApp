/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.NestedScrollView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.radio.PeaceRadioButton;
import com.peacedesign.android.widget.radio.PeaceRadioGroup;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.subcomponents.TranslationBook;
import com.quranapp.android.utils.sp.SPReader;

import java.util.Set;

public class FragEditorTransls extends FragEditorBase {
    private PeaceRadioGroup mRadioGroup;

    public static FragEditorTransls newInstance() {
        return new FragEditorTransls();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        NestedScrollView scrollView = new NestedScrollView(inflater.getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        LinearLayout layout = new LinearLayout(inflater.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(layout, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return scrollView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout layout = (LinearLayout) ((ViewGroup) view).getChildAt(0);
        Context ctx = view.getContext();

        Set<String> translSlugs = SPReader.getSavedTranslations(ctx);
        //        QuranTransl.prepareInstance(ctx, translSlugs, transl -> initViews(transl, translSlugs, layout));
    }

    /*private void initViews(QuranTransl transl, Set<String> translSlugs, LinearLayout parent) {
        initMsgView(parent);
        initRadios(parent, translSlugs, transl);
    }*/

    private void initMsgView(LinearLayout parent) {
        Context ctx = parent.getContext();
        AppCompatTextView msgView = new AppCompatTextView(ctx);
        ViewUtils.setPaddingHorizontal(msgView, dp2px(ctx, 15));
        ViewUtils.setPaddingVertical(msgView, dp2px(ctx, 10));

        msgView.setText(R.string.strMsgEditShareTransl);
        msgView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
        msgView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen(ctx, R.dimen.dmnCommonSize2_5));
        msgView.setTextColor(color(ctx, R.color.colorText2));

        parent.addView(msgView, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    /*private void initRadios(LinearLayout parent, Set<String> translSlugs, QuranTransl transl) {
        mRadioGroup = new PeaceRadioGroup(parent.getContext());
        for (String slug : translSlugs) {
            makeRadio(mRadioGroup, transl.getParsedTranslBook(slug));
        }

        mRadioGroup.setOnCheckedChangedListener((button, checkedId) -> {
            if (mVerseEditor == null || button == null) {
                return;
            }

            TranslationBook book = transl.getParsedTranslBook(((String) button.getTag()));
            mVerseEditor.getListener().onTranslChange(book);
        });

        parent.addView(mRadioGroup, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        selectFirst();
    }*/

    private void makeRadio(PeaceRadioGroup radioGroup, TranslationBook book) {
        PeaceRadioButton radio = new PeaceRadioButton(radioGroup.getContext());
        ViewUtils.setPaddingVertical(radio, Dimen.dp2px(radio.getContext(), 10));
        ViewUtils.setPaddingHorizontal(radio, Dimen.dp2px(radio.getContext(), 15));

        radio.setTag(book.getSlug());
        radio.setBackgroundResource(R.drawable.dr_bg_action);
        radio.setTexts(book.getDisplayName(true), null);
        radio.setTextAppearance(R.style.TextAppearanceCommonTitle);
        radioGroup.addView(radio);
    }

    public void selectFirst() {
        if (mRadioGroup != null) {
            mRadioGroup.checkAtPosition(0);
        }
    }
}
