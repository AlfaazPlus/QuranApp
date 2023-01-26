package com.quranapp.android.frags.search;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivitySearch;
import com.quranapp.android.adapters.search.ADPSearchSugg;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.databinding.FragSearchSuggestionsBinding;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.peacedesign.android.utils.span.RoundedBG_FGSpan;

import java.util.ArrayList;

public class FragSearchSuggestions extends BaseFragment implements Destroyable {
    private boolean firstTime = true;
    private FragSearchSuggestionsBinding mBinding;
    private int spannableTextColor;
    private int spannableBGColor;
    private ADPSearchSugg mSearchSuggAdapter;

    public FragSearchSuggestions() {
    }

    public static FragSearchSuggestions newInstance() {
        return new FragSearchSuggestions();
    }

    @Override
    public void onDestroy() {
        if (mSearchSuggAdapter != null) {
            mSearchSuggAdapter.mSuggModels.clear();
        }
        super.onDestroy();
    }

    @Override
    public void destroy() {
        if (mSearchSuggAdapter != null) {
            mSearchSuggAdapter.mSuggModels.clear();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (firstTime || mBinding == null) {
            mBinding = FragSearchSuggestionsBinding.inflate(inflater, container, false);
        }
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!firstTime) {
            return;
        }

        firstTime = false;

        spannableTextColor = ContextCompat.getColor(mBinding.getRoot().getContext(), R.color.colorPrimary);
        spannableBGColor = ContextCompat.getColor(mBinding.getRoot().getContext(), R.color.colorBGLightGrey);

        init(view.getContext());
    }

    private void init(Context context) {
        initSuggRecycler(context);
        //        setupContent(mBinding);
    }

    private void initSuggRecycler(Context context) {
        mSearchSuggAdapter = new ADPSearchSugg(context);
        mBinding.suggs.setLayoutManager(new LinearLayoutManager(context));
        mBinding.suggs.setAdapter(mSearchSuggAdapter);
        mBinding.suggs.setItemAnimator(null);
    }

    /*private void setupContent(FragSearchSuggestionsBinding binding) {
        String eg = "\n\u2799\te.g.,\t";

        String specVText = "1. Search for specific verse: \n";
        String specVSyntax = "<b>&lt;Surah No&gt; : &lt;Verse No&gt;</b>";
        String specVEx = "<b>2:255<b> ";
        String specVExMsg = String.format("<i>(Surah %s, Verse 255)</i>", QuranUtils.getChapterNameTransliterated(2));

        String rangeVText = "2. Search for verse range: \n";
        String rangeVSyntax = "<b>&lt;Surah No&gt; : &lt;From Verse No&gt; − &lt;To Verse No&gt;</b>";
        String rangeVEx = "<b>88:16-19<b> ";
        String rangeVExMsg = String.format("<i>(Surah %s, from verse 16 to 19)</i>",
                QuranUtils.getChapterNameTransliterated(88));


        SpannableString specVSpannable = setBGAndFCSpan(specVSyntax, true);
        SpannableString specVExSpannable = setBGAndFCSpan(specVEx, false);
        SpannableString rangeVSpannable = setBGAndFCSpan(rangeVSyntax, true);
        SpannableString rangeVExSpannable = setBGAndFCSpan(rangeVEx, false);

        CharSequence tip1 = TextUtils.concat(specVText, specVSpannable, eg, specVExSpannable,
                HtmlCompat.fromHtml(specVExMsg, HtmlCompat.FROM_HTML_MODE_LEGACY), "\n\n");
        CharSequence tip2 = TextUtils.concat(rangeVText, rangeVSpannable, eg, rangeVExSpannable,
                HtmlCompat.fromHtml(rangeVExMsg, HtmlCompat.FROM_HTML_MODE_LEGACY), "\n\n");

        //        binding.tipText.setText(TextUtils.concat(tip1, tip2));
    }*/

    private SpannableString setBGAndFCSpan(String string, boolean setBG) {
        Spanned spanned = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY);
        SpannableString spannable = new SpannableString(spanned);
        if (setBG) {
            spannable.setSpan(new RoundedBG_FGSpan(spannableBGColor, spannableTextColor), 0, spanned.length(),
                    SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannable.setSpan(new ForegroundColorSpan(spannableTextColor), 0, spanned.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public void initSuggestion(ActivitySearch activitySearch, String query) throws NumberFormatException {
        boolean isEmpty = TextUtils.isEmpty(query);
        prepareNShowSugg(activitySearch, query, isEmpty);
    }

    private void prepareNShowSugg(ActivitySearch activitySearch, String query, boolean isEmpty) {
        ArrayList<SearchResultModelBase> suggModels = new ArrayList<>();

        if (!isEmpty) {
            ArrayList<SearchResultModelBase> jumperSuggs = activitySearch.prepareJumper(activitySearch.mQuranMeta, query);
            suggModels.addAll(jumperSuggs);
        }

        ArrayList<SearchResultModelBase> historySuggs = prepareHistories(activitySearch, isEmpty ? null : query);
        suggModels.addAll(historySuggs);

        mSearchSuggAdapter.setSuggModels(activitySearch, suggModels);
        mBinding.suggs.setAdapter(mSearchSuggAdapter);
    }

    private ArrayList<SearchResultModelBase> prepareHistories(ActivitySearch activitySearch, String query) {
        return activitySearch.mHistoryDBHelper.getHistories(query);
    }
}
