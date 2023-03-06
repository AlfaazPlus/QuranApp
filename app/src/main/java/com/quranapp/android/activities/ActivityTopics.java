/*
 * Created by Faisal Khan on (c) 13/8/2021.
 */

package com.quranapp.android.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.LITERAL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.peacedesign.android.utils.ArrayUtils;
import com.peacedesign.android.utils.anim.DimensionAnimator;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.ADPTopics;
import com.quranapp.android.adapters.utility.TopicFilterSpinnerAdapter;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.QuranTopic;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.databinding.ActivityTopicsBinding;
import com.quranapp.android.databinding.LytChipgroupBinding;
import com.quranapp.android.databinding.LytTopicsActivityHeaderBinding;
import com.quranapp.android.utils.extended.GapedItemDecoration;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;
import com.quranapp.android.utils.thread.runner.RunnableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseRunnableTask;
import com.quranapp.android.views.helper.Spinner2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityTopics extends BaseActivity {
    private final RunnableTaskRunner mTaskRunner = new RunnableTaskRunner();
    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private ActivityTopicsBinding mBinding;
    private QuranTopic mQuranTopic;
    private ADPTopics mTopicsAdapter;
    private Character[] availableAlphabets;
    private boolean mFilterMostMentions;
    private char mLastSelectedChar;
    private List<QuranTopic.Topic> mLastTopics;
    private final TextWatcher searchTextWatcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mBinding.header.searchContainer.btnClear.setVisibility(s.length() == 0 ? GONE : VISIBLE);
            mSearchHandler.removeCallbacksAndMessages(null);
            mSearchHandler.postDelayed(() -> searchTopics(s.toString()), 150);
        }
    };
    private LytChipgroupBinding mAlphabetsGroupBinding;

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGHomePageItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scrollToCurrentChip();
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("filterMostMentions", mFilterMostMentions);
        outState.putChar("lastSelectedChar", mLastSelectedChar);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mFilterMostMentions = savedInstanceState.getBoolean("filterMostMentions", false);
            mLastSelectedChar = savedInstanceState.getChar("lastSelectedChar");
        }
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityTopicsBinding.bind(activityView);

        QuranMeta.prepareInstance(this, quranMeta -> QuranTopic.prepareInstance(this, quranMeta, quranTopic -> {
            mQuranTopic = quranTopic;
            availableAlphabets = quranTopic.getAvailableAlphabets();
            initContent(this);
        }));
    }


    private void initContent(Context context) {
        if (!ArrayUtils.contains(availableAlphabets, mLastSelectedChar)) {
            mLastSelectedChar = availableAlphabets[0];
        }

        initTopics(context);
        initHeader(mBinding.header);
    }

    private void initHeader(LytTopicsActivityHeaderBinding header) {
        header.searchContainer.getRoot().setBackgroundColor(getStatusBarBG());

        header.topicTitle.setText(getString(R.string.strTitleTopics));
        header.search.setOnClickListener(v -> toggleSearchBox(header, true));
        header.back.setOnClickListener(v -> onBackPressed());
        initTopicFilters(header.filter);

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

        initAlphabets();
    }

    private void initTopicFilters(Spinner2 spinner) {
        final List<SpinnerItem> filters = new ArrayList<>();
        String[] items = strArray(R.array.arrTopicFilterItems);
        for (String item : items) {
            filters.add(new SpinnerItem(item));
        }

        int itemLayoutRes = R.layout.lyt_topic_filter_spinner_item;
        TopicFilterSpinnerAdapter adapter = new TopicFilterSpinnerAdapter(spinner.getContext(), itemLayoutRes,
            R.id.text, filters);
        spinner.setAdapterWithDynamicWidth(adapter);

        spinner.setOnItemSelectedListener(new Spinner2.SimplerSpinnerItemSelectListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFilterMostMentions = position == 1;
                setupAlphabetsVisibility(mFilterMostMentions);

                if (position == 0) {
                    mAlphabetsGroupBinding.chipGroup.check(mLastSelectedChar);
                } else if (position == 1) {
                    mAlphabetsGroupBinding.chipGroup.clearCheck();

                    List<QuranTopic.Topic> topics = new ArrayList<>();

                    Map<Integer, QuranTopic.Topic> topicMap = mQuranTopic.getTopics();
                    topicMap.forEach((topicId, topic) -> topics.add(topic));

                    Collections.sort(topics, (o1, o2) -> o2.verses.size() - o1.verses.size());

                    resetAdapter(topics, false);
                }
            }
        });

        spinner.post(() -> {
            // Check after setting listener so that adapter could be invoked by the listener for the first time.
            spinner.setSelection(mFilterMostMentions ? 1 : 0);
        });
    }

    private void searchTopics(String query) {
        if (TextUtils.isEmpty(query)) {
            resetAdapter(mLastTopics, true);
            return;
        }

        Pattern pattern = Pattern.compile(query, CASE_INSENSITIVE | LITERAL | DOTALL);
        List<QuranTopic.Topic> queryTopics = new ArrayList<>();
        for (QuranTopic.Topic topic : mLastTopics) {
            if (topic == null) {
                continue;
            }

            Matcher matcher = pattern.matcher(topic.name + topic.otherTerms);
            if (matcher.find()) {
                queryTopics.add(topic);
            }
        }

        resetAdapter(queryTopics, true);
    }

    private void toggleSearchBox(LytTopicsActivityHeaderBinding header, boolean showSearch) {
        header.searchContainer.getRoot().setVisibility(showSearch ? VISIBLE : GONE);

        ChipGroup alphabetsGroup = mAlphabetsGroupBinding.chipGroup;
        EditText searchBox = header.searchContainer.searchBox;

        Chip chip = alphabetsGroup.findViewById(alphabetsGroup.getCheckedChipId());
        if (mFilterMostMentions || chip == null) {
            searchBox.setHint(R.string.strHintSearchTopic);
        } else {
            searchBox.setHint(str(R.string.strHintSearchTopicAlphabet, chip.getText().toString()));
        }

        if (showSearch) {
            searchBox.requestFocus();
            InputMethodManager imm = (InputMethodManager) searchBox.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        } else {
            searchBox.clearFocus();
            if (!TextUtils.isEmpty(searchBox.getText())) {
                searchBox.setText(null);
            }
        }
    }

    private void setupAlphabetsVisibility(boolean hide) {
        View alphabetsContainer = mAlphabetsGroupBinding.getRoot();
        if (hide ? alphabetsContainer.getVisibility() == GONE : alphabetsContainer.getVisibility() == VISIBLE) {
            return;
        }

        int dimen = ContextKt.getDimenPx(alphabetsContainer.getContext(), R.dimen.dmnChipGroupHeight);
        DimensionAnimator animator = DimensionAnimator.Companion.ofHeight(alphabetsContainer, hide ? dimen : 0,
            hide ? 0 : dimen);
        animator.setDuration(70);

        if (hide) {
            animator.setAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    alphabetsContainer.setVisibility(GONE);
                }
            });
        } else {
            alphabetsContainer.setVisibility(VISIBLE);
            scrollToCurrentChip();
        }

        alphabetsContainer.post(animator::start);
    }

    private void initAlphabets() {
        mAlphabetsGroupBinding = LytChipgroupBinding.inflate(LayoutInflater.from(this), mBinding.header.getRoot(),
            true);
        initAlphabetsAsync(mAlphabetsGroupBinding);
    }

    private void initAlphabetsAsync(LytChipgroupBinding chipGroupBinding) {
        ChipGroup alphabetsGroup = chipGroupBinding.chipGroup;
        alphabetsGroup.setChipSpacingHorizontal(0);

        for (char alphabet : availableAlphabets) {
            makeAlphabetChip(alphabetsGroup, alphabet);
        }

        alphabetsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (ArrayUtils.contains(availableAlphabets, (char) checkedId)) {
                mLastSelectedChar = (char) checkedId;
                resetTopics(checkedId);
            }
        });
    }

    private void makeAlphabetChip(ChipGroup parent, char alphabet) {
        Chip chip = new Chip(parent.getContext());
        chip.setId(alphabet);
        chip.setText(String.valueOf(alphabet).toUpperCase());

        ShapeAppearanceModel shapeAppearanceModel = chip.getShapeAppearanceModel();
        chip.setShapeAppearanceModel(shapeAppearanceModel.withCornerSize(dp2px(10)).toBuilder().build());

        parent.addView(chip);
    }

    private void initTopics(Context context) {
        mTopicsAdapter = new ADPTopics(this, MATCH_PARENT);

        mBinding.topics.addItemDecoration(new GapedItemDecoration(dp2px(2.5f)));

        GridLayoutManager layoutManager = new GridLayoutManager(context, 2);
        mBinding.topics.setLayoutManager(layoutManager);
    }

    private void resetTopics(int alphabet) {
        EditText searchBox = mBinding.header.searchContainer.searchBox;
        searchBox.removeTextChangedListener(searchTextWatcher);
        searchBox.setText(null);
        searchBox.addTextChangedListener(searchTextWatcher);

        ChipGroup alphabetsGroup = mAlphabetsGroupBinding.chipGroup;
        Chip chip = alphabetsGroup.findViewById(alphabetsGroup.getCheckedChipId());
        if (mFilterMostMentions || chip == null) {
            searchBox.setHint(R.string.strHintSearchTopic);
        } else {
            searchBox.setHint(str(R.string.strHintSearchTopicAlphabet, chip.getText()));
        }

        mTaskRunner.cancel();
        mTaskRunner.runAsync(new BaseRunnableTask() {
            private List<QuranTopic.Topic> topicsOfAlphabet = new ArrayList<>();

            @Override
            public void runTask() {
                topicsOfAlphabet = mQuranTopic.getTopicsOfAlphabet(alphabet);
            }

            @Override
            public void onComplete() {
                resetAdapter(topicsOfAlphabet, false);
            }
        });
    }

    private void resetAdapter(List<QuranTopic.Topic> topics, boolean search) {
        if (topics == null) {
            return;
        }

        mTopicsAdapter.setTopics(topics);
        mBinding.topics.setAdapter(mTopicsAdapter);

        if (!search) {
            mLastTopics = topics;
        }
    }

    public void showFresh() {
        if (mBinding != null) {
            mBinding.header.filter.setSelection(0);
            toggleSearchBox(mBinding.header, false);
            mAlphabetsGroupBinding.chipGroup.check(availableAlphabets[0]);

            scrollToCurrentChip();
        }
    }

    private void scrollToCurrentChip() {
        if (mAlphabetsGroupBinding != null) {
            int checkedChipId = mAlphabetsGroupBinding.chipGroup.getCheckedChipId();
            View checkedView = mAlphabetsGroupBinding.chipGroup.findViewById(checkedChipId);
            if (checkedView != null) {
                mAlphabetsGroupBinding.getRoot().smoothScrollTo(checkedView.getLeft() - checkedView.getWidth() >> 1, 0);
            }
        }
    }
}
