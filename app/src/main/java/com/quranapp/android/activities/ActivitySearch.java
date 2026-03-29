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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
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
import com.quranapp.android.api.models.translation.TranslationBookInfoModel;
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
import com.quranapp.android.utils.search.ArabicSearchManager;
import com.quranapp.android.utils.search.SearchFilters;
import com.quranapp.android.utils.search.SearchLocalHistoryManager;
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;
import com.quranapp.android.widgets.list.base.BaseListAdapter;
import com.quranapp.android.widgets.list.base.BaseListItem;
import com.quranapp.android.widgets.list.base.BaseListItemView;
import com.quranapp.android.widgets.radio.PeaceRadioButton;
import com.quranapp.android.widgets.radio.PeaceRadioGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetMenu;
import com.quranapp.android.adapters.extended.PeaceBottomSheetMenuAdapter;
import com.quranapp.android.widgets.list.singleChoice.SingleChoiceListAdapter;

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
    public Map<String, TranslationBookInfoModel> availableTranslModels;
    public boolean mSupportsVoiceInput;
    private String mVoiceLangCode = null;
    private boolean mIsSuggFrag = true;

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
        mBinding.voiceSearch.setOnClickListener(v -> startVoiceRecognitionActivity(mVoiceLangCode));
        mBinding.voiceSettings.setOnClickListener(v -> showVoiceLanguageSelector());
        mBinding.filter.setOnClickListener(v -> mSearchFilters.show());
        mBinding.clearSearch.setOnClickListener(v -> {
            mBinding.search.setText("");
            mLocalHistoryManager.setLastQuery("");
            if (!mIsSuggFrag) {
                showSugg();
            } else {
                initSugg("");
                updateActionIconsVisibility();
            }
        });

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

                if (s.length() == 0 && !mIsSuggFrag) {
                    showSugg();
                }

                updateActionIconsVisibility();
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

        TranslationBookInfoModel selectedBook = availableTranslModels.get(mSearchFilters.selectedTranslSlug);
        if (selectedBook != null) {
            mBinding.btnSelectTransl.setText(selectedBook.getBookName());
        }

        updateActionIconsVisibility();
    }

    private void updateActionIconsVisibility() {
        if (mBinding == null) return;

        String query = mBinding.search.getText() != null ? mBinding.search.getText().toString() : "";
        boolean isEmpty = query.isEmpty();
        boolean isArabic = ArabicSearchManager.INSTANCE.isArabic(query);
        
        mBinding.clearSearch.setVisibility(isEmpty ? GONE : VISIBLE);
        mBinding.voiceSearch.setVisibility(mSupportsVoiceInput ? VISIBLE : GONE);
        mBinding.voiceSettings.setVisibility((mSupportsVoiceInput && isEmpty && mIsSuggFrag) ? VISIBLE : GONE);

        int resultsVisibility = mIsSuggFrag ? GONE : VISIBLE;

        mBinding.btnSelectTransl.setVisibility(isArabic ? GONE : resultsVisibility);
        mBinding.btnQuickLinks.setVisibility(isArabic ? GONE : resultsVisibility);
        
        mBinding.filter.setVisibility(resultsVisibility);
        mBinding.header.setElevation(mIsSuggFrag ? 0 : dp2px(4));
    }

    private void setupActionButtons(boolean isSuggFrag) {
        mIsSuggFrag = isSuggFrag;
        updateActionIconsVisibility();
    }

    private void initFrags() {
        mFragSearchResult = new FragSearchResult();
        mFragSearchSugg = FragSearchSuggestions.newInstance();

        getSupportFragmentManager().addFragmentOnAttachListener((fragmentManager, fragment) -> {
            if (fragment instanceof FragSearchSuggestions) {
                setupActionButtons(true);
            } else if (fragment instanceof FragSearchResult) {
                setupActionButtons(false);
            }
            
            mBinding.search.setLongClickable(fragment instanceof FragSearchSuggestions);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if (fragment instanceof FragSearchSuggestions) {
                // Focus but don't request keyboard
            } else if (fragment instanceof FragSearchResult) {
                mBinding.search.clearFocus();
                imm.hideSoftInputFromWindow(mBinding.search.getWindowToken(), 0);
            }
        });

        addInitialFrag();
    }

    private void addInitialFrag() {
        showSugg();
    }

    private void initVoiceInput() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                0);
        mSupportsVoiceInput = activities.size() != 0;
        if (!mSupportsVoiceInput) {
            mBinding.voiceSearch.setEnabled(false);
            mBinding.voiceSearch.setVisibility(GONE);
            mBinding.voiceSettings.setVisibility(GONE);
        }
    }

    private void initManagers(ActivitySearch activitySearch) {
        String initiallySelectedSlug = null;

        Set<String> savedTranslations = SPReader.getSavedTranslations(activitySearch);
        for (String slug : savedTranslations) {
            if (availableTranslModels.containsKey(slug)) {
                initiallySelectedSlug = slug;
                break;
            }
        }

        if (initiallySelectedSlug == null) {
            for (TranslationBookInfoModel bookInfo : availableTranslModels.values()) {
                if ("en".equals(bookInfo.getLangCode())) {
                    initiallySelectedSlug = bookInfo.getSlug();
                    break;
                }
            }
        }

        if (initiallySelectedSlug == null && !availableTranslModels.isEmpty()) {
            initiallySelectedSlug = availableTranslModels.keySet().iterator().next();
        }

        mSearchFilters = new SearchFilters(activitySearch, initiallySelectedSlug);
        mLocalHistoryManager = new SearchLocalHistoryManager();
        
        mVoiceLangCode = SPAppConfigs.getVoiceSearchLang(this);
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
        mIsSuggFrag = true;
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.frameLayout, mFragSearchSugg);
        t.runOnCommit(() -> {
            initSugg(mLocalHistoryManager.getLastQuery());
            updateActionIconsVisibility();
        });
        t.commitAllowingStateLoss();
    }

    private void hideSugg(Runnable runnable) {
        mIsSuggFrag = false;
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.frameLayout, mFragSearchResult);
        t.runOnCommit(() -> {
            if (runnable != null) {
                runnable.run();
            }
            updateActionIconsVisibility();
        });
        t.commitAllowingStateLoss();
    }

    private void showVoiceLanguageSelector() {
        showVoiceLanguageSelector(false);
    }

    private void showVoiceLanguageSelector(final boolean expanded) {
        final PeaceBottomSheetMenu menu = new PeaceBottomSheetMenu();
        menu.getParams().setHeaderTitleResource(R.string.strTitleVoiceSearchLang);

        final ArrayList<BaseListItem> listItems = new ArrayList<>();

        final String appLangTag = Locale.getDefault().toLanguageTag();
        Set<String> addedCodes = new HashSet<>();

        // 1. Arabic
        BaseListItem arItem = new BaseListItem(0, "العربية (Arabic)", null);
        arItem.setKey("ar-SA");
        arItem.setSelected("ar-SA".equals(mVoiceLangCode));
        listItems.add(arItem);
        addedCodes.add("ar-SA");

        // 2. English
        BaseListItem enItem = new BaseListItem(0, "English", null);
        enItem.setKey("en-US");
        enItem.setSelected("en-US".equals(mVoiceLangCode));
        listItems.add(enItem);
        addedCodes.add("en-US");

        // 3. Urdu if app language is English, else App Language
        if (appLangTag.startsWith("en")) {
            BaseListItem urItem = new BaseListItem(0, "اردو (Urdu)", null);
            urItem.setKey("ur-PK");
            urItem.setSelected("ur-PK".equals(mVoiceLangCode));
            listItems.add(urItem);
            addedCodes.add("ur-PK");
        } else if (!addedCodes.contains(appLangTag)) {
            BaseListItem appItem = new BaseListItem(0, Locale.getDefault().getDisplayName(), null);
            appItem.setKey(appLangTag);
            appItem.setSelected(appLangTag.equals(mVoiceLangCode));
            listItems.add(appItem);
            addedCodes.add(appLangTag);
        }

        if (!expanded) {
            BaseListItem trigger = new BaseListItem(com.peacedesign.R.drawable.dr_icon_chevron_right, getString(R.string.strLabelOtherLanguages), null);
            trigger.setId(3000);
            listItems.add(trigger);
        } else {
            String[][] langData = {
                {"Deutsch (German)", "de-DE"}, {"Español (Spanish)", "es-ES"},
                {"Français (French)", "fr-FR"}, {"Hindi", "hi-IN"}, {"Bahasa Indonesia", "id-ID"},
                {"Italiano (Italian)", "it-IT"}, {"Português (Portuguese)", "pt-PT"},
                {"Русский", "ru-RU"}, {"Türkçe (Turkish)", "tr-TR"}, {"اردو (Urdu)", "ur-PK"},
                {"O'zbekchа (Uzbek)", "uz-UZ"}, {"中文 (Chinese)", "zh-CN"}
            };
            
            List<BaseListItem> othersList = new ArrayList<>();
            int idCounter = 2001;
            for (String[] data : langData) {
                if (addedCodes.contains(data[1])) continue;
                BaseListItem item = new BaseListItem(0, data[0], null);
                item.setId(idCounter++);
                item.setKey(data[1]);
                item.setSelected(data[1].equals(mVoiceLangCode));
                othersList.add(item);
            }
            Collections.sort(othersList, (l1, l2) -> l1.getLabel().compareToIgnoreCase(l2.getLabel()));
            listItems.addAll(othersList);
        }

        BaseListAdapter adapter = new BaseListAdapter(this) {
            @Override
            protected View onCreateItemView(BaseListItem item, int position) {
                if (item.getId() == 3000) {
                    BaseListItemView v = new BaseListItemView(getContext(), item);
                    int padH = dp2px(20F);
                    int padV = dp2px(12F);
                    v.getContainerView().setPadding(padH, padV, padH, padV);
                    v.setBackgroundResource(com.peacedesign.R.drawable.dr_bg_action);

                    v.setOnClickListener(view -> {
                        menu.dismiss();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> showVoiceLanguageSelector(true), 50);
                    });
                    return v;
                }

                PeaceRadioButton radio = new PeaceRadioButton(getContext());
                radio.setTexts(item.getLabel(), item.getMessage());
                radio.setChecked(item.getSelected());
                
                int padH = dp2px(20F);
                int padV = dp2px(12F);
                ViewPaddingKt.updatePaddings(radio, padH, padV);
                
                // Border for selected language
                radio.setBackgroundResource(item.getSelected() ? R.drawable.dr_bg_chapter_card_bordered : com.peacedesign.R.drawable.dr_bg_action);

                radio.setTextAppearance(R.style.TextAppearanceCommonTitle);
                radio.setForceTextGravity(COMPOUND_TEXT_GRAVITY_LEFT);
                radio.setSpaceBetween(dp2px(15F));

                return radio;
            }
        };

        adapter.setItems(listItems);
        menu.setAdapter(adapter);
        menu.setOnItemClickListener((dialog, item) -> {
            if (item.getId() == 3000) {
                dialog.dismiss();
                new Handler(Looper.getMainLooper()).postDelayed(() -> showVoiceLanguageSelector(true), 50);
            } else if (item.getKey() != null) {
                mVoiceLangCode = item.getKey();
                SPAppConfigs.setVoiceSearchLang(ActivitySearch.this, mVoiceLangCode);
                dialog.dismiss();
            }
        });
        menu.show(getSupportFragmentManager());
    }

    private void startVoiceRecognitionActivity(String langCode) {
        if (!mSupportsVoiceInput) {
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            if (langCode != null) {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode);
            }
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.strHintSearch));
            mActivityResultLauncher.launch(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private ActivityResultLauncher<Intent> activityResultHandler() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultIntent -> {
            Intent data = resultIntent.getData();
            if (data == null) {
                return;
            }
            int resultCode = resultIntent.getResultCode();
            if (resultCode == RESULT_OK) {
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() < 1) {
                    return;
                }
                String query = matches.get(0);
                mBinding.search.setText(query);
                initSearch(query, false, false);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFragSearchSugg.isAdded() && mFragSearchSugg.isVisible()) {
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
        collection.add(new ChapterJumpModel(chapNo, String.valueOf(chapNo), quranName(this, chapNo),
                quranMeta.getChapterNameTranslation(chapNo)));
    }

    private String quranName(Context context, int chapNo) {
        return mQuranMeta.getChapterName(context, chapNo);
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

    private void showTranslationSheet() {
        PeaceBottomSheet sheet = new PeaceBottomSheet();
        NestedScrollView scrollView = new NestedScrollView(this);
        PeaceRadioGroup radioGroup = new PeaceRadioGroup(this);
        ViewPaddingKt.updatePaddingVertical(radioGroup, dp2px(15F), dp2px(25F));
        int padH = dp2px(25F);
        int padV = dp2px(13F);
        int spaceBtwn = dp2px(15F);

        for (TranslationBookInfoModel bookInfo : availableTranslModels.values()) {
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
            TranslationBookInfoModel bookInfo = (TranslationBookInfoModel) btn.getTag();
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

    public static class SearchResultViewType {
        public static final int VERSE_JUMPER = 0x0;
        public static final int CHAPTER_JUMPER = 0x1;
        public static final int JUZ_JUMPER = 0x2;
        public static final int TAFSIR_JUMPER = 0x3;
        public static final int RESULT_COUNT = 0x4;
        public static final int RESULT = 0x5;
        public static final int LOAD_MORE = 0x6;
    }
}
