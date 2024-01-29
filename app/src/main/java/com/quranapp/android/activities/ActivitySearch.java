package com.quranapp.android.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentTransaction;
import static com.quranapp.android.utils.univ.RegexPattern.CHAPTER_OR_JUZ_PATTERN;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_JUMP_PATTERN;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_RANGE_JUMP_PATTERN;
import static com.quranapp.android.widgets.compound.PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_LEFT;
import static android.view.View.FOCUS_DOWN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.search.ChapterJumpModel;
import com.quranapp.android.components.search.JuzJumpModel;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.components.search.TafsirJumpModel;
import com.quranapp.android.components.search.VerseJumpModel;
import com.quranapp.android.databinding.ActivitySearchBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.db.search.SearchHistoryDBHelper;
import com.quranapp.android.frags.search.FragSearchResult;
import com.quranapp.android.frags.search.FragSearchSuggestions;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.extensions.ViewPaddingKt;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;
import com.quranapp.android.utils.search.SearchFilters;
import com.quranapp.android.utils.search.SearchLocalHistoryManager;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;
import com.quranapp.android.widgets.radio.PeaceRadioButton;
import com.quranapp.android.widgets.radio.PeaceRadioGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;

public class ActivitySearch extends BaseActivity {
    private final ActivityResultLauncher<Intent> mActivityResultLauncher = activityResultHandler();
    public ActivitySearchBinding mBinding;
    public QuranTranslationFactory mTranslFactory;
    public BookmarkDBHelper mBookmarkDBHelper;
    public SearchHistoryDBHelper mHistoryDBHelper;
    private FragSearchResult mFragSearchResult;
    private FragSearchSuggestions mFragSearchSugg;
    public QuranMeta mQuranMeta;
    public SearchFilters mSearchFilters;
    public SearchLocalHistoryManager mLocalHistoryManager;
    private final Handler mSuggHandler = new Handler(Looper.getMainLooper());
    public Map<String, QuranTranslBookInfo> availableTranslModels;
    public boolean mSupportsVoiceInput;

