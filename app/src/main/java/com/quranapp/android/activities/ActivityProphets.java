/*
 * Created by Faisal Khan on (c) 13/8/2021.
 */

package com.quranapp.android.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.LITERAL;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.ADPProphets;
import com.quranapp.android.adapters.utility.TopicFilterSpinnerAdapter;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.QuranProphet;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.databinding.ActivityTopicsBinding;
import com.quranapp.android.databinding.LytTopicsActivityHeaderBinding;
import com.quranapp.android.utils.extended.GapedItemDecoration;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;
import com.quranapp.android.views.helper.Spinner2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;

public class ActivityProphets extends BaseActivity {
    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private ActivityTopicsBinding mBinding;
    private QuranProphet mQuranProphet;
    private ADPProphets mProphetsAdapter;
    private final TextWatcher searchTextWatcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mBinding.header.searchContainer.btnClear.setVisibility(s.length() == 0 ? GONE : VISIBLE);
            mSearchHandler.removeCallbacksAndMessages(null);
            mSearchHandler.postDelayed(() -> searchProphets(s.toString()), 150);
        }
    };

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGHomePageItem);
    }

    @Override
    public void onBackPressed() {
        if (mBinding == null) {
            super.onBackPressed();
            return;
        }

        if (mBinding.header.searchContainer.getRoot().getVisibility() == VISIBLE) {
            toggleSearchBox(mBinding.header, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_topics;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityTopicsBinding.bind(activityView);

        QuranMeta.prepareInstance(this, quranMeta -> QuranProphet.Companion.prepareInstance(this, quranMeta, quranProphet -> {
            mQuranProphet = quranProphet;
            initContent(this);

            return Unit.INSTANCE;
        }));
    }


    private void initContent(Context context) {
        initTopics(context);
        initHeader(mBinding.header);
    }

    private void initHeader(LytTopicsActivityHeaderBinding header) {
        header.searchContainer.getRoot().setBackgroundColor(getStatusBarBG());

        header.topicTitle.setText(getString(R.string.strTitleProphets));
        initProphetFilters(header.filter);
        header.search.setOnClickListener(v -> toggleSearchBox(header, true));
        header.back.setOnClickListener(v -> onBackPressed());

        EditText searchBox = header.searchContainer.searchBox;
        searchBox.setPaddingRelative(
            dp2px(5),
            searchBox.getPaddingTop(),
            searchBox.getPaddingEnd(),
            searchBox.getPaddingBottom()
        );
        header.searchContainer.btnClear.setOnClickListener(v -> header.searchContainer.searchBox.setText(null));
        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = ContextCompat.getSystemService(this, InputMethodManager.class);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });
        searchBox.addTextChangedListener(searchTextWatcher);
    }

    private void initProphetFilters(Spinner2 spinner) {
        final List<SpinnerItem> filters = new ArrayList<>();
        String[] items = strArray(R.array.arrProphetFilterItems);
        for (String item : items) {
            filters.add(new SpinnerItem(item));
        }

        int itemLayoutRes = R.layout.lyt_topic_filter_spinner_item;
        TopicFilterSpinnerAdapter adapter = new TopicFilterSpinnerAdapter(spinner.getContext(), itemLayoutRes,
            R.id.text, filters);
        spinner.setAdapterWithDynamicWidth(adapter);

        // running it twice before and after setting listener to prevent it to be invoked for the first time.
        spinner.setSelection(0);

        spinner.setOnItemSelectedListener(new Spinner2.SimplerSpinnerItemSelectListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    resetAdapter(mQuranProphet.getProphets());
                } else if (position == 1) {
                    List<QuranProphet.Prophet> sortedByOrder = new ArrayList<>(mQuranProphet.getProphets());
                    Collections.sort(sortedByOrder, Comparator.comparingInt(QuranProphet.Prophet::getOrder));
                    resetAdapter(sortedByOrder);
                    Toast.makeText(ActivityProphets.this, R.string.strMsgProphetsOrder, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // running it twice before and after setting listener to prevent it to be invoked for the first time.
        spinner.setSelection(0);
    }

    private void searchProphets(String query) {
        if (TextUtils.isEmpty(query)) {
            resetAdapter(mQuranProphet.getProphets());
            return;
        }

        Pattern pattern = Pattern.compile(query, CASE_INSENSITIVE | LITERAL | DOTALL);
        List<QuranProphet.Prophet> queryProphets = new ArrayList<>();
        for (QuranProphet.Prophet prophet : mQuranProphet.getProphets()) {
            Matcher matcher = pattern.matcher(prophet.getName() + prophet.getNameEn() + prophet.getNameAr());
            if (matcher.find()) {
                queryProphets.add(prophet);
            }
        }

        resetAdapter(queryProphets);
    }

    private void toggleSearchBox(LytTopicsActivityHeaderBinding header, boolean showSearch) {
        header.searchContainer.getRoot().setVisibility(showSearch ? VISIBLE : GONE);

        EditText searchBox = header.searchContainer.searchBox;
        searchBox.setHint(R.string.strHintSearchProphet);

        if (showSearch) {
            searchBox.requestFocus();
            InputMethodManager imm = (InputMethodManager) searchBox.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        } else {
            searchBox.clearFocus();
        }
    }

    private void initTopics(Context context) {
        mProphetsAdapter = new ADPProphets(this, MATCH_PARENT, -1);

        mBinding.topics.addItemDecoration(new GapedItemDecoration(dp2px(2.5f)));
        mBinding.topics.setLayoutManager(new LinearLayoutManager(context));
        mBinding.topics.setAdapter(mProphetsAdapter);
    }

    private void resetProphets() {
        EditText searchBox = mBinding.header.searchContainer.searchBox;
        searchBox.removeTextChangedListener(searchTextWatcher);
        searchBox.setText(null);
        searchBox.addTextChangedListener(searchTextWatcher);
        searchBox.setHint(R.string.strHintSearchProphet);

        resetAdapter(mQuranProphet.getProphets());
    }

    private void resetAdapter(List<QuranProphet.Prophet> topics) {
        if (topics == null) {
            return;
        }

        int tmpCount = mProphetsAdapter.getItemCount();
        mProphetsAdapter.setProphets(new LinkedList<>());
        mProphetsAdapter.notifyItemRangeRemoved(0, tmpCount);

        mProphetsAdapter.setProphets(topics);
        mProphetsAdapter.notifyItemRangeInserted(0, topics.size());
    }

    public void showFresh() {
        if (mBinding != null) {
            toggleSearchBox(mBinding.header, false);
        }
    }
}
