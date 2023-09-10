package com.quranapp.android.frags.search;

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
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.span.RoundedBG_FGSpan;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivitySearch;
import com.quranapp.android.adapters.search.ADPSearchSugg;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.databinding.FragSearchSuggestionsBinding;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.univ.MessageUtils;

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
    }

    private void initSuggRecycler(Context context) {
        mSearchSuggAdapter = new ADPSearchSugg(context);
        mBinding.suggs.setLayoutManager(new LinearLayoutManager(context));
        mBinding.suggs.setAdapter(mSearchSuggAdapter);
        mBinding.suggs.setItemAnimator(null);
    }

    public void initSuggestion(ActivitySearch activitySearch, String query) throws NumberFormatException {
        mBinding.btnClear.setOnClickListener(v -> MessageUtils.showConfirmationDialog(
            activitySearch,
            R.string.msgClearSearchHistory,
            null,
            R.string.strLabelRemoveAll,
            ColorUtils.DANGER,
            () -> {
                activitySearch.mHistoryDBHelper.clearHistories();
                initSuggestion(activitySearch, query);
            }
        ));

        boolean isEmpty = TextUtils.isEmpty(query);

        mBinding.btnClear.setVisibility(
            isEmpty && activitySearch.mHistoryDBHelper.getHistoriesCount() > 0
                ? View.VISIBLE
                : View.GONE
        );

        prepareNShowSugg(activitySearch, query, isEmpty);
    }

    private void prepareNShowSugg(ActivitySearch activitySearch, String query, boolean isEmpty) {
        ArrayList<SearchResultModelBase> suggModels = new ArrayList<>();

        if (!isEmpty) {
            suggModels.addAll(activitySearch.prepareJumper(
                activitySearch.mQuranMeta,
                query
            ));
        }

        suggModels.addAll(prepareHistories(activitySearch, isEmpty ? null : query));

        mSearchSuggAdapter.setSuggModels(activitySearch, suggModels);
        mBinding.suggs.setAdapter(mSearchSuggAdapter);
    }

    private ArrayList<SearchResultModelBase> prepareHistories(ActivitySearch activitySearch, String query) {
        return activitySearch.mHistoryDBHelper.getHistories(query);
    }
}