    @Override
    protected void onDestroy() {
        if (mFragSearchResult != null) {
            mFragSearchResult.destroy();
        }
        if (mFragSearchSugg != null) {
            mFragSearchSugg.destroy();
        }
        if (mBookmarkDBHelper != null) {
            mBookmarkDBHelper.close();
        }
        if (mHistoryDBHelper != null) {
            mHistoryDBHelper.close();
        }

        if (mTranslFactory != null) {
            mTranslFactory.close();
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareAvailableTrans();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_search;
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        mTranslFactory = new QuranTranslationFactory(this);
        mBookmarkDBHelper = new BookmarkDBHelper(this);
        mHistoryDBHelper = new SearchHistoryDBHelper(this);
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivitySearchBinding.bind(activityView);

        QuranMeta.prepareInstance(this, quranMeta -> {
            mQuranMeta = quranMeta;

            prepareAvailableTrans();
            init();
        });
    }

    private void prepareAvailableTrans() {
        availableTranslModels = mTranslFactory.getAvailableTranslationBooksInfo();
    }

    private void init() {
        initManagers(this);
        initHeader();
        initFrags();
        initVoiceInput();
    }

    private void initHeader() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
        mBinding.voiceSearch.setOnClickListener(v -> startVoiceRecognitionActivity());
        mBinding.filter.setOnClickListener(v -> mSearchFilters.show());

        mBinding.search.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showSugg();
            }
        });
        mBinding.search.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mBinding.search.hasFocus()) {
                    return;
                }

                mBinding.voiceSearch.setVisibility((mSupportsVoiceInput && s.length() == 0) ? VISIBLE : GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mBinding.search.hasFocus()) {
                    return;
                }

                if (!mFragSearchSugg.isVisible()) {
                    showSugg();
                }

                mSuggHandler.removeCallbacksAndMessages(null);
                mSuggHandler.postDelayed(() -> initSugg(s.toString()), 200);
            }
        });
        mBinding.search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                initSearch(v.getText().toString(), false, false);
            }
            return true;
        });

        Drawable chevronRight = drawable(com.peacedesign.R.drawable.dr_icon_chevron_right);
        chevronRight = DrawableUtils.rotate(this, chevronRight, 90F);
        chevronRight.setTintList(colorStateList(R.color.colorIcon));
        mBinding.btnSelectTransl.setDrawables(null, null, chevronRight, null);

        mBinding.btnSelectTransl.setOnClickListener(v -> showTranslationSheet());
        mBinding.btnQuickLinks.setOnCheckChangedListener((button, isChecked) -> {
            mSearchFilters.showQuickLinks = isChecked;
            reSearch();

            return Unit.INSTANCE;
        });

        mBinding.filter.setVisibility(GONE);
        mBinding.btnSelectTransl.setVisibility(GONE);
        mBinding.btnQuickLinks.setVisibility(GONE);
    }

    private void setupActionButtons(boolean isSuggFrag) {
        if (isSuggFrag) {
            mBinding.filter.setVisibility(GONE);
            boolean isQueryEmpty = mBinding.search.getText() == null || mBinding.search.getText().length() == 0;
            mBinding.voiceSearch.setVisibility((mSupportsVoiceInput && isQueryEmpty) ? VISIBLE : GONE);
        } else {
            if (!mFragSearchResult.mIsLoadingInProgress) {
                mBinding.filter.setVisibility(VISIBLE);
                mBinding.voiceSearch.setVisibility(mSupportsVoiceInput ? VISIBLE : GONE);
            }
        }
    }

    private void initFrags() {
        mFragSearchResult = new FragSearchResult();
        mFragSearchSugg = FragSearchSuggestions.newInstance();

        getSupportFragmentManager().addFragmentOnAttachListener((fragmentManager, fragment) -> {
            boolean isSuggFrag = fragment instanceof FragSearchSuggestions;

            setupActionButtons(isSuggFrag);
            setupHeader(isSuggFrag);
            mBinding.search.setLongClickable(isSuggFrag);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if (isSuggFrag) {
                mBinding.search.requestFocus();
            } else {
                mBinding.search.clearFocus();
                imm.hideSoftInputFromWindow(mBinding.search.getWindowToken(), 0);
            }
        });

        addInitialFrag();
    }

    private void addInitialFrag() {
        mBinding.search.postDelayed(() -> mBinding.search.requestFocus(FOCUS_DOWN, null), 50);
    }

    private void initVoiceInput() {
        // Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
            0);
        mSupportsVoiceInput = activities.size() != 0;
        if (!mSupportsVoiceInput) {
            mBinding.voiceSearch.setEnabled(false);
            mBinding.voiceSearch.setVisibility(GONE);
        }
    }

    private void initManagers(ActivitySearch activitySearch) {
        String initiallySelectedSlug = null;
        for (QuranTranslBookInfo bookInfo : availableTranslModels.values()) {
            if ("en".equals(bookInfo.getLangCode())) {
                initiallySelectedSlug = bookInfo.getSlug();
                break;
            }
        }

        mSearchFilters = new SearchFilters(activitySearch, initiallySelectedSlug);
        mLocalHistoryManager = new SearchLocalHistoryManager();
    }

    public void reSearch() {
        initSearch(mLocalHistoryManager.getLastQuery(), true, true);
    }

    public void initSearch(String query, boolean reSearch, boolean protectFromLocalHistory) {
        String finalQuery = query.toLowerCase();

        if (TextUtils.isEmpty(finalQuery)) {
            return;
        }

        boolean isSameQuery = mLocalHistoryManager.isCurrentQuery(finalQuery);
        if (!protectFromLocalHistory) {
            mLocalHistoryManager.onTravelForward(finalQuery);
        } else {
            mLocalHistoryManager.setLastQuery(finalQuery);
        }

        hideSugg(() -> {
            if (reSearch || !isSameQuery) {
                mHistoryDBHelper.addToHistory(finalQuery, () -> Log.d("ADDED/UPDATED TO HISTORY"));
                mFragSearchResult.initSearch(this);
            }
        });
    }

    private void initSugg(String query) {
        try {
            mFragSearchSugg.initSuggestion(this, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyFilters() {
        reSearch();
    }

    private void showSugg() {
        if (mFragSearchSugg.isVisible()) {
            return;
        }

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.frameLayout, mFragSearchSugg);
        t.runOnCommit(() -> initSugg(mLocalHistoryManager.getLastQuery()));
        t.commitAllowingStateLoss();
    }

    private void hideSugg(Runnable runnable) {
        if (mFragSearchResult.isVisible()) {
            mBinding.search.clearFocus();
            mBinding.search.setText(mLocalHistoryManager.getLastQuery());
            mBinding.header.setExpanded(true);

            if (runnable != null) {
                runnable.run();
            }
            return;
        }

        mBinding.search.clearFocus();
        mBinding.search.setText(mLocalHistoryManager.getLastQuery());
        mBinding.header.setExpanded(true);

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.frameLayout, mFragSearchResult);
        t.runOnCommit(() -> {
            if (runnable != null) {
                runnable.run();
            }
            setupHeader(false);
        });
        t.commitAllowingStateLoss();
    }

    private void showTranslationSheet() {
        PeaceBottomSheet sheet = new PeaceBottomSheet();
        NestedScrollView scrollView = new NestedScrollView(this);
        PeaceRadioGroup radioGroup = new PeaceRadioGroup(this);
        ViewPaddingKt.updatePaddingVertical(radioGroup, dp2px(15F), dp2px(25F));
        int padH = dp2px(25F);
        int padV = dp2px(13F);
        int spaceBtwn = dp2px(15F);

        for (QuranTranslBookInfo bookInfo : availableTranslModels.values()) {
            PeaceRadioButton radio = new PeaceRadioButton(this);
            ViewPaddingKt.updatePaddings(radio, padH, padV);
            radio.setBackgroundResource(com.peacedesign.R.drawable.dr_bg_action);
            radio.setText(bookInfo.getBookName());
            radio.setTag(bookInfo);
            radio.setTextAppearance(R.style.TextAppearanceCommonTitle);
            radio.setChecked(bookInfo.getSlug().equals(mSearchFilters.selectedTranslSlug));
            radio.setForceTextGravity(COMPOUND_TEXT_GRAVITY_LEFT);
            radio.setSpaceBetween(spaceBtwn);
            radioGroup.addView(radio);
        }
        radioGroup.setOnCheckChangedListener((btn, id) -> {
            sheet.dismiss();
            QuranTranslBookInfo bookInfo = (QuranTranslBookInfo) btn.getTag();
            mSearchFilters.selectedTranslSlug = bookInfo.getSlug();
            mBinding.btnSelectTransl.setText(bookInfo.getBookName());
            reSearch();

            return Unit.INSTANCE;
        });
        scrollView.addView(radioGroup);

        PeaceBottomSheetParams params = sheet.getParams();
        params.setHeaderTitleResource(R.string.strLabelSelectTranslation);
        params.setContentView(scrollView);
        sheet.show(getSupportFragmentManager());
    }

    private void setupHeader(boolean isFragSugg) {
        mBinding.header.setElevation(isFragSugg ? 0 : dp2px(4));
        mBinding.btnSelectTransl.setVisibility(isFragSugg ? GONE : VISIBLE);
        mBinding.btnQuickLinks.setVisibility(isFragSugg ? GONE : VISIBLE);
    }

    /**
     * Fire an intent to start the voice recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        if (!mSupportsVoiceInput) {
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search in Quran");
            mActivityResultLauncher.launch(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    /**
     * Handle the results from the voice recognition activity.
     */
    private ActivityResultLauncher<Intent> activityResultHandler() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultIntent -> {
            Intent data = resultIntent.getData();
            if (data == null) {
                return;
            }
            int resultCode = resultIntent.getResultCode();
            if (resultCode == RESULT_OK) {
                // Populate the wordsList with the String values the recognition engine thought it heard
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() < 1) {
                    return;
                }
                String query = matches.get(0);
                initSearch(query, false, false);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFragSearchSugg.isVisible()) {
            if (!TextUtils.isEmpty(mLocalHistoryManager.getLastQuery())) {
                hideSugg(null);
            } else {
                super.onBackPressed();
            }
        } else {
            if (mLocalHistoryManager.hasHistories()) {
                initSearch(mLocalHistoryManager.onTravelBackward(), false, true);
            } else {
                super.onBackPressed();
            }
        }
    }

    public void pushQuery(CharSequence query) {
        mBinding.search.setText(query);
        mBinding.search.setSelection(query.length());
    }

    public ArrayList<SearchResultModelBase> prepareJumper(QuranMeta quranMeta, String query) throws NumberFormatException {
        query = StringUtils.escapeRegex(query);

        ArrayList<SearchResultModelBase> jumperSuggCollection = new ArrayList<>();

        Matcher mtchrVRangeJump = VERSE_RANGE_JUMP_PATTERN.matcher(query);
        Matcher mtchrVJump = VERSE_JUMP_PATTERN.matcher(query);
        Matcher mtchrChapOrJuzNo = CHAPTER_OR_JUZ_PATTERN.matcher(query);

        Pattern patternQuery = Pattern.compile(query, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        if (mtchrVRangeJump.find()) {
            MatchResult r = mtchrVRangeJump.toMatchResult();
            if (r.groupCount() >= 3) {
                int chapNo = Integer.parseInt(r.group(1));

                if (!QuranMeta.isChapterValid(chapNo)) {
                    return jumperSuggCollection;
                }

                int fromVerse = Integer.parseInt(r.group(2));
                int toVerse = Integer.parseInt(r.group(3));

                // swap
                int tmpFrom = fromVerse;
                fromVerse = Math.min(fromVerse, toVerse);
                toVerse = Math.max(tmpFrom, toVerse);

                if (quranMeta.isVerseRangeValid4Chapter(chapNo, fromVerse, toVerse)) {
                    makeVerseSuggestion(quranMeta, jumperSuggCollection, chapNo, fromVerse, toVerse);
                }

                if (fromVerse == 0) {
                    fromVerse = 1;
                }

                boolean isFromVerseValid = quranMeta.isVerseValid4Chapter(chapNo, fromVerse);
                boolean isToVerseValid = quranMeta.isVerseValid4Chapter(chapNo, toVerse);

                if (isFromVerseValid) {
                    makeVerseSuggestion(quranMeta, jumperSuggCollection, chapNo, fromVerse, fromVerse);
                }

                if (isToVerseValid) {
                    makeVerseSuggestion(quranMeta, jumperSuggCollection, chapNo, toVerse, toVerse);
                }

                if (isFromVerseValid) {
                    makeTafsirSuggestion(quranMeta, jumperSuggCollection, chapNo, fromVerse);
                }

                if (isToVerseValid) {
                    makeTafsirSuggestion(quranMeta, jumperSuggCollection, chapNo, toVerse);
                }

                makeChapterSuggestion(quranMeta, jumperSuggCollection, chapNo);
            }
        } else if (mtchrVJump.find()) {
            MatchResult r = mtchrVJump.toMatchResult();
            if (r.groupCount() >= 2) {
                int chapNo = Integer.parseInt(r.group(1));
                int verseNo = Integer.parseInt(r.group(2));

                if (!QuranMeta.isChapterValid(chapNo)) {
                    return jumperSuggCollection;
                }

                if (quranMeta.isVerseValid4Chapter(chapNo, verseNo)) {
                    makeVerseSuggestion(quranMeta, jumperSuggCollection, chapNo, verseNo, verseNo);
                    makeTafsirSuggestion(quranMeta, jumperSuggCollection, chapNo, verseNo);
                }

                makeChapterSuggestion(quranMeta, jumperSuggCollection, chapNo);
            }
        } else if (mtchrChapOrJuzNo.find()) {
            MatchResult r = mtchrChapOrJuzNo.toMatchResult();
            if (r.groupCount() >= 1) {
                int chapOrJuzNo = Integer.parseInt(r.group(1));

                if (QuranMeta.isChapterValid(chapOrJuzNo)) {
                    makeChapterSuggestion(quranMeta, jumperSuggCollection, chapOrJuzNo);
                }

                if (QuranMeta.isJuzValid(chapOrJuzNo)) {
                    makeJuzSuggestion(quranMeta, jumperSuggCollection, chapOrJuzNo);
                }
            }
        }

        QuranUtils.iterateChapterNo(chapterNo -> {
            QuranMeta.ChapterMeta chapterMeta = quranMeta.getChapterMeta(chapterNo);
            if (chapterMeta != null) {
                String destination = chapterMeta.tags;
                Matcher mtchrQuery = patternQuery.matcher(destination);
                if (mtchrQuery.find()) {
                    makeChapterSuggestion(quranMeta, jumperSuggCollection, chapterNo);
                }
            }
        });

        return jumperSuggCollection;
    }

    private void makeJuzSuggestion(QuranMeta quranMeta, ArrayList<SearchResultModelBase> collection, int juzNo) {
        JuzJumpModel juzJumpModel = new JuzJumpModel(juzNo, "Juz " + juzNo,
                quranMeta.getJuzNameTransliterated(juzNo), quranMeta.getJuzNameArabic(juzNo));

        collection.add(juzJumpModel);
    }

    private void makeChapterSuggestion(QuranMeta quranMeta, ArrayList<SearchResultModelBase> collection, int chapNo) {
        collection.add(new ChapterJumpModel(chapNo, String.valueOf(chapNo), quranMeta.getChapterName(this, chapNo),
            quranMeta.getChapterNameTranslation(chapNo)));
    }

    private void makeVerseSuggestion(QuranMeta quranMeta, ArrayList<SearchResultModelBase> collection, int chapNo, int fromVerse, int toVerse) {
        String formatted = getString(R.string.strTitleReadVerseRange, fromVerse, toVerse);
        if (fromVerse == toVerse) formatted = getString(R.string.strTitleGotoVerseNo, fromVerse);

        VerseJumpModel verseJumpModel = new VerseJumpModel(chapNo, fromVerse, toVerse, formatted,
                quranMeta.getChapterName(this, chapNo, true));

        collection.add(verseJumpModel);
    }

    private void makeTafsirSuggestion(QuranMeta quranMeta, ArrayList<SearchResultModelBase> collection, int chapNo, int verseNo) {
        TafsirJumpModel tafsirJumpModel = new TafsirJumpModel(chapNo, verseNo, getString(R.string.strTitleReadTafsirOfVerse, verseNo),
                quranMeta.getChapterName(this, chapNo, true));

        collection.add(tafsirJumpModel);
    }

    public static class SearchResultViewType {
        public static final int VERSE_JUMPER = 0x0;
        public static final int CHAPTER_JUMPER = 0x1;
        public static final int JUZ_JUMPER = 0x2;
        public static final int TAFSIR_JUMPER = 0x3;
        public static final int RESULT_COUNT = 0x4;
        public static final int RESULT = 0x5;
    }
}